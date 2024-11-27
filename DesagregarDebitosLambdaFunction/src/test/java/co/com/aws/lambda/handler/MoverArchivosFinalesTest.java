package co.com.aws.lambda.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import co.com.aws.lambda.constants.Constantes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

class MoverArchivosFinalesTest {

        @Mock
        private S3Client s3Client;

        @InjectMocks
        private MoverArchivosFinales moverArchivosFinales;

        @Mock
        private Context mockContext;

        @Mock
        private LambdaLogger mockLogger;

        @Mock
        List<S3Object> archivosBucket;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
                moverArchivosFinales = new MoverArchivosFinales(s3Client);
                setField(moverArchivosFinales, "s3Client", s3Client);
        }

        @Test
        void testMoverArchivos_Success() {
                S3Object s3Object1 = mock(S3Object.class);
                when(s3Object1.key()).thenReturn(Constantes.RUTA_ENTRADA + "file1.pgp");
                archivosBucket = Arrays.asList(s3Object1);
                MoverArchivosFinales realMoverArchivosFinales = new MoverArchivosFinales(s3Client);
                MoverArchivosFinales spyMoverArchivosFinales = spy(realMoverArchivosFinales);
                doNothing().when(spyMoverArchivosFinales).copiarArchivo(anyString(), anyString(), anyString());
                doNothing().when(spyMoverArchivosFinales).eliminarArchivo(anyString(), anyString());
                spyMoverArchivosFinales.moverArchivos(archivosBucket);
                verify(spyMoverArchivosFinales, times(1)).copiarArchivo(Constantes.NOMBRE_BUCKET_ARCHIVOS_ENTRADA,
                                Constantes.RUTA_ENTRADA + "file1.pgp", Constantes.RUTA_PROCESADOS + "file1.pgp");
                verify(spyMoverArchivosFinales, times(1)).eliminarArchivo(Constantes.NOMBRE_BUCKET_ARCHIVOS_ENTRADA,
                                Constantes.RUTA_ENTRADA + "file1.pgp");
        }

        @Test
        void testCopiarArchivo() {
                String bucketName = "test-bucket";
                String origenKey = "source/path/to/object.txt";
                String destinoKey = "destination/path/to/object.txt";
                CopyObjectResponse copyObjectResponse = CopyObjectResponse.builder().build();
                when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(copyObjectResponse);
                moverArchivosFinales.copiarArchivo(bucketName, origenKey, destinoKey);
                ArgumentCaptor<CopyObjectRequest> copyObjectRequestCaptor = ArgumentCaptor
                                .forClass(CopyObjectRequest.class);
                verify(s3Client).copyObject(copyObjectRequestCaptor.capture());
                CopyObjectRequest capturedRequest = copyObjectRequestCaptor.getValue();
                assertEquals(bucketName, capturedRequest.sourceBucket());
                assertEquals(origenKey, capturedRequest.sourceKey());
                assertEquals(bucketName, capturedRequest.destinationBucket());
                assertEquals(destinoKey, capturedRequest.destinationKey());
        }

        @Test
        void testEliminarArchivo() {
                String bucketName = "test-bucket";
                String origenKey = "source/path/to/object.txt";
                DeleteObjectResponse deleteObjectResponse = DeleteObjectResponse.builder().build();
                when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(deleteObjectResponse);
                moverArchivosFinales.eliminarArchivo(bucketName, origenKey);
                ArgumentCaptor<DeleteObjectRequest> deleteObjectRequestCaptor = ArgumentCaptor
                                .forClass(DeleteObjectRequest.class);
                verify(s3Client).deleteObject(deleteObjectRequestCaptor.capture());
                DeleteObjectRequest capturedRequest = deleteObjectRequestCaptor.getValue();
                assertEquals(bucketName, capturedRequest.bucket());
                assertEquals(origenKey, capturedRequest.key());
        }

        private void setField(Object target, String fieldName, Object value) {
                try {
                        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        field.set(target, value);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                }
        }
}
