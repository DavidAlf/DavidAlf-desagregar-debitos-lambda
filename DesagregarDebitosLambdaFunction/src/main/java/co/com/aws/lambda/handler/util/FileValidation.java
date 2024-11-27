package co.com.aws.lambda.handler.util;

import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

public class FileValidation {

    private String nombreBucketArchivos;
    private String ruteEntradaArchivopgp;

    public FileValidation(String nombreBucketArchivos, String ruteEntradaArchivopgp) {
        this.nombreBucketArchivos = nombreBucketArchivos;
        this.ruteEntradaArchivopgp = ruteEntradaArchivopgp;
    }

    public boolean isValidFile(S3EventNotificationRecord recordFile) {
        String srcBucket = recordFile.getS3().getBucket().getName();
        String srcInFile = recordFile.getS3().getObject().getUrlDecodedKey();

        boolean isBucketValid = srcBucket.equals(nombreBucketArchivos);
        boolean isPathValid = srcInFile.startsWith(ruteEntradaArchivopgp);

        boolean isFileValid = srcInFile.endsWith(".pgp");

        return isBucketValid && isPathValid && isFileValid;
    }

    public void logInvalidFile(S3EventNotificationRecord recordFile) {
        String srcInFile = recordFile.getS3().getObject().getUrlDecodedKey();
        String srcBucket = recordFile.getS3().getBucket().getName();
        throw new RuntimeException(
                "[ERROR] El archivo [" + srcInFile + "] en el bucket [" + srcBucket + "] no es válido.");
    }
}
