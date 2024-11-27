package co.com.aws.lambda.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;

import co.com.ath.aws.commons.AthConstants;
import co.com.ath.aws.dto.SecretAWSConnectionDto;
import co.com.ath.aws.exception.AthCodigosError;
import co.com.ath.aws.exception.AthException;
import co.com.ath.aws.secretmanagerutil.ObtenerSecretoDB;
import co.com.aws.lambda.constants.Constantes;
import co.com.aws.lambda.dto.AuditoriaDividendosDto;

/**
 * Clase que tiene la logica para la gestion de auditoria en BD
 * 
 * @version 1.0
 * @since   2024-11-13
 */
public class AuditoriaDividendosDao {

	private static final LambdaLogger LOGGER = LambdaRuntime.getLogger();

	/**
	 * Método encargado de registra en base de datos en la tabla
	 * historico_archivos_dividendos datos de procesamiento de dividendos
	 * 
	 * @return boolean : true insercion exitosa - false error en la insercion
	 */
	public boolean registrarAuditoria(AuditoriaDividendosDto auditoriaDividendosDto) {
		LOGGER.log("[INFO] 10.1.Inicio registrarAuditoria\n");
		Connection conection = getConnection(ObtenerSecretoDB.obtenerSecreto(AthConstants.SECRET_NAME_BILLPAY),
				AthConstants.BD_BILLPAY);
		String queryRegistro = Constantes.QUERY_REGISTRAR_AUDITORIA;
		try (PreparedStatement stmt = conection.prepareStatement(queryRegistro)) {
			stmt.setString(1, auditoriaDividendosDto.getNombreArchivo1());
			stmt.setString(2, auditoriaDividendosDto.getNombreArchivo2());
			stmt.setTimestamp(3, auditoriaDividendosDto.getHoraInicio());
			stmt.setTimestamp(4, auditoriaDividendosDto.getHoraFin());
			stmt.setInt(5, auditoriaDividendosDto.getTotalRegistrosArchivo1());
			stmt.setInt(6, auditoriaDividendosDto.getTotalRegistrosArchivo2());
			stmt.setInt(7, auditoriaDividendosDto.getTotalRegistrosDuplicados());
			stmt.setInt(8, auditoriaDividendosDto.getTotalRegistrosFusionados());
			stmt.setInt(9, auditoriaDividendosDto.getArchivosCargados());
			if (stmt.executeUpdate() == 0) {
				LOGGER.log(AthConstants.ERROR_INSERT_TABLA + Constantes.AUDITORIA_DIVIDENDOS + " "
						+ "insercion auditoria no realizada");
				return false;
			}
		} catch (SQLException e) {
			LOGGER.log(AthConstants.ERROR_CONSULTA_TABLA + AthConstants.CONVENIOS_EJECUCION_GAP + " " + e.getMessage()
					+ ":::" + e);
			return false;
		}
		LOGGER.log("[INFO] 10.3.Registro Auditoria Exitoso\n");
		return true;
	}

	public static Connection getConnection(SecretAWSConnectionDto secret, String baseDeDatos) {
		try {
			LOGGER.log("[INFO] 10.2.Conectando a base de datos " + baseDeDatos);
			return DriverManager.getConnection("jdbc:mysql://" + secret.getHost() + ":" + secret.getPort(),
					secret.getUsername(), secret.getPassword());
		} catch (SQLException e) {
			LOGGER.log(AthConstants.ERROR_CONEXION + " " + e.getMessage() + ":::" + e);
			throw new AthException(AthCodigosError.C003.getCodigo(),
					AthCodigosError.getDescriptionByCode(AthCodigosError.C003.getCodigo()), e);
		}
	}
}