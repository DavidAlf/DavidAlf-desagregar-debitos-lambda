package co.com.aws.lambda.handler;

import java.util.List;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;

import co.com.aws.lambda.constants.Constantes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Clase encargada de mover archivos desde un bucket de entrada a una ruta de
 * archivos procesados en el mismo bucket de S3. La clase también gestiona la
 * eliminación de los archivos originales después de moverlos.
 * <p>
 * Esta clase se utiliza para mover los archivos que han sido procesados de una
 * ubicación de entrada a una ubicación de salida, asegurándose de eliminar los
 * archivos de la ubicación original una vez que han sido correctamente
 * copiados.
 * </p>
 * 
 * @author  David Alfonso
 * @version 1.0
 * @since   2024-11-21
 */
public class MoverArchivosFinales {

    private static final LambdaLogger LOGGER = LambdaRuntime.getLogger();

    private final S3Client s3Client;

    /**
     * Constructor que inicializa el cliente de S3 para interactuar con el servicio.
     * 
     * @param s3Client Cliente de S3 para realizar operaciones de copiar y eliminar
     *                 archivos en S3.
     */
    public MoverArchivosFinales(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Método que recorre los archivos en el bucket de entrada, los mueve a la ruta
     * de archivos procesados y elimina los archivos originales en el bucket de
     * entrada.
     * 
     * @param archivosBucket Lista de archivos que deben ser movidos y eliminados.
     */
    public void moverArchivos(List<S3Object> archivosBucket) {
        LOGGER.log("[INFO] 9.moverArchivos\n");
        archivosBucket.forEach(fileRecords -> {
            String origenKey = fileRecords.key();
            String destinoKey = Constantes.RUTA_PROCESADOS + origenKey.substring(Constantes.RUTA_ENTRADA.length());
            copiarArchivo(Constantes.NOMBRE_BUCKET_ARCHIVOS_ENTRADA, origenKey, destinoKey);
            eliminarArchivo(Constantes.NOMBRE_BUCKET_ARCHIVOS_ENTRADA, origenKey);
        });
    }

    /**
     * Método que copia un archivo de un origen en S3 a un destino en el mismo
     * bucket.
     * 
     * @param bucketName Nombre del bucket de S3 donde se encuentran los archivos.
     * @param origenKey  La clave (key) del archivo en el bucket de origen.
     * @param destinoKey La clave (key) del archivo en el bucket de destino.
     */
    protected void copiarArchivo(String bucketName, String origenKey, String destinoKey) {
        LOGGER.log("[INFO] 9.1.copiarArchivo ");
        CopyObjectRequest copyRequest = CopyObjectRequest.builder().sourceBucket(bucketName).sourceKey(origenKey)
                .destinationBucket(bucketName).destinationKey(destinoKey).build();
        s3Client.copyObject(copyRequest);
        LOGGER.log(String.format(" Archivo copiado de [%s] a [%s]%s", origenKey, destinoKey, "\n"));
    }

    /**
     * Método que elimina un archivo de un bucket de S3.
     * 
     * @param bucketName Nombre del bucket de S3 desde donde se eliminará el
     *                   archivo.
     * @param origenKey  La clave (key) del archivo en el bucket que se desea
     *                   eliminar.
     */
    protected void eliminarArchivo(String bucketName, String origenKey) {
        LOGGER.log("[INFO] 9.2.eliminarArchivo ");
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(bucketName).key(origenKey).build();
        s3Client.deleteObject(deleteRequest);
        LOGGER.log(String.format(" Archivo eliminado: de [%s]%s", origenKey, "\n"));
    }
}
