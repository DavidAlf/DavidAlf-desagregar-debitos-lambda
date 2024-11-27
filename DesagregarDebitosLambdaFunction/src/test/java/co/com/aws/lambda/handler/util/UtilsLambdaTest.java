package co.com.aws.lambda.handler.util;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

class UtilsLambdaTest {

    private Context mockContext;
    private LambdaLogger mockLogger;
    private S3Client mockS3Client;

    @BeforeEach
    void setUp() {
        // Mockear el Context y el Logger
        mockContext = mock(Context.class);
        mockLogger = mock(LambdaLogger.class);
        mockS3Client = mock(S3Client.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    @Test
    void testPrintRecords_logsKeyAndValue() {
        // Arrange
        Map<String, String> allRecords = Map.of(
                "key1", "value1",
                "key2", "value2");

        // Act
        UtilsLambda.printRecords(mockContext, allRecords);

        // Assert
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);

        // Verificar que se hayan registrado 6 mensajes (3 por cada par clave-valor)
        verify(mockLogger, times(6)).log(logCaptor.capture());

        // Verificar que los mensajes contienen las claves y los valores correctos
        var logs = logCaptor.getAllValues();
        assertTrue(logs.contains("  Key: key1, Value: ["));
        assertTrue(logs.contains("value1"));
        assertTrue(logs.contains("]\n"));
        assertTrue(logs.contains("  Key: key2, Value: ["));
        assertTrue(logs.contains("value2"));
        assertTrue(logs.contains("]\n"));
    }

    @Test
    void testPrintRecords_emptyMap() {
        // Arrange
        Map<String, String> emptyRecords = Map.of();

        // Act
        UtilsLambda.printRecords(mockContext, emptyRecords);

        // Assert
        // Verificar que no se ha registrado ningún mensaje de log
        verify(mockLogger, never()).log(anyString());
    }

    @Test
    void testPrintRecords_singleEntry() {
        // Arrange
        Map<String, String> singleEntryMap = Map.of("key1", "value1");

        // Act
        UtilsLambda.printRecords(mockContext, singleEntryMap);

        // Assert
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, times(3)).log(logCaptor.capture());

        var logs = logCaptor.getAllValues();
        assertTrue(logs.contains("  Key: key1, Value: ["));
        assertTrue(logs.contains("value1"));
        assertTrue(logs.contains("]\n"));
    }

    @Test
    void testPrintMap_logsKeyAndNestedMap() {
        // Arrange
        Map<String, Map<String, String>> typesMaps = Map.of(
                "Map1", Map.of(
                        "key1", "value1",
                        "key2", "value2"),
                "Map2", Map.of(
                        "key3", "value3",
                        "key4", "value4"));

        // Act
        UtilsLambda.printMap(mockContext, typesMaps);

        // Assert
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        // Verificar que `log` se ha llamado varias veces (2 veces por el nombre de los
        // mapas y 4 veces por las claves y valores internos)
        verify(mockLogger, times(14)).log(logCaptor.capture());

        var logs = logCaptor.getAllValues();
        // Verificar que se han registrado los mapas
        assertFalse(logs.contains("Mapa: Map1"));
        assertFalse(logs.contains("Mapa: Map2"));

        // Verificar que las claves y valores internos también están en los logs
        assertTrue(logs.contains("  Key: key1, Value: ["));
        assertTrue(logs.contains("value1"));
        assertTrue(logs.contains("  Key: key2, Value: ["));
        assertTrue(logs.contains("value2"));
        assertTrue(logs.contains("  Key: key3, Value: ["));
        assertTrue(logs.contains("value3"));
        assertTrue(logs.contains("  Key: key4, Value: ["));
        assertTrue(logs.contains("value4"));
    }

    @Test
    void testPrintMap_emptyMap() {
        // Arrange
        Map<String, Map<String, String>> emptyMap = Map.of();

        // Act
        UtilsLambda.printMap(mockContext, emptyMap);

        // Assert
        // Verificar que no se ha registrado nada, ya que el mapa es vacío
        verify(mockLogger, never()).log(anyString());
    }

    @Test
    void testGetObjectRequest_createsRequestCorrectly() {
        // Arrange
        String nombreBucket = "my-bucket";
        String nombreArchivo = "my-file.txt";

        // Act
        GetObjectRequest request = UtilsLambda.getObjectRequest(nombreBucket, nombreArchivo);

        // Assert
        assertNotNull(request);
        assertEquals(nombreBucket, request.bucket());
        assertEquals(nombreArchivo, request.key());
    }

    @Test
    void testGetObjectRequest_emptyBucket() {
        // Arrange
        String nombreBucket = "";
        String nombreArchivo = "my-file.txt";

        // Act
        GetObjectRequest request = UtilsLambda.getObjectRequest(nombreBucket, nombreArchivo);

        // Assert
        assertNotNull(request);
        assertEquals(nombreBucket, request.bucket());
        assertEquals(nombreArchivo, request.key());
    }

    @Test
    void testGetObjectRequest_emptyFileName() {
        // Arrange
        String nombreBucket = "my-bucket";
        String nombreArchivo = "";

        // Act
        GetObjectRequest request = UtilsLambda.getObjectRequest(nombreBucket, nombreArchivo);

        // Assert
        assertNotNull(request);
        assertEquals(nombreBucket, request.bucket());
        assertEquals(nombreArchivo, request.key());
    }

    @Test
    void testObtenerLlavePgpS3_fileExists() throws IOException {
        // Arrange
        String nombreBucket = "my-bucket";
        String rutaLlavePrivada = "path/to/private-key.pgp";
        GetObjectRequest getObjectRequest = UtilsLambda.getObjectRequest(nombreBucket, rutaLlavePrivada);

        // Mockear el InputStream que se devolvería desde S3
        InputStream mockInputStream = new ByteArrayInputStream("dummy data".getBytes());

        // Crear un mock de GetObjectResponse (aunque no lo estamos usando aquí, lo
        // necesitamos para ResponseInputStream)
        GetObjectResponse mockGetObjectResponse = mock(GetObjectResponse.class);

        // Crear un mock de ResponseInputStream, que contiene el InputStream simulado
        ResponseInputStream<GetObjectResponse> mockResponse = mock(ResponseInputStream.class);
        when(mockResponse.read()).thenReturn(mockInputStream.read());
        when(mockS3Client.getObject(getObjectRequest)).thenReturn(mockResponse);

        // Act
        InputStream result = UtilsLambda.obtenerLlavePgpS3(mockContext, mockS3Client, nombreBucket, rutaLlavePrivada);

        // Assert
        assertNotNull(result);

        // Verificar logs
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, times(2)).log(logCaptor.capture());
        var logs = logCaptor.getAllValues();
        assertTrue(logs.contains("[INFO] obtenerLlavePgpS3 [my-bucket][path/to/private-key.pgp]"));
    }

    @Test
    void testObtenerLlavePgpS3_fileNotFound() {
        // Arrange
        String nombreBucket = "my-bucket";
        String rutaLlavePrivada = "path/to/private-key.pgp";
        GetObjectRequest getObjectRequest = UtilsLambda.getObjectRequest(nombreBucket, rutaLlavePrivada);

        // Simulamos que el archivo no existe y lanza una excepción
        when(mockS3Client.getObject(getObjectRequest)).thenThrow(S3Exception.builder().build());

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            UtilsLambda.obtenerLlavePgpS3(mockContext, mockS3Client, nombreBucket, rutaLlavePrivada);
        });
        assertEquals("Error al intentar acceder al archivo en S3: ", thrown.getMessage());

        // Verificar logs
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, times(1)).log(logCaptor.capture());
        var logs = logCaptor.getAllValues();
        assertTrue(logs.contains("[INFO] obtenerLlavePgpS3 [my-bucket][path/to/private-key.pgp]"));
    }

    @Test
    void testObtenerLlavePgpS3_s3Exception() {
        // Arrange
        String nombreBucket = "my-bucket";
        String rutaLlavePrivada = "path/to/private-key.pgp";
        GetObjectRequest getObjectRequest = UtilsLambda.getObjectRequest(nombreBucket, rutaLlavePrivada);

        // Simulamos una excepción al acceder al objeto S3
        when(mockS3Client.getObject(getObjectRequest)).thenThrow(S3Exception.builder().build());

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            UtilsLambda.obtenerLlavePgpS3(mockContext, mockS3Client, nombreBucket, rutaLlavePrivada);
        });
        assertTrue(thrown.getMessage().contains("Error al intentar acceder al archivo en S3"));

        // Verificar logs
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, times(1)).log(logCaptor.capture());
        var logs = logCaptor.getAllValues();
        assertTrue(logs.contains("[INFO] obtenerLlavePgpS3 [my-bucket][path/to/private-key.pgp]"));
    }

    @Test
    void testPrintFiles_successfulUpload() {
        // Arrange
        String nombreBucket = "my-bucket";
        String rutaSalida = "path/to/file/";
        String nombreArchivo = "file.txt";
        byte[] fileContent = "Hello, World!".getBytes();
        String contentType = "text/plain";

        // Mockear la respuesta de putObject
        PutObjectResponse mockPutObjectResponse = mock(PutObjectResponse.class);
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockPutObjectResponse);

        // Act
        UtilsLambda.printFiles(mockContext, mockS3Client, nombreBucket, rutaSalida, nombreArchivo, fileContent,
                contentType);

        // Assert
        // Verificar que se registraron los logs correctamente
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, times(2)).log(logCaptor.capture());
        var logs = logCaptor.getAllValues();
        assertTrue(logs.get(0).contains("[INFO] 9.printFiles [file.txt]"));
        assertTrue(logs.get(1).contains("[INFO] Fin subir archivo descifrado a S3"));
    }

    @Test
    void testPrintFiles_s3Exception() {
        // Arrange
        String nombreBucket = "my-bucket";
        String rutaSalida = "path/to/file/";
        String nombreArchivo = "file.txt";
        byte[] fileContent = "Hello, World!".getBytes();
        String contentType = "text/plain";

        // Crear el PutObjectRequest
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(nombreBucket)
                .key(rutaSalida + nombreArchivo)
                .contentLength((long) fileContent.length)
                .contentType(contentType)
                .build();

        // Simulamos que la llamada a putObject lanza una S3Exception
        doThrow(S3Exception.builder().build()).when(mockS3Client).putObject(eq(putObjectRequest),
                any(RequestBody.class));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            UtilsLambda.printFiles(mockContext, mockS3Client, nombreBucket, rutaSalida, nombreArchivo, fileContent,
                    contentType);
        });

        // Verificar que el mensaje de la excepción contenga el error esperado
        assertTrue(thrown.getMessage().contains("[ERROR] Error al obtener el archivo desde S3"));

        // Verificar que se haya registrado el log inicial
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, times(1)).log(logCaptor.capture());
        var logs = logCaptor.getAllValues();
        assertTrue(logs.get(0).contains("[INFO] 9.printFiles [file.txt]"));
    }

    @Test
    void testPrintFiles_genericException() {
        // Arrange
        String nombreBucket = "my-bucket";
        String rutaSalida = "path/to/file/";
        String nombreArchivo = "file.txt";
        byte[] fileContent = "Hello, World!".getBytes();
        String contentType = "text/plain";

        // Crear el PutObjectRequest
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(nombreBucket)
                .key(rutaSalida + nombreArchivo)
                .contentLength((long) fileContent.length)
                .contentType(contentType)
                .build();

        // Simulamos una excepción genérica (como un NullPointerException)
        doThrow(new NullPointerException("Error processing file")).when(mockS3Client).putObject(eq(putObjectRequest),
                any(RequestBody.class));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            UtilsLambda.printFiles(mockContext, mockS3Client, nombreBucket, rutaSalida, nombreArchivo, fileContent,
                    contentType);
        });

        // Verificar que el mensaje de la excepción contenga el error esperado
        assertTrue(thrown.getMessage().contains("[ERROR] Error procesando el archivo de texto en la línea:"));

        // Verificar que se haya registrado el log inicial
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, times(1)).log(logCaptor.capture());
        var logs = logCaptor.getAllValues();
        assertTrue(logs.get(0).contains("[INFO] 9.printFiles [file.txt]"));
    }
}