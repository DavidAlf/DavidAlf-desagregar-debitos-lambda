package co.com.aws.lambda.dto;

import java.sql.Timestamp;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

@Generated
@Getter
@Setter
public class AuditoriaDividendosDto {

	private int id;

	private String nombreArchivo1;

	private String nombreArchivo2;

	private Timestamp horaInicio;

	private Timestamp horaFin;

	private int totalRegistrosArchivo1;

	private int totalRegistrosArchivo2;

	private int totalRegistrosDuplicados;

	private int totalRegistrosFusionados;

	private int archivosCargados;

	@Override
	public String toString() {
		return "AuditoriaDividendosDto [nombreArchivo1=" + nombreArchivo1 + ", nombreArchivo2=" + nombreArchivo2
				+ ", horaInicio=" + horaInicio + ", horaFin=" + horaFin + ", totalRegistrosArchivo1="
				+ totalRegistrosArchivo1 + ", totalRegistrosArchivo2=" + totalRegistrosArchivo2
				+ ", totalRegistrosDuplicados=" + totalRegistrosDuplicados + ", totalRegistrosFusionados="
				+ totalRegistrosFusionados + ", archivosCargados=" + archivosCargados + "]";
	}
}
