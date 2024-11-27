package co.com.aws.lambda.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;

import co.com.aws.lambda.constants.Constantes;
import co.com.aws.lambda.dao.AuditoriaDividendosDao;
import co.com.aws.lambda.util.FileValidation;
import co.com.aws.lambda.util.UtilsLambda;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

class BillpayLambdaHandlerTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Event s3EventTest;

    @Mock
    private DesencriptaArchivos desencriptaArchivos;

    @Mock
    private EncriptarArchivos encriptarArchivos;

    @Mock
    private AuditoriaDividendosDao auditoriaDividendosDao;

    @Mock
    private MoverArchivosFinales moverArchivosFinales;

    @Mock
    private FileValidation fileValidation;

    @Mock
    private Map<String, Map<String, String>> typesMaps;

    @Mock
    private Map<String, String> archivoFusinadoMap;

    @InjectMocks
    private BillpayLambdaHandler billpayLambdaHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHandleRequest_validEvent() {
        // Arrange
        S3Event.S3EventNotificationRecord recordEvent = mock(S3Event.S3EventNotificationRecord.class);
        S3EventNotification.S3Entity s3Entity = mock(S3EventNotification.S3Entity.class);
        S3EventNotification.S3ObjectEntity s3Object = mock(S3EventNotification.S3ObjectEntity.class);
        when(s3EventTest.getRecords()).thenReturn(Arrays.asList(recordEvent));
        when(recordEvent.getS3()).thenReturn(s3Entity);
        when(s3Entity.getBucket()).thenReturn(mock(S3EventNotification.S3BucketEntity.class));
        when(s3Entity.getBucket().getName()).thenReturn(Constantes.NOMBRE_BUCKET_ARCHIVOS_ENTRADA);
        when(s3Entity.getObject()).thenReturn(s3Object);
        when(s3Object.getUrlDecodedKey()).thenReturn(Constantes.RUTA_ENTRADA + "Prueba.pgp");
        when(fileValidation.isValidFile(recordEvent)).thenReturn(true);
        ListObjectsV2Response mockResponse = mock(ListObjectsV2Response.class);
        when(mockResponse.contents()).thenReturn(Collections.singletonList(mock(S3Object.class)));
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockResponse);
        doNothing().when(desencriptaArchivos).getFiles(any(), any(), any());
        doNothing().when(encriptarArchivos).getEncrypRecords(any());
        doNothing().when(moverArchivosFinales).moverArchivos(any());
        when(auditoriaDividendosDao.registrarAuditoria(any())).thenReturn(true);
        try (MockedStatic<UtilsLambda> mockedUtilsLambda = mockStatic(UtilsLambda.class)) {
            ListObjectsV2Response response = mock(ListObjectsV2Response.class);
            @SuppressWarnings("unchecked")
            List<S3Object> files = mock(List.class);
            S3Object file1 = S3Object.builder().key("file1.txt").build();
            S3Object file2 = S3Object.builder().key("file2.txt").build();
            when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);
            when(response.contents()).thenReturn(List.of(file1, file2));
            mockedUtilsLambda.when(() -> UtilsLambda.verificarArchivosEnBucket(s3Client)).thenReturn(files);
            billpayLambdaHandler.handleRequest(s3EventTest);
            // Assert
            verify(desencriptaArchivos, times(1)).getFiles(any(), any(), any());
            verify(encriptarArchivos, times(1)).getEncrypRecords(any());
            verify(moverArchivosFinales, times(1)).moverArchivos(any());
            verify(auditoriaDividendosDao, times(1)).registrarAuditoria(any());
        }
    }

    @Test
    void testHandleRequest_noFilesInEvent() {
        // Arrange
        s3EventTest = mock(S3Event.class);
        when(s3EventTest.getRecords()).thenReturn(Arrays.asList());
        // Act & Assert
        billpayLambdaHandler.handleRequest(s3EventTest);
        // No deben ocurrir interacciones si no hay archivos para procesar
        verifyNoInteractions(desencriptaArchivos, encriptarArchivos, auditoriaDividendosDao, moverArchivosFinales);
    }
}
