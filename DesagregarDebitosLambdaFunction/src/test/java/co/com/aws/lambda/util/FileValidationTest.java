package co.com.aws.lambda.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;

class FileValidationTest {

    private static final String BUCKET_NAME = "mi-bucket";

    private static final String RUTE_ENTRADA_ARCHIVO_PGP = "entrada/archivos/";

    private FileValidation fileValidation;

    @BeforeEach
    void setUp() {
        fileValidation = new FileValidation(BUCKET_NAME, RUTE_ENTRADA_ARCHIVO_PGP);
    }

    @Test
    void testIsValidFile_ValidFile_ReturnsTrue() {
        // Arrange
        S3EventNotification.S3EventNotificationRecord recordEvent = mock(
                S3EventNotification.S3EventNotificationRecord.class);
        S3EventNotification.S3Entity s3Entity = mock(S3EventNotification.S3Entity.class);
        S3EventNotification.S3ObjectEntity s3Object = mock(S3EventNotification.S3ObjectEntity.class);
        when(recordEvent.getS3()).thenReturn(s3Entity);
        when(s3Entity.getBucket()).thenReturn(mock(S3EventNotification.S3BucketEntity.class));
        when(s3Entity.getBucket().getName()).thenReturn(BUCKET_NAME);
        when(s3Entity.getObject()).thenReturn(s3Object);
        when(s3Object.getUrlDecodedKey()).thenReturn("entrada/archivos/archivo.pgp");
        // Act
        boolean result = fileValidation.isValidFile(recordEvent);
        // Assert
        assertTrue(result);
    }

    @Test
    void testIsValidFile_InvalidBucket_ReturnsFalse() {
        // Arrange
        S3EventNotification.S3EventNotificationRecord recordEvent = mock(
                S3EventNotification.S3EventNotificationRecord.class);
        S3EventNotification.S3Entity s3Entity = mock(S3EventNotification.S3Entity.class);
        S3EventNotification.S3ObjectEntity s3Object = mock(S3EventNotification.S3ObjectEntity.class);
        when(recordEvent.getS3()).thenReturn(s3Entity);
        when(s3Entity.getBucket()).thenReturn(mock(S3EventNotification.S3BucketEntity.class));
        when(s3Entity.getBucket().getName()).thenReturn("otro-bucket");
        when(s3Entity.getObject()).thenReturn(s3Object);
        when(s3Object.getUrlDecodedKey()).thenReturn("entrada/archivos/archivo.pgp");
        // Act
        boolean result = fileValidation.isValidFile(recordEvent);
        // Assert
        assertFalse(result);
    }

    @Test
    void testIsValidFile_InvalidPath_ReturnsFalse() {
        // Arrange
        S3EventNotification.S3EventNotificationRecord recordEvent = mock(
                S3EventNotification.S3EventNotificationRecord.class);
        S3EventNotification.S3Entity s3Entity = mock(S3EventNotification.S3Entity.class);
        S3EventNotification.S3ObjectEntity s3Object = mock(S3EventNotification.S3ObjectEntity.class);
        when(recordEvent.getS3()).thenReturn(s3Entity);
        when(s3Entity.getBucket()).thenReturn(mock(S3EventNotification.S3BucketEntity.class));
        when(s3Entity.getBucket().getName()).thenReturn(BUCKET_NAME);
        when(s3Entity.getObject()).thenReturn(s3Object);
        when(s3Object.getUrlDecodedKey()).thenReturn("otro/path/archivo.pgp");
        // Act
        boolean result = fileValidation.isValidFile(recordEvent);
        // Assert
        assertFalse(result);
    }

    @Test
    void testIsValidFile_InvalidFileExtension_ReturnsFalse() {
        // Arrange
        S3EventNotification.S3EventNotificationRecord recordEvent = mock(
                S3EventNotification.S3EventNotificationRecord.class);
        S3EventNotification.S3Entity s3Entity = mock(S3EventNotification.S3Entity.class);
        S3EventNotification.S3ObjectEntity s3Object = mock(S3EventNotification.S3ObjectEntity.class);
        when(recordEvent.getS3()).thenReturn(s3Entity);
        when(s3Entity.getBucket()).thenReturn(mock(S3EventNotification.S3BucketEntity.class));
        when(s3Entity.getBucket().getName()).thenReturn(BUCKET_NAME);
        when(s3Entity.getObject()).thenReturn(s3Object);
        when(s3Object.getUrlDecodedKey()).thenReturn("entrada/archivos/archivo.txt");
        // Act
        boolean result = fileValidation.isValidFile(recordEvent);
        // Assert
        assertFalse(result);
    }

    @Test
    void testLogInvalidFile_ThrowsRuntimeException() {
        // Arrange
        S3EventNotification.S3EventNotificationRecord recordEvent = mock(
                S3EventNotification.S3EventNotificationRecord.class);
        S3EventNotification.S3Entity s3Entity = mock(S3EventNotification.S3Entity.class);
        S3EventNotification.S3ObjectEntity s3Object = mock(S3EventNotification.S3ObjectEntity.class);
        when(recordEvent.getS3()).thenReturn(s3Entity);
        when(s3Entity.getBucket()).thenReturn(mock(S3EventNotification.S3BucketEntity.class));
        when(s3Entity.getBucket().getName()).thenReturn(BUCKET_NAME);
        when(s3Entity.getObject()).thenReturn(s3Object);
        when(s3Object.getUrlDecodedKey()).thenReturn("entrada/archivos/archivo.txt");
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileValidation.logInvalidFile(recordEvent);
        });
        assertEquals("[ERROR] El archivo [entrada/archivos/archivo.txt] en el bucket [mi-bucket] no es válido.",
                exception.getMessage());
    }
}
