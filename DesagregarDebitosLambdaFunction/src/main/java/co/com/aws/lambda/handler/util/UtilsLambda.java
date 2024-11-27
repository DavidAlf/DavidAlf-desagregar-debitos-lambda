package co.com.aws.lambda.handler.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;

import co.com.ath.aws.exception.AthCodigosError;
import co.com.ath.aws.exception.AthException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class UtilsLambda {

    private UtilsLambda() {
        throw new UnsupportedOperationException("No se puede crear instancias de esta clase.");
    }

    public static void printRecords(Context context, Map<String, String> allRecords) {
        allRecords
                .forEach((key, value) -> {
                    context.getLogger().log("  Key: " + key + ", Value: [");
                    context.getLogger().log(value);
                    context.getLogger().log("]\n");
                });
    }

    public static void printMap(Context context, Map<String, Map<String, String>> typesMaps) {
        for (Map.Entry<String, Map<String, String>> entry : typesMaps.entrySet()) {
            String typeMap = entry.getKey();
            Map<String, String> innerMap = entry.getValue();

            context.getLogger().log("Mapa: " + typeMap + "\n");

            printRecords(context, innerMap);
        }
    }

    public static GetObjectRequest getObjectRequest(String nombreBucket, String nombreArchivo) {
        return GetObjectRequest.builder()
                .bucket(nombreBucket)
                .key(nombreArchivo)
                .build();
    }

    public static InputStream obtenerLlavePgpS3(Context context, S3Client s3Client, String nombreBucket,
            String rutaLlavePrivada) {
        context.getLogger().log("[INFO] obtenerLlavePgpS3 [" + nombreBucket + "][" + rutaLlavePrivada + "]");
        GetObjectRequest getObjectRequest = UtilsLambda.getObjectRequest(nombreBucket, rutaLlavePrivada);

        try {
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

            context.getLogger().log(" Existe, bucket y la llave! \n");

            if (s3Object != null) {
                return s3Object;
            }

            context.getLogger().log("[ERROR] La llave para descifrar el PGP en S3 no existe\n");
            throw new AthException(AthCodigosError.C029.getCodigo(), AthCodigosError.C029.getMensaje());
        } catch (S3Exception e) {
            throw new RuntimeException("Error al intentar acceder al archivo en S3: ", e);
        }
    }

    public static void printFiles(Context context, S3Client s3Client, String nombreBucket, String rutaSalida,
            String nombreArchivo, byte[] fileContent, String contentType) {
                context.getLogger().log(String.format("[INFO] 9.printFiles [%s]", nombreArchivo) + " \n");
        try {

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileContent);

            int lengthDescifrado = byteArrayInputStream.available();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(nombreBucket)
                    .key(rutaSalida + nombreArchivo)
                    .contentLength((long) lengthDescifrado)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(byteArrayInputStream, lengthDescifrado));

            context.getLogger().log("[INFO] Fin subir archivo descifrado a S3 \n");

        } catch (S3Exception e) {
            throw new RuntimeException("[ERROR] Error al obtener el archivo desde S3: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("[ERROR] Error procesando el archivo de texto en la línea: " + e.getMessage(),
                    e);
        }
    }
}
