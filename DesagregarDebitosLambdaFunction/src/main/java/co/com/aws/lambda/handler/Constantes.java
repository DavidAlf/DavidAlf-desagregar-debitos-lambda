package co.com.aws.lambda.handler;

import co.com.ath.aws.commons.AthConstants;

public class Constantes {

	public static final String AUDITORIA_DIVIDENDOS = "historico_archivos_dividendos";

	public static final String QUERY_REGISTRAR_AUDITORIA = " INSERT INTO " + AthConstants.BD_BILLPAY + "."
			+ AUDITORIA_DIVIDENDOS
			+ " (nombre_archivo1,nombre_archivo2,hora_inicio,hora_fin,total_registros_archivo1,total_registros_archivo2,"
			+ " total_registros_duplicados,total_registros_fusionados,archivos_cargados) "
			+ " VALUES (?,?,?,?,?,?,?,?,?)";

	public static final String ARCHIVO_FUSIONADO = "00000177ACCAVAL_FUSIONADO";

	public static final String ARCHIVO_DIVIDENDOS = "DIVIDENDOS";

}
