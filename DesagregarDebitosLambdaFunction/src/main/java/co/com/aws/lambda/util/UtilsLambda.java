package co.com.aws.lambda.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;

import co.com.ath.aws.exception.AthCodigosError;
import co.com.ath.aws.exception.AthException;
import co.com.aws.lambda.constants.Constantes;
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

/**
 * Clase utilitaria que proporciona métodos comunes para interactuar con AWS S3
 * en el contexto de el procesamiento de archivos, incluyendo la obtención de
 * objetos de S3, la impresión de archivos y la verificación de archivos en el
 * bucket.
 * <p>
 * Contiene métodos para obtener claves PGP, almacenar archivos en S3, y
 * verificar la existencia de archivos en un bucket específico.
 * </p>
 * 
 * @author  David Alfonso
 * @version 1.0
 * @since   2024-11-21
 */
public class UtilsLambda {

    private static final LambdaLogger LOGGER = LambdaRuntime.getLogger();

    /**
     * Constructor privado para evitar la creación de instancias de esta clase. Esta
     * clase solo debe ser utilizada de forma estática.
     */
    private UtilsLambda() {
        throw new UnsupportedOperationException("Esta clase no debe ser instanciada");
    }

    /**
     * Crea un objeto de solicitud para obtener un archivo de S3.
     * 
     * @param  nombreBucket  El nombre del bucket de S3.
     * @param  nombreArchivo El nombre del archivo a obtener.
     * @return               Un objeto {@link GetObjectRequest} configurado con el
     *                       bucket y el archivo.
     */
    public static GetObjectRequest getObjectRequest(String nombreBucket, String nombreArchivo) {
        return GetObjectRequest.builder().bucket(nombreBucket).key(nombreArchivo).build();
    }

    /**
     * Obtiene la clave PGP almacenada en un bucket de S3.
     * 
     * @param  s3Client         El cliente de S3 para interactuar con el servicio.
     * @param  nombreBucket     El nombre del bucket donde está almacenada la clave
     *                          PGP.
     * @param  rutaLlavePrivada La ruta de la clave privada dentro del bucket.
     * @return                  Un {@link InputStream} que contiene la clave privada
     *                          PGP.
     * @throws AthException     Si ocurre algún error al acceder al archivo en S3.
     */
    public static InputStream obtenerLlavePgpS3(S3Client s3Client, String nombreBucket, String rutaLlavePrivada) {
        LOGGER.log("[INFO] obtenerLlavePgpS3 [" + nombreBucket + "][" + rutaLlavePrivada + "]");
        GetObjectRequest getObjectRequest = UtilsLambda.getObjectRequest(nombreBucket, rutaLlavePrivada);
        try {
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            LOGGER.log("Existe, bucket y la llave! \n");
            if (s3Object != null) {
                return s3Object;
            }
            throw new AthException(AthCodigosError.C029.getCodigo(), AthCodigosError.C029.getMensaje());
        } catch (S3Exception e) {
            throw new AthException("[ERROR]", "[ERROR] Error al intentar acceder al archivo en S3: ", e);
        }
    }

    /**
     * Imprime un archivo en un bucket de S3.
     * 
     * @param  s3Client      El cliente de S3 para interactuar con el servicio.
     * @param  nombreBucket  El nombre del bucket donde se almacenará el archivo.
     * @param  rutaSalida    La ruta dentro del bucket donde se almacenará el
     *                       archivo.
     * @param  nombreArchivo El nombre del archivo a almacenar.
     * @param  fileContent   El contenido del archivo en forma de un arreglo de
     *                       bytes.
     * @param  contentType   El tipo de contenido del archivo.
     * @throws AthException  Si ocurre un error al intentar almacenar el archivo en
     *                       S3.
     */
    public static void printFiles(S3Client s3Client, String nombreBucket, String rutaSalida, String nombreArchivo,
            byte[] fileContent, String contentType) {
        LOGGER.log(String.format("[INFO] 8.printFiles [%s]%s", nombreArchivo, "\n"));
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileContent);
            int lengthDescifrado = byteArrayInputStream.available();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(nombreBucket)
                    .key(rutaSalida + nombreArchivo).contentLength((long) lengthDescifrado).contentType(contentType)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(byteArrayInputStream, lengthDescifrado));
            LOGGER.log("[INFO] Fin subir archivo descifrado a S3\n");
        } catch (S3Exception e) {
            throw new AthException("[ERROR][8.1]", "[ERROR] Error al obtener el archivo desde S3: " + e.getMessage(),
                    e);
        } catch (Exception e) {
            throw new AthException("[ERROR][8.2]",
                    "[ERROR] Error procesando el archivo de texto en la línea: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica la existencia de archivos en un bucket específico, con un prefijo
     * determinado.
     * 
     * @param  s3Client     El cliente de S3 para interactuar con el servicio.
     * @return              Una lista de objetos S3 que representan los archivos
     *                      encontrados en el bucket.
     * @throws AthException Si no se encuentran exactamente 2 archivos o si ocurre
     *                      algún error en la verificación.
     */
    public static List<S3Object> verificarArchivosEnBucket(S3Client s3Client) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(Constantes.NOMBRE_BUCKET_ARCHIVOS_ENTRADA).prefix(Constantes.RUTA_ENTRADA).build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
        // Obtener la lista de objetos en el bucket
        List<S3Object> s3Objects = listObjectsV2Response.contents();
        s3Objects = s3Objects.stream().filter(fileRecords -> !fileRecords.key().endsWith("/"))
                .collect(Collectors.toList());
        if (s3Objects.size() != 2) {
            throw new AthException("[ERROR]",
                    "[WARN] Se esperan exactamente 2 archivos en el bucket, pero se encontraron: " + s3Objects.size());
        }
        LOGGER.log("[INFO] El bucket contiene exactamente 2 archivos.");
        return s3Objects;
    }
}
