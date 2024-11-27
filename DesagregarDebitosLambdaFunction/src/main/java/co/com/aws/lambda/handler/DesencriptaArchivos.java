package co.com.aws.lambda.handler;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;

import co.com.ath.aws.cifrado.CifrarFraseUtil;
import co.com.ath.aws.cifrado.PgpDecryptionUtil;
import co.com.ath.aws.commons.AthConstants;
import co.com.ath.aws.commons.AthUtil;
import co.com.ath.aws.exception.AthCodigosError;
import co.com.ath.aws.exception.AthException;
import co.com.ath.aws.secretmanagerutil.SecretsManagerUtil;
import co.com.aws.lambda.constants.Constantes;
import co.com.aws.lambda.dto.AuditoriaDividendosDto;
import co.com.aws.lambda.util.UtilsLambda;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Clase encargada de la desencriptación de archivos PGP almacenados en S3,
 * procesamiento y clasificación de registros desde los archivos desencriptados.
 * <p>
 * La clase gestiona el proceso de descarga, desencriptación y clasificación de
 * archivos desde el almacenamiento S3, realizando la validación de claves
 * secretas, la obtención de claves privadas y la descifrado de archivos PGP.
 * También coordina la actualización de auditoría con la información sobre los
 * archivos procesados.
 * </p>
 * <p>
 * La desencriptación se realiza utilizando claves PGP y frases secretas
 * configuradas, con la verificación y el manejo adecuado de errores durante el
 * proceso.
 * </p>
 *
 * @author  David Alfonso
 * @version 1.0
 * @since   2024-11-21
 */
public class DesencriptaArchivos {

    private static final LambdaLogger LOGGER = LambdaRuntime.getLogger();

    private final S3Client s3Client;

    private final ClasificaRegistros clasificaRegistros;

    /**
     * Constructor de la clase que inicializa el cliente de S3 y el objeto encargado
     * de clasificar los registros.
     * 
     * @param s3Client El cliente de S3 utilizado para obtener los archivos.
     */
    public DesencriptaArchivos(S3Client s3Client) {
        this.clasificaRegistros = new ClasificaRegistros();
        this.s3Client = s3Client;
    }

    /**
     * Método principal para obtener los archivos desde S3, desencriptarlos y
     * procesarlos. También actualiza el objeto de auditoría con la información de
     * los archivos procesados.
     * 
     * @param archivosBucket         Lista de archivos S3 a procesar.
     * @param typesMaps              Mapas donde se almacenan los registros
     *                               clasificados.
     * @param auditoriaDividendosDto Objeto de auditoría para actualizar con la
     *                               información del procesamiento.
     */
    public void getFiles(List<S3Object> archivosBucket, Map<String, Map<String, String>> typesMaps,
            AuditoriaDividendosDto auditoriaDividendosDto) {
        LOGGER.log("[INFO] 1.getFiles\n");
        auditoriaDividendosDto.setArchivosCargados(2);
        AtomicBoolean firstRecord = new AtomicBoolean(false);
        AtomicInteger totalRecords = new AtomicInteger(0);
        archivosBucket.forEach(fileRecords -> {
            String srcFile = fileRecords.key();
            int lastSlashIndex = srcFile.lastIndexOf('/');
            String nombreArchivo = srcFile.substring(lastSlashIndex + 1).replace(AthConstants.PGP_EXTENSION, "");
            totalRecords.set(this.decryptionFile(srcFile, typesMaps));
            if (firstRecord.get()) {
                auditoriaDividendosDto.setNombreArchivo2(nombreArchivo);
                auditoriaDividendosDto.setTotalRegistrosArchivo2(totalRecords.get());
            } else {
                auditoriaDividendosDto.setNombreArchivo1(nombreArchivo);
                auditoriaDividendosDto.setTotalRegistrosArchivo1(totalRecords.get());
            }
            firstRecord.set(true);
        });
    }

    /**
     * Método encargado de desencriptar el archivo PGP especificado. Obtiene las
     * claves necesarias para la desencriptación y luego delega el procesamiento al
     * método adecuado.
     * 
     * @param  srcFile      El nombre del archivo PGP a desencriptar.
     * @param  typesMaps    El mapa donde se almacenarán los registros clasificados.
     * @return              El total de registros procesados.
     * @throws AthException Si ocurre un error en el proceso de desencriptación.
     */
    protected Integer decryptionFile(String srcFile, Map<String, Map<String, String>> typesMaps) {
        LOGGER.log("[INFO] 2.decryptionFile\n");
        int totalRecords = 0;
        try {
            if (Constantes.FRASE_SECRETAPGP == null || Constantes.FRASE_SECRETAPGP.isEmpty()) {
                throw new AthException(AthCodigosError.C021.getCodigo(),
                        "Error: No se encuentra configurada la frase secreta para descifrar los archivos pgp");
            }
            String llavePrivadaChiper = SecretsManagerUtil.getSecretString(AthConstants.KEY_SECRET_NAME_CIPHER,
                    AthConstants.KEY_SECRET_CIPHER);
            if (llavePrivadaChiper == null || llavePrivadaChiper.isEmpty()) {
                throw new AthException(AthCodigosError.C021.getCodigo(),
                        "Error: No se encuentra configurada la llave secreta para descifrar la frase secreta");
            }
            String fraseSecretaPgpDescifrada = CifrarFraseUtil.decrypt(Constantes.FRASE_SECRETAPGP, llavePrivadaChiper);
            if (fraseSecretaPgpDescifrada == null || fraseSecretaPgpDescifrada.isEmpty()) {
                throw new AthException(AthCodigosError.C021.getCodigo(),
                        "Error decrypt fraseSecretaPgp: No se encuentra configurada la frase secreta para descifrar los archivos pgp");
            }
            InputStream llavePrivada = UtilsLambda.obtenerLlavePgpS3(s3Client, Constantes.NOMBRE_BUCKET_LLAVES,
                    Constantes.RUTA_LLAVE_PRIVADAPGP);
            totalRecords = this.descifrarArchivoPgp(srcFile, llavePrivada, fraseSecretaPgpDescifrada, typesMaps);
            llavePrivada.close();
        } catch (Exception e) {
            throw new AthException("[ERROR][2]", String.format("[ERROR] %s ::: %s",
                    AthConstants.ERROR_GENERAL + e.getMessage(), AthUtil.getStackTraceMessage(e)));
        }
        return totalRecords;
    }

    /**
     * Método encargado de descifrar el archivo PGP desde S3. Utiliza la clave
     * privada y la frase secreta para realizar la desencriptación del archivo y
     * luego clasificar los registros obtenidos.
     * 
     * @param  srcFile                   El nombre del archivo en S3.
     * @param  llavePrivada              InputStream de la clave privada utilizada
     *                                   para la desencriptación.
     * @param  fraseSecretaPgpDescifrada La frase secreta PGP ya descifrada.
     * @param  typesMaps                 El mapa donde se almacenarán los registros
     *                                   clasificados.
     * @return                           El total de registros procesados.
     */
    protected Integer descifrarArchivoPgp(String srcFile, InputStream llavePrivada, String fraseSecretaPgpDescifrada,
            Map<String, Map<String, String>> typesMaps) {
        LOGGER.log("[INFO] 3.descifrarArchivoPgp");
        LOGGER.log("archivo, con nombre: [" + srcFile + "]\n");
        GetObjectRequest getObjectRequest = UtilsLambda.getObjectRequest(Constantes.NOMBRE_BUCKET_ARCHIVOS_ENTRADA,
                srcFile);
        try {
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            InputStream archivoDesCifrado = PgpDecryptionUtil.descifrarArchivo(s3Object, fraseSecretaPgpDescifrada,
                    llavePrivada);
            if (archivoDesCifrado == null) {
                throw new AthException("[ERROR][3.1]", "[ERROR] Error al desencriptar archivo no tiene contenido.");
            }
            return this.clasificaRegistros.processFiles(srcFile, archivoDesCifrado, typesMaps);
        } catch (S3Exception e) {
            throw new AthException("[ERROR][3.2]",
                    "[ERROR] Error al obtener el archivo desde S3: [" + srcFile + "] " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AthException("[ERROR][3.3]",
                    "[ERROR] Error procesando el archivo de texto en la línea: " + e.getMessage(), e);
        }
    }
}
