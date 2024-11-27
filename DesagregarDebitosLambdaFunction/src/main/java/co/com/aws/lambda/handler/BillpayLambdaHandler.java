package co.com.aws.lambda.handler;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;
import com.amazonaws.services.lambda.runtime.events.S3Event;

import co.com.ath.aws.exception.AthException;
import co.com.aws.lambda.constants.Constantes;
import co.com.aws.lambda.dao.AuditoriaDividendosDao;
import co.com.aws.lambda.dto.AuditoriaDividendosDto;
import co.com.aws.lambda.util.FileValidation;
import co.com.aws.lambda.util.UtilsLambda;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Clase manejadora principal de la Lambda Billpay. Esta clase se encarga de
 * procesar eventos de archivos en S3, realizar validaciones de los archivos,
 * auditoría de dividendos, encriptación de datos y mover archivos a su
 * ubicación final. La función principal de esta Lambda es manejar la
 * integración de datos para procesar informes de dividendos, incluyendo la
 * desencriptación y validación de archivos, así como registrar auditoría de los
 * procesos realizados.
 * <p>
 * La clase también interactúa con el bucket de entrada y salida, y otros
 * servicios asociados a la auditoría y manejo de archivos.
 * </p>
 * 
 * @author  David Alfonso
 * @version 1.0
 * @since   2024-11-21
 */
public class BillpayLambdaHandler {

    private static final LambdaLogger LOGGER = LambdaRuntime.getLogger();

    private final S3Client s3Client;

    private DesencriptaArchivos desencriptaArchivos;

    private EncriptarArchivos encriptarArchivos;

    private AuditoriaDividendosDao auditoriaDividendosDao;

    private MoverArchivosFinales moverArchivosFinales;

    /**
     * Constructor por defecto que inicializa los componentes necesarios para la
     * Lambda: el cliente de S3, las clases encargadas de desencriptar y encriptar
     * archivos, el DAO para auditoría y la clase encargada de mover archivos
     * finales.
     */
    public BillpayLambdaHandler() {
        this.s3Client = S3Client.builder().region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create()).build();
        this.desencriptaArchivos = new DesencriptaArchivos(this.s3Client);
        this.encriptarArchivos = new EncriptarArchivos(this.s3Client);
        this.moverArchivosFinales = new MoverArchivosFinales(this.s3Client);
        this.auditoriaDividendosDao = new AuditoriaDividendosDao();
    }

    /**
     * Método que maneja el evento de entrada S3Event. Este método se encarga de
     * procesar los archivos que llegan a través de S3, validar los archivos,
     * realizar la auditoría de los dividendos, encriptar los datos y mover los
     * archivos procesados a su ubicación final.
     * 
     * @param s3Event El evento S3 que contiene los registros de los archivos a
     *                procesar.
     */
    public void handleRequest(final S3Event s3Event) {
        List<S3Object> archivosBucket;
        LocalDateTime fechaEjecucion = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HH-mm-ss");
        LOGGER.log(String.format("[INFO] Inicia Lambda DesagregarDebitosLambdaFunction [%s]%s",
                fechaEjecucion.format(formatter), "\n"));
        if (s3Event == null || s3Event.getRecords().isEmpty()) {
            LOGGER.log("[WARN] No hay archivos para procesar\n");
            return;
        }
        processFileValidation(s3Event);
        archivosBucket = UtilsLambda.verificarArchivosEnBucket(s3Client);
        AuditoriaDividendosDto auditoriaDividendosDto = new AuditoriaDividendosDto();
        auditoriaDividendosDto.setHoraInicio(Timestamp.valueOf(LocalDateTime.now()));
        Map<String, Map<String, String>> typesMaps = new HashMap<>();
        this.desencriptaArchivos.getFiles(archivosBucket, typesMaps, auditoriaDividendosDto);
        int totalRegitrosUnicos = calculateTotalRecords(typesMaps);
        processAuditoria(typesMaps, auditoriaDividendosDto, totalRegitrosUnicos);
        this.encriptarArchivos.getEncrypRecords(typesMaps);
        auditoriaDividendosDto.setHoraFin(Timestamp.valueOf(LocalDateTime.now()));
        this.moverArchivosFinales.moverArchivos(archivosBucket);
        try {
            LOGGER.log("[INFO] 10.Registra Auditoria\n");
            auditoriaDividendosDao.registrarAuditoria(auditoriaDividendosDto);
        } catch (Exception e) {
            throw new AthException("[ERROR][10]", "[ERROR] Error al registrar auditoria: ", e);
        }
        LOGGER.log("[INFO] Fin Lambda DesagregarDebitosLambdaFunction\n");
    }

    /**
     * Método encargado de realizar la validación de los archivos provenientes de
     * S3. Este método verifica si los archivos son válidos según las reglas
     * establecidas.
     * 
     * @param s3Event El evento S3 que contiene los registros de los archivos a
     *                validar.
     */
    protected void processFileValidation(S3Event s3Event) {
        FileValidation fileValidation = new FileValidation(Constantes.NOMBRE_BUCKET_ARCHIVOS_ENTRADA,
                Constantes.RUTA_ENTRADA);
        s3Event.getRecords().forEach(fileRecords -> {
            if (!fileValidation.isValidFile(fileRecords)) {
                fileValidation.logInvalidFile(fileRecords);
            }
        });
    }

    /**
     * Método que calcula el total de registros únicos procesados en el archivo
     * fusionado.
     * 
     * @param  typesMaps El mapa que contiene los registros de los diferentes tipos
     *                   de archivos procesados.
     * @return           El total de registros únicos en el archivo fusionado.
     */
    protected int calculateTotalRecords(Map<String, Map<String, String>> typesMaps) {
        Map<String, String> archivoFusionadoMap = typesMaps.get(Constantes.ARCHIVO_FUSIONADO);
        return archivoFusionadoMap != null ? archivoFusionadoMap.size() + 1 : 0;
    }

    /**
     * Método encargado de procesar la auditoría, calculando los registros
     * fusionados y asignándolos al DTO de auditoría para su posterior registro.
     * 
     * @param typesMaps              El mapa que contiene los registros de los
     *                               diferentes tipos de archivos procesados.
     * @param auditoriaDividendosDto El objeto de auditoría donde se almacenarán los
     *                               resultados.
     * @param totalRegitrosUnicos    El total de registros únicos calculados.
     */
    protected void processAuditoria(Map<String, Map<String, String>> typesMaps,
            AuditoriaDividendosDto auditoriaDividendosDto, int totalRegitrosUnicos) {
        String numResult = String.format("%0" + 6 + "d", totalRegitrosUnicos);
        String totalRecords = "3".concat(numResult);
        Map<String, String> archivoFusionadoMap = typesMaps.get(Constantes.ARCHIVO_FUSIONADO);
        if (archivoFusionadoMap != null) {
            archivoFusionadoMap.put("total", totalRecords);
        }
        auditoriaDividendosDto.setTotalRegistrosFusionados(totalRegitrosUnicos);
    }
}
