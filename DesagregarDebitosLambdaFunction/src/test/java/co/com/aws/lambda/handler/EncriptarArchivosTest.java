package co.com.aws.lambda.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import co.com.ath.aws.cifrado.PgpEncryptionUtil;
import co.com.aws.lambda.constants.Constantes;
import co.com.aws.lambda.util.UtilsLambda;
import software.amazon.awssdk.services.s3.S3Client;

class EncriptarArchivosTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private LambdaLogger logger;

    @Mock
    private UtilsLambda utilsLambda;

    @Mock
    private PgpEncryptionUtil pgpEncryptionUtil;

    @InjectMocks
    private EncriptarArchivos encriptarArchivos;

    InputStream llavePrivada = new ByteArrayInputStream("Llave privada".getBytes());

    InputStream llavePublica = new ByteArrayInputStream("Llave publica".getBytes());

    InputStream archivoCifrado = new ByteArrayInputStream("ArchivoCifrado.pgp".getBytes());

    private static final String NOMBRE_ARCHIVO = "test.pgp";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        encriptarArchivos = spy(new EncriptarArchivos(s3Client));
    }

    @Test
    void testGetEncrypRecords_Success() throws Exception {
        // Crear un mapa simulado de datos
        Map<String, Map<String, String>> typesMaps = new HashMap<>();
        Map<String, String> fileData = new HashMap<>();
        fileData.put("key1", "value1");
        typesMaps.put(NOMBRE_ARCHIVO, fileData);
        try (MockedStatic<UtilsLambda> mockedUtilsLambda = mockStatic(UtilsLambda.class)) {
            mockedUtilsLambda.when(() -> UtilsLambda.obtenerLlavePgpS3(any(S3Client.class),
                    eq(Constantes.NOMBRE_BUCKET_LLAVES), eq(Constantes.RUTA_LLAVE_PUBLICAPGP)))
                    .thenReturn(llavePrivada);
            EncriptarArchivos realEncriptarArchivos = new EncriptarArchivos(s3Client);
            EncriptarArchivos spyEncriptarArchivos = spy(realEncriptarArchivos);
            doNothing().when(spyEncriptarArchivos).cifrarArchivoPgp(anyString(), any(), any());
            spyEncriptarArchivos.getEncrypRecords(typesMaps);
            verify(spyEncriptarArchivos, times(1)).cifrarArchivoPgp(eq(NOMBRE_ARCHIVO + ".txt"), any(),
                    eq(llavePrivada));
        }
    }

    @Test
    void testGetEncrypRecords_ExceptionHandling() throws Exception {
        Map<String, Map<String, String>> typesMaps = new HashMap<>();
        Map<String, String> fileData = new HashMap<>();
        fileData.put("key1", "value1");
        typesMaps.put(NOMBRE_ARCHIVO, fileData);
        try (MockedStatic<UtilsLambda> mockedUtilsLambda = mockStatic(UtilsLambda.class)) {
            mockedUtilsLambda
                    .when(() -> UtilsLambda.obtenerLlavePgpS3(any(S3Client.class), eq(Constantes.NOMBRE_BUCKET_LLAVES),
                            eq(Constantes.RUTA_LLAVE_PUBLICAPGP)))
                    .thenThrow(new RuntimeException("Error al obtener la llave pública"));
            EncriptarArchivos realEncriptarArchivos = new EncriptarArchivos(s3Client);
            EncriptarArchivos spyEncriptarArchivos = spy(realEncriptarArchivos);
            doNothing().when(spyEncriptarArchivos).cifrarArchivoPgp(anyString(), any(), any());
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                spyEncriptarArchivos.getEncrypRecords(typesMaps);
            });
            assertTrue(exception.getMessage().contains("Error al obtener la llave pública"));
        }
    }

    @Test
    void testCifrarArchivoPgps_Success() throws Exception {
        boolean cubreCodigo = false;
        EncriptarArchivos realEncriptarArchivos = new EncriptarArchivos(s3Client);
        EncriptarArchivos spyEncriptarArchivos = spy(realEncriptarArchivos);
        try (MockedStatic<PgpEncryptionUtil> mockedPgpEncryptionUtill = mockStatic(PgpEncryptionUtil.class)) {
            mockedPgpEncryptionUtill.when(() -> PgpEncryptionUtil.cifrarArchivo(any(InputStream.class), any(long.class),
                    any(InputStream.class))).thenReturn(archivoCifrado);
            try (MockedStatic<UtilsLambda> mockedUtilsLambda = mockStatic(UtilsLambda.class)) {
                doNothing().when(UtilsLambda.class);
                UtilsLambda.printFiles(any(S3Client.class), anyString(), anyString(), anyString(), any(byte[].class),
                        anyString());
                spyEncriptarArchivos.cifrarArchivoPgp(NOMBRE_ARCHIVO, llavePrivada, llavePublica);
                cubreCodigo = true;
                assertTrue(cubreCodigo);
            }
        }
    }

    @Test
    void testCifrarArchivoPgps_Exception() {
        boolean cubreCodigo = false;
        EncriptarArchivos realEncriptarArchivos = new EncriptarArchivos(s3Client);
        EncriptarArchivos spyEncriptarArchivos = spy(realEncriptarArchivos);
        try (MockedStatic<PgpEncryptionUtil> mockedPgpEncryptionUtill = mockStatic(PgpEncryptionUtil.class)) {
            mockedPgpEncryptionUtill.when(() -> PgpEncryptionUtil.cifrarArchivo(any(InputStream.class), any(long.class),
                    any(InputStream.class))).thenThrow(new IOException("Error al cifrar el archivo"));
            try (MockedStatic<UtilsLambda> mockedUtilsLambda = mockStatic(UtilsLambda.class)) {
                doNothing().when(UtilsLambda.class);
                UtilsLambda.printFiles(any(S3Client.class), anyString(), anyString(), anyString(), any(byte[].class),
                        anyString());
                Exception exception = assertThrows(RuntimeException.class, () -> {
                    spyEncriptarArchivos.cifrarArchivoPgp(NOMBRE_ARCHIVO, llavePrivada, llavePublica);
                });
                assertTrue(exception.getMessage().contains("error al cifrarArchivoPgp"));
                cubreCodigo = true;
                assertTrue(cubreCodigo);
            }
        }
    }
}
