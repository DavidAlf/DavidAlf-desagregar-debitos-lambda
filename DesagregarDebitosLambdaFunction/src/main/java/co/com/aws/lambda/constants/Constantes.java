package co.com.aws.lambda.constants;

import co.com.ath.aws.commons.AthConstants;
import lombok.Generated;

/**
 * Clase de constantes utilizadas a lo largo del proyecto para evitar la
 * repetición de valores estáticos. Contiene las configuraciones de los nombres
 * de los archivos, rutas de entrada y salida, y nombres de los buckets
 * utilizados en los procesos de la Lambda para la gestión de auditoría y
 * procesamiento de archivos.
 * <p>
 * Esta clase incluye valores predeterminados que se pueden sobrescribir
 * mediante las variables de entorno del sistema.
 * </p>
 * 
 * @author  David Alfonso
 * @version 1.0
 * @since   2024-11-21
 */
@Generated
public class Constantes {

	private Constantes() {
		throw new UnsupportedOperationException("Esta clase no debe ser instanciada");
	}

	/**
	 * Nombre de la tabla de auditoría para los dividendos.
	 */
	public static final String AUDITORIA_DIVIDENDOS = "historico_archivos_dividendos";

	/**
	 * Consulta SQL para registrar la auditoría de los dividendos.
	 */
	public static final String QUERY_REGISTRAR_AUDITORIA = " INSERT INTO " + AthConstants.BD_BILLPAY + "."
			+ AUDITORIA_DIVIDENDOS
			+ " (nombre_archivo1,nombre_archivo2,hora_inicio,hora_fin,total_registros_archivo1,total_registros_archivo2,"
			+ " total_registros_duplicados,total_registros_fusionados,archivos_cargados) "
			+ " VALUES (?,?,?,?,?,?,?,?,?)";

	/**
	 * Identificador para el archivo fusionado.
	 */
	public static final String ARCHIVO_FUSIONADO = "00000177ACCAVAL_FUSIONADO";

	/**
	 * Identificador para los archivos de dividendos.
	 */
	public static final String ARCHIVO_DIVIDENDOS = "DIVIDENDOS";

	/**
	 * Nombre del bucket de llaves de PGP utilizado en el proyecto. Su valor puede
	 * ser sobrescrito mediante la variable de entorno BUCKET_LLAVES.
	 */
	public static final String NOMBRE_BUCKET_LLAVES = (System.getenv("BUCKET_LLAVES") != null)
			? System.getenv("BUCKET_LLAVES")
			: "key-pairs-bpa-dev";

	/**
	 * Nombre del bucket de archivos de entrada utilizado en el proyecto. Su valor
	 * puede ser sobrescrito mediante la variable de entorno
	 * BUCKET_ARCHIVOS_ENTRADA.
	 */
	public static final String NOMBRE_BUCKET_ARCHIVOS_ENTRADA = (System.getenv("BUCKET_ARCHIVOS_ENTRADA") != null)
			? System.getenv("BUCKET_ARCHIVOS_ENTRADA")
			: "bpa-informe-recaudo-avc-dividendos-dev";

	/**
	 * Nombre del bucket de archivos de salida utilizado en el proyecto. Su valor
	 * puede ser sobrescrito mediante la variable de entorno BUCKET_ARCHIVOS_SALIDA.
	 */
	public static final String NOMBRE_BUCKET_ARCHIVOS_SALIDA = (System.getenv("BUCKET_ARCHIVOS_SALIDA") != null)
			? System.getenv("BUCKET_ARCHIVOS_SALIDA")
			: "bpa-informe-recaudo-ath-dev";

	/**
	 * Ruta de entrada de los archivos a procesar, puede ser sobrescrita mediante la
	 * variable de entorno ENTRADA.
	 */
	public static final String RUTA_ENTRADA = (System.getenv("ENTRADA") != null) ? System.getenv("ENTRADA")
			: "carpeta_entrada/";

	/**
	 * Ruta de salida donde se guardarán los archivos procesados, puede ser
	 * sobrescrita mediante la variable de entorno SALIDA.
	 */
	public static final String RUTA_SALIDA = (System.getenv("SALIDA") != null) ? System.getenv("SALIDA")
			: "carpeta_salida/";

	/**
	 * Ruta de los archivos procesados, puede ser sobrescrita mediante la variable
	 * de entorno PROCESADOS.
	 */
	public static final String RUTA_PROCESADOS = (System.getenv("PROCESADOS") != null) ? System.getenv("PROCESADOS")
			: "carpeta_procesado/";

	/**
	 * Frase secreta para PGP, puede ser sobrescrita mediante la variable de entorno
	 * FRASE_SECRETA_PGP.
	 */
	public static final String FRASE_SECRETAPGP = (System.getenv("FRASE_SECRETA_PGP") != null)
			? System.getenv("FRASE_SECRETA_PGP")
			: "frase_secreta";

	/**
	 * Ruta de la llave privada PGP, puede ser sobrescrita mediante la variable de
	 * entorno RUTA_LLAVE_PRIVADA_PGP.
	 */
	public static final String RUTA_LLAVE_PRIVADAPGP = (System.getenv("RUTA_LLAVE_PRIVADA_PGP") != null)
			? System.getenv("RUTA_LLAVE_PRIVADA_PGP")
			: "ruta_llave_privada";

	/**
	 * Ruta de la llave pública PGP, puede ser sobrescrita mediante la variable de
	 * entorno RUTA_LLAVE_PUBLICA_PGP.
	 */
	public static final String RUTA_LLAVE_PUBLICAPGP = (System.getenv("RUTA_LLAVE_PUBLICA_PGP") != null)
			? System.getenv("RUTA_LLAVE_PUBLICA_PGP")
			: "ruta_llave_publica";
}
