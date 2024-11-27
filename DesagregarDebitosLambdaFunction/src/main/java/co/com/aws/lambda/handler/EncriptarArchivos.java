package co.com.aws.lambda.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;
import com.amazonaws.util.IOUtils;

import co.com.ath.aws.cifrado.PgpEncryptionUtil;
import co.com.ath.aws.commons.AthConstants;
import co.com.ath.aws.exception.AthException;
import co.com.aws.lambda.constants.Constantes;
import co.com.aws.lambda.util.UtilsLambda;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Clase encargada de la encriptación de registros y su posterior almacenamiento
 * en S3, utilizando la tecnología de cifrado PGP.
 * <p>
 * La clase se enfoca en el proceso de generar archivos encriptados a partir de
 * registros proporcionados, utilizando una clave pública almacenada en un
 * bucket de S3, y luego almacenando estos archivos encriptados en un bucket de
 * salida en S3.
 * </p>
 * 
 * @author  David Alfonso
 * @version 1.0
 * @since   2024-11-21
 */
public class EncriptarArchivos {

    private static final LambdaLogger LOGGER = LambdaRuntime.getLogger();

    private final S3Client s3Client;

    /**
     * Constructor de la clase que inicializa el cliente de S3 utilizado para
     * obtener y almacenar los archivos.
     * 
     * @param s3Client El cliente de S3 utilizado para obtener las claves públicas y
     *                 almacenar los archivos cifrados.
     */
    public EncriptarArchivos(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Método que itera sobre los registros proporcionados, los convierte en
     * archivos de texto y los cifra utilizando una clave pública PGP almacenada en
     * S3. Los archivos encriptados se almacenan en un bucket de salida en S3.
     * 
     * @param  typesMaps    Mapa que contiene los registros a ser encriptados,
     *                      organizados por nombre de archivo.
     * @throws AthException Si ocurre un error durante el proceso de encriptación.
     */
    public void getEncrypRecords(Map<String, Map<String, String>> typesMaps) {
        LOGGER.log("[INFO] 6.getEncrypRecords\n");
        typesMaps.entrySet().parallelStream().forEach(entry -> {
            try {
                Map<String, String> value = entry.getValue();
                String nombreArchivo = entry.getKey() + ".txt";
                byte[] fileContent = value.values().stream().collect(Collectors.joining("\n"))
                        .getBytes(StandardCharsets.UTF_8);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileContent);
                InputStream llavePublica = UtilsLambda.obtenerLlavePgpS3(s3Client, Constantes.NOMBRE_BUCKET_LLAVES,
                        Constantes.RUTA_LLAVE_PUBLICAPGP);
                cifrarArchivoPgp(nombreArchivo, byteArrayInputStream, llavePublica);
                llavePublica.close();
            } catch (Exception e) {
                throw new AthException("[ERROR][6.1]", "[ERROR] Desencriptando el archivo" + e.getMessage(), e);
            }
        });
    }

    /**
     * Método encargado de encriptar un archivo utilizando PGP. El archivo es
     * cifrado con la clave pública proporcionada y luego almacenado en el bucket de
     * salida en S3.
     * 
     * @param  nombreArchivo     Nombre del archivo a ser encriptado.
     * @param  archivoDescifrado El contenido del archivo a ser encriptado.
     * @param  llavePublica      El InputStream de la clave pública utilizada para
     *                           el cifrado PGP.
     * @throws IOException       Si ocurre un error durante la lectura o escritura
     *                           del archivo.
     * @throws AthException      Si ocurre un error durante el proceso de
     *                           encriptación.
     */
    protected void cifrarArchivoPgp(String nombreArchivo, InputStream archivoDescifrado, InputStream llavePublica)
            throws IOException {
        LOGGER.log(String.format("[INFO] 7.cifrarArchivoPgp [%s]%s", nombreArchivo, "\n"));
        String nombreArchivoCifrado = nombreArchivo + AthConstants.PGP_EXTENSION;
        Integer lengthArchivoDescifrado = archivoDescifrado.available();
        try {
            String contentType = "application/octet-stream";
            InputStream archivoCifrado = PgpEncryptionUtil.cifrarArchivo(archivoDescifrado, lengthArchivoDescifrado,
                    llavePublica);
            byte[] fileContent = IOUtils.toByteArray(archivoCifrado);
            UtilsLambda.printFiles(s3Client, Constantes.NOMBRE_BUCKET_ARCHIVOS_SALIDA, Constantes.RUTA_SALIDA,
                    nombreArchivoCifrado, fileContent, contentType);
        } catch (Exception e) {
            throw new AthException("[ERROR][7.1]", "[ERROR] error al cifrarArchivoPgp " + e.getMessage(), e);
        }
    }
}
