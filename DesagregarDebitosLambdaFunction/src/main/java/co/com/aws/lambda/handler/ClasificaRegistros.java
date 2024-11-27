package co.com.aws.lambda.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;

import co.com.ath.aws.commons.AthConstants;
import co.com.ath.aws.exception.AthException;
import co.com.aws.lambda.constants.Constantes;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Clase encargada de clasificar los registros de los archivos procesados en la
 * Lambda. Esta clase se encarga de leer los archivos, procesar las líneas y
 * clasificar los registros en mapas únicos y duplicados, dependiendo de su
 * aparición en el archivo. Además, maneja las excepciones relacionadas con los
 * procesos de lectura y almacenamiento en S3, asegurando que los datos sean
 * clasificados correctamente en los archivos de salida correspondientes.
 * <p>
 * Se enfoca en el procesamiento de archivos de texto y en la gestión de
 * registros de factura, diferenciando entre registros únicos y duplicados, y
 * gestionando la creación de mensajes para los registros duplicados.
 * </p>
 *
 * @author  David Alfonso
 * @version 1.0
 * @since   2024-11-21
 */
public class ClasificaRegistros {

    private static final LambdaLogger LOGGER = LambdaRuntime.getLogger();

    /**
     * Método principal para procesar los archivos. Este método invoca otros métodos
     * para leer el archivo y clasificar los registros como únicos o duplicados.
     *
     * @param  nombreArchivo El nombre del archivo a procesar.
     * @param  descifrado    El InputStream con el contenido del archivo
     *                       desencriptado.
     * @param  typesMaps     El mapa que contiene los registros clasificados.
     * @return               El total de registros procesados.
     */
    public Integer processFiles(String srcFile, InputStream descifrado, Map<String, Map<String, String>> typesMaps) {
        int totalRecords = 0;
        LOGGER.log("[INFO] 4.processFiles\n");
        int lastSlashIndex = srcFile.lastIndexOf('/');
        String nombreArchivo = srcFile.substring(lastSlashIndex + 1).replace(AthConstants.PGP_EXTENSION, "");
        totalRecords = createMapsUniqueAndDuplicate(nombreArchivo, descifrado, typesMaps);
        return totalRecords;
    }

    /**
     * Método encargado de crear los mapas para registros únicos y duplicados. Este
     * método lee el archivo línea por línea y procesa cada registro.
     *
     * @param  nombreArchivo El nombre del archivo.
     * @param  descifrado    El InputStream con el contenido del archivo
     *                       desencriptado.
     * @param  typesMaps     El mapa que contiene los registros clasificados.
     * @return               El número total de registros procesados.
     */
    protected Integer createMapsUniqueAndDuplicate(String nombreArchivo, InputStream descifrado,
            Map<String, Map<String, String>> typesMaps) {
        LOGGER.log("[INFO] 5.createMapsUniqueAndDuplicate\n");
        boolean firstLineProcessed = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(descifrado))) {
            String line;
            int lineNumber = 0;
            initializeMaps(typesMaps);
            Map<String, String> firstOccurrence = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() < 13)
                    continue;
                String numFactura = line.substring(1, 13);
                String key = nombreArchivo + "_" + lineNumber;
                String value = line;
                processLine(firstLineProcessed, numFactura, key, value, firstOccurrence, typesMaps);
                lineNumber++;
                if (!firstLineProcessed)
                    firstLineProcessed = true;
            }
            return lineNumber;
        } catch (S3Exception e) {
            throw new AthException("[ERROR][5.1]", "[ERROR] Error al obtener el archivo desde S3: " + e.getMessage(),
                    e);
        } catch (IOException e) {
            throw new AthException("[ERROR][5.2]",
                    "[ERROR] Error procesando el archivo de texto en la línea: " + e.getMessage(), e);
        }
    }

    /**
     * Método que inicializa los mapas donde se guardarán los registros
     * clasificados.
     * 
     * @param typesMaps El mapa donde se almacenarán los registros clasificados.
     */
    protected void initializeMaps(Map<String, Map<String, String>> typesMaps) {
        typesMaps.putIfAbsent(Constantes.ARCHIVO_FUSIONADO, new LinkedHashMap<>());
        typesMaps.putIfAbsent(Constantes.ARCHIVO_DIVIDENDOS, new LinkedHashMap<>());
    }

    /**
     * Método encargado de procesar una línea de un archivo. Según si es la primera
     * línea o una línea subsecuente, se clasifica el registro como único o
     * duplicado.
     * 
     * @param  firstLineProcessed Bandera que indica si la primera línea ya fue
     *                            procesada.
     * @param  numFactura         El número de factura extraído de la línea.
     * @param  key                La clave única que identifica la línea en el
     *                            archivo.
     * @param  value              El valor de la línea.
     * @param  firstOccurrence    Mapa que guarda la primera ocurrencia de las
     *                            facturas.
     * @param  typesMaps          El mapa donde se almacenarán los registros
     *                            clasificados.
     * @param  lineNumber         El número de la línea actual.
     * @return                    El número de la línea siguiente.
     */
    protected void processLine(boolean firstLineProcessed, String numFactura, String key, String value,
            Map<String, String> firstOccurrence, Map<String, Map<String, String>> typesMaps) {
        if (!firstLineProcessed) {
            processFirstLine(key, value, typesMaps);
        } else {
            processSubsequentLines(numFactura, key, value, firstOccurrence, typesMaps);
        }
    }

    /**
     * Método encargado de procesar la primera línea de un archivo. Si el archivo
     * aún no contiene registros fusionados, agrega la primera línea al mapa de
     * registros fusionados.
     * 
     * @param key       La clave única que identifica la línea.
     * @param value     El valor de la línea.
     * @param typesMaps El mapa donde se almacenarán los registros clasificados.
     */
    protected void processFirstLine(String key, String value, Map<String, Map<String, String>> typesMaps) {
        if (typesMaps.get(Constantes.ARCHIVO_FUSIONADO) == null
                || typesMaps.get(Constantes.ARCHIVO_FUSIONADO).isEmpty()) {
            typesMaps.get(Constantes.ARCHIVO_FUSIONADO).put(key, value);
        }
    }

    /**
     * Método que procesa las líneas subsecuentes de un archivo. Si una factura ya
     * ha sido procesada previamente, se considera un registro duplicado y se agrega
     * al mapa de dividendos. Si no, se agrega al mapa de registros fusionados.
     * 
     * @param  numFactura      El número de factura extraído de la línea.
     * @param  key             La clave única que identifica la línea.
     * @param  value           El valor de la línea.
     * @param  firstOccurrence Mapa que guarda la primera ocurrencia de las
     *                         facturas.
     * @param  typesMaps       El mapa donde se almacenarán los registros
     *                         clasificados.
     * @param  lineNumber      El número de la línea actual.
     * @return                 El número de la línea siguiente.
     */
    protected void processSubsequentLines(String numFactura, String key, String value,
            Map<String, String> firstOccurrence, Map<String, Map<String, String>> typesMaps) {
        if (firstOccurrence.containsKey(numFactura)) {
            String existingKey = firstOccurrence.get(numFactura);
            typesMaps.get(Constantes.ARCHIVO_DIVIDENDOS).put(key, createOutText(existingKey, value));
        } else {
            firstOccurrence.put(numFactura, key);
            typesMaps.get(Constantes.ARCHIVO_FUSIONADO).put(key, value);
        }
    }

    /**
     * Método que crea un mensaje de salida para los registros duplicados. Este
     * mensaje contiene información sobre el número de factura y el valor de la
     * factura duplicada.
     * 
     * @param  key   La clave única que identifica la línea.
     * @param  value El valor de la línea.
     * @return       Un mensaje indicando que la factura se encuentra repetida en el
     *               archivo.
     */
    protected String createOutText(String key, String value) {
        String param = key.toUpperCase().contains("O") ? "5402ORDINARIO.dat" : "0177PREFERENCIAL.dat";
        String numFactura = value.substring(1, 13);
        String valFactura = value.substring(89, 101);
        return String.format("El numero de factura [%s] con el valor [%s] se encuentra repetido en el archivo [%s].",
                numFactura, valFactura, param);
    }
}
