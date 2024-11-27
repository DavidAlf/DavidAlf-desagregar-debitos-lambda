package co.com.aws.lambda.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.events.S3Event;

import co.com.ath.aws.cifrado.CifrarFraseUtil;
import co.com.ath.aws.cifrado.PgpDecryptionUtil;
import co.com.ath.aws.commons.AthConstants;
import co.com.ath.aws.secretmanagerutil.SecretsManagerUtil;
import co.com.aws.lambda.constants.Constantes;
import co.com.aws.lambda.dto.AuditoriaDividendosDto;
import co.com.aws.lambda.util.FileValidation;
import co.com.aws.lambda.util.UtilsLambda;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

class DesencriptaArchivosTest {

        @Mock
        private S3Client s3Client;

        @Mock
        private S3Event s3Event;

        @Mock
        private ClasificaRegistros clasificaRegistros;

        @Mock
        private AuditoriaDividendosDto auditoriaDividendosDto;

        @Mock
        private FileValidation fileValidation;

        @InjectMocks
        private DesencriptaArchivos desencriptaArchivos;

        String llaveCifrada = "Cifrada";

        String llaveDesencriptada = "Desencriptado";

        InputStream llavePrivada = new ByteArrayInputStream("Llave privada".getBytes());

        InputStream llavePublica = new ByteArrayInputStream("Llave publica".getBytes());

        InputStream archivoCifrado = new ByteArrayInputStream("ArchivoCifrado.pgp".getBytes());

        InputStream archivoDescifrado = new ByteArrayInputStream("ArchivoDescifrado.txt".getBytes());

        private String nombreArchivo1 = "file1";

        private String nombreArchivo2 = "file2";

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
        }

        @Test
        void testGetFiles_Success() throws Exception {
                // Crear un mapa simulado de datos
                Map<String, Map<String, String>> typesMaps = new HashMap<>();
                Map<String, String> fileData = new HashMap<>();
                fileData.put("key1", "value1");
                typesMaps.put(nombreArchivo1, fileData);
                nombreArchivo1 = nombreArchivo1 + ".pgp";
                nombreArchivo2 = nombreArchivo2 + ".pgp";
                // Simular el S3Object para cada archivo
                String srcFile1 = Constantes.RUTA_ENTRADA + nombreArchivo1;
                String srcFile2 = Constantes.RUTA_ENTRADA + nombreArchivo2;
                S3Object s3Object1 = mock(S3Object.class);
                S3Object s3Object2 = mock(S3Object.class);
                when(s3Object1.key()).thenReturn(srcFile1);
                when(s3Object2.key()).thenReturn(srcFile2);
                // Crear la lista de objetos S3
                List<S3Object> archivosBucket = Arrays.asList(s3Object1, s3Object2);
                // Simulamos la clase real que tiene el comportamiento de mover los archivos
                DesencriptaArchivos realDesencriptaArchivos = new DesencriptaArchivos(s3Client);
                DesencriptaArchivos spyDesencriptaArchivos = spy(realDesencriptaArchivos);
                // Simulamos la clase de validación de archivo
                fileValidation = mock(FileValidation.class);
                when(fileValidation.isValidFile(any())).thenReturn(true); // Hacemos que siempre devuelva true
                // Auditoría simulada
                auditoriaDividendosDto = mock(AuditoriaDividendosDto.class);
                try (MockedStatic<SecretsManagerUtil> secretsManagerUtil = mockStatic(SecretsManagerUtil.class)) {
                        when(SecretsManagerUtil.getSecretString(AthConstants.KEY_SECRET_NAME_CIPHER,
                                        AthConstants.KEY_SECRET_CIPHER)).thenReturn(llaveCifrada);
                        try (MockedStatic<CifrarFraseUtil> cifrarFraseUtil = mockStatic(CifrarFraseUtil.class)) {
                                when(CifrarFraseUtil.decrypt(Constantes.FRASE_SECRETAPGP, llaveCifrada))
                                                .thenReturn(llaveDesencriptada);
                                try (MockedStatic<UtilsLambda> mockedUtilsLambda = mockStatic(UtilsLambda.class)) {
                                        mockedUtilsLambda
                                                        .when(() -> UtilsLambda.obtenerLlavePgpS3(any(S3Client.class),
                                                                        eq(Constantes.NOMBRE_BUCKET_LLAVES),
                                                                        eq(Constantes.RUTA_LLAVE_PRIVADAPGP)))
                                                        .thenReturn(llavePrivada);
                                        GetObjectRequest mockGetObjectRequest = mock(GetObjectRequest.class);
                                        mockedUtilsLambda.when(
                                                        () -> UtilsLambda.getObjectRequest(anyString(), anyString()))
                                                        .thenReturn(mockGetObjectRequest);
                                        try (MockedStatic<PgpDecryptionUtil> mockedDecryptionUtil = mockStatic(
                                                        PgpDecryptionUtil.class)) {
                                                mockedDecryptionUtil
                                                                .when(() -> PgpDecryptionUtil.descifrarArchivo(any(),
                                                                                anyString(), any()))
                                                                .thenReturn(archivoDescifrado);
                                                int totalRecords = 2;
                                                when(clasificaRegistros.processFiles(anyString(), any(), any()))
                                                                .thenReturn(totalRecords);
                                                when(spyDesencriptaArchivos.descifrarArchivoPgp(anyString(), any(),
                                                                anyString(), anyMap())).thenReturn(totalRecords);
                                                when(spyDesencriptaArchivos.decryptionFile(anyString(), anyMap()))
                                                                .thenReturn(totalRecords);
                                                spyDesencriptaArchivos.getFiles(archivosBucket, typesMaps,
                                                                auditoriaDividendosDto);
                                                verify(spyDesencriptaArchivos, times(3)).decryptionFile(anyString(),
                                                                any());
                                        }
                                }
                        }
                }
        }
}
