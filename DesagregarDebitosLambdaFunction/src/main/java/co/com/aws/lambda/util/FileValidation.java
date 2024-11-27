package co.com.aws.lambda.util;

import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

import co.com.ath.aws.exception.AthException;

/**
 * Clase que valida archivos recibidos en un evento de S3, verificando que se
 * encuentren en el bucket y la ruta correctos y que tengan la extensión ".pgp".
 * <p>
 * Esta clase es utilizada para asegurarse de que los archivos recibidos en un
 * evento de S3 son válidos para el procesamiento posterior, asegurando que se
 * encuentren en la ubicación de entrada y sean archivos PGP.
 * </p>
 * 
 * @author  David Alfonso
 * @version 1.0
 * @since   2024-11-21
 */
public class FileValidation {

    private String nombreBucketArchivos;

    private String ruteEntradaArchivopgp;

    /**
     * Constructor que inicializa los parámetros necesarios para la validación de
     * archivos.
     * 
     * @param nombreBucketArchivos  Nombre del bucket de S3 donde se espera recibir
     *                              los archivos.
     * @param ruteEntradaArchivopgp Ruta de entrada de los archivos PGP dentro del
     *                              bucket.
     */
    public FileValidation(String nombreBucketArchivos, String ruteEntradaArchivopgp) {
        this.nombreBucketArchivos = nombreBucketArchivos;
        this.ruteEntradaArchivopgp = ruteEntradaArchivopgp;
    }

    /**
     * Método que valida si un archivo recibido en un evento de S3 es válido. Se
     * considera válido si:
     * <ul>
     * <li>El archivo se encuentra en el bucket correcto.</li>
     * <li>El archivo está en la ruta correcta de entrada.</li>
     * <li>El archivo tiene la extensión ".pgp".</li>
     * </ul>
     * 
     * @param  recordFile El registro del evento de S3 que contiene la información
     *                    del archivo.
     * @return            {@code true} si el archivo es válido, {@code false} de lo
     *                    contrario.
     */
    public boolean isValidFile(S3EventNotificationRecord recordFile) {
        String srcBucket = recordFile.getS3().getBucket().getName();
        String srcInFile = recordFile.getS3().getObject().getUrlDecodedKey();
        boolean isBucketValid = srcBucket.equals(nombreBucketArchivos);
        boolean isPathValid = srcInFile.startsWith(ruteEntradaArchivopgp);
        boolean isFileValid = srcInFile.endsWith(".pgp");
        return isBucketValid && isPathValid && isFileValid;
    }

    /**
     * Método que registra un error y lanza una excepción si el archivo no es
     * válido.
     * 
     * @param  recordFile   El registro del evento de S3 que contiene la información
     *                      del archivo.
     * @throws AthException Si el archivo no es válido, se lanza una excepción
     *                      personalizada.
     */
    public void logInvalidFile(S3EventNotificationRecord recordFile) {
        String srcInFile = recordFile.getS3().getObject().getUrlDecodedKey();
        String srcBucket = recordFile.getS3().getBucket().getName();
        throw new AthException("[ERROR]",
                "[ERROR] El archivo [" + srcInFile + "] en el bucket [" + srcBucket + "] no es válido.");
    }
}
