package co.com.aws.lambda.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import co.com.ath.aws.exception.AthException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

class UtilsLambdaTest {

    @Mock
    ResponseInputStream<GetObjectResponse> s3ResponseMock;

    @Test
    void testGetObjectRequest() {
        String bucketName = "test-bucket";
        String fileName = "test-file.txt";
        GetObjectRequest request = UtilsLambda.getObjectRequest(bucketName, fileName);
        assertEquals(bucketName, request.bucket(), "El bucket debe coincidir");
        assertEquals(fileName, request.key(), "El key debe coincidir");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testObtenerLlavePgpS3_successful() {
        // Arrange
        String bucketName = "test-bucket";
        String key = "test-key";
        S3Client s3Client = mock(S3Client.class);
        GetObjectRequest request = UtilsLambda.getObjectRequest(bucketName, key);
        try {
            s3ResponseMock = mock(ResponseInputStream.class);
            when(s3Client.getObject(request)).thenReturn(s3ResponseMock);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Act
        InputStream result = UtilsLambda.obtenerLlavePgpS3(s3Client, bucketName, key);
        // Assert
        assertNotNull(result, "El InputStream no debe ser nulo.");
        verify(s3Client, times(1)).getObject(request);
    }

    @Test
    void testObtenerLlavePgpS3_error() {
        // Arrange
        String bucketName = "test-bucket";
        String key = "test-key";
        S3Client s3Client = mock(S3Client.class);
        GetObjectRequest request = UtilsLambda.getObjectRequest(bucketName, key);
        when(s3Client.getObject(request)).thenThrow(S3Exception.builder().build());
        // Act & Assert
        AthException thrown = assertThrows(AthException.class, () -> {
            UtilsLambda.obtenerLlavePgpS3(s3Client, bucketName, key);
        });
        assertTrue(thrown.getMessage().contains("Error al intentar acceder al archivo en S3"),
                "Se debe lanzar una excepción en caso de error.");
    }

    @Test
    void testPrintFiles_successfulUpload() {
        // Arrange
        S3Client s3Client = mock(S3Client.class);
        byte[] fileContent = "test-content".getBytes();
        String bucketName = "test-bucket";
        String filePath = "test-path/";
        String fileName = "test-file.txt";
        String contentType = "text/plain";
        // Act
        UtilsLambda.printFiles(s3Client, bucketName, filePath, fileName, fileContent, contentType);
        // Assert
        PutObjectRequest expectedRequest = PutObjectRequest.builder().bucket(bucketName).key(filePath + fileName)
                .contentLength((long) fileContent.length).contentType(contentType).build();
        verify(s3Client, times(1)).putObject(eq(expectedRequest), any(RequestBody.class));
    }

    @Test
    void testPrintFiles_error() {
        // Arrange
        S3Client s3Client = mock(S3Client.class);
        byte[] fileContent = "test-content".getBytes();
        String bucketName = "test-bucket";
        String filePath = "test-path/";
        String fileName = "test-file.txt";
        String contentType = "text/plain";
        // Simulamos que el método putObject lanzará una excepción
        doThrow(S3Exception.builder().message("Error al subir el archivo").build()).when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));
        // Act & Assert
        AthException thrown = assertThrows(AthException.class, () -> {
            UtilsLambda.printFiles(s3Client, bucketName, filePath, fileName, fileContent, contentType);
        });
        assertTrue(thrown.getMessage().contains("Error al obtener el archivo desde S3"),
                "Se debe lanzar una excepción en caso de error.");
    }

    @Test
    void testVerificarArchivosEnBucket_successful() {
        // Arrange
        S3Client s3Client = mock(S3Client.class);
        ListObjectsV2Response response = mock(ListObjectsV2Response.class);
        S3Object file1 = S3Object.builder().key("file1.txt").build();
        S3Object file2 = S3Object.builder().key("file2.txt").build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);
        when(response.contents()).thenReturn(List.of(file1, file2));
        // Act
        List<S3Object> files = UtilsLambda.verificarArchivosEnBucket(s3Client);
        // Assert
        assertEquals(2, files.size(), "Se deben encontrar exactamente 2 archivos en el bucket.");
    }

    @Test
    void testVerificarArchivosEnBucket_error() {
        // Arrange
        S3Client s3Client = mock(S3Client.class);
        ListObjectsV2Response response = mock(ListObjectsV2Response.class);
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);
        when(response.contents()).thenReturn(List.of()); // Simulamos que no hay archivos
        // Act & Assert
        AthException thrown = assertThrows(AthException.class, () -> {
            UtilsLambda.verificarArchivosEnBucket(s3Client);
        });
        assertTrue(thrown.getMessage().contains("Se esperan exactamente 2 archivos en el bucket"),
                "Debe lanzar una excepción si no hay 2 archivos.");
    }
}
