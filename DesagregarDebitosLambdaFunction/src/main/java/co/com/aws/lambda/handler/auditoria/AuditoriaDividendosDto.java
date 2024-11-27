package co.com.aws.lambda.handler.auditoria;

import java.sql.Timestamp;

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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getNombreArchivo1() {
		return nombreArchivo1;
	}

	public void setNombreArchivo1(String nombreArchivo1) {
		this.nombreArchivo1 = nombreArchivo1;
	}

	public String getNombreArchivo2() {
		return nombreArchivo2;
	}

	public void setNombreArchivo2(String nombreArchivo2) {
		this.nombreArchivo2 = nombreArchivo2;
	}

	public Timestamp getHoraInicio() {
		return horaInicio;
	}

	public void setHoraInicio(Timestamp horaInicio) {
		this.horaInicio = horaInicio;
	}

	public Timestamp getHoraFin() {
		return horaFin;
	}

	public void setHoraFin(Timestamp horaFin) {
		this.horaFin = horaFin;
	}

	public int getTotalRegistrosArchivo1() {
		return totalRegistrosArchivo1;
	}

	public void setTotalRegistrosArchivo1(int totalRegistrosArchivo1) {
		this.totalRegistrosArchivo1 = totalRegistrosArchivo1;
	}

	public int getTotalRegistrosArchivo2() {
		return totalRegistrosArchivo2;
	}

	public void setTotalRegistrosArchivo2(int totalRegistrosArchivo2) {
		this.totalRegistrosArchivo2 = totalRegistrosArchivo2;
	}

	public int getTotalRegistrosDuplicados() {
		return totalRegistrosDuplicados;
	}

	public void setTotalRegistrosDuplicados(int totalRegistrosDuplicados) {
		this.totalRegistrosDuplicados = totalRegistrosDuplicados;
	}

	public int getTotalRegistrosFusionados() {
		return totalRegistrosFusionados;
	}

	public void setTotalRegistrosFusionados(int totalRegistrosFusionados) {
		this.totalRegistrosFusionados = totalRegistrosFusionados;
	}

	public int getArchivosCargados() {
		return archivosCargados;
	}

	public void setArchivosCargados(int archivosCargados) {
		this.archivosCargados = archivosCargados;
	}

	@Override
	public String toString() {
		return "AuditoriaDividendosDto [nombreArchivo1=" + nombreArchivo1 + ", nombreArchivo2=" + nombreArchivo2
				+ ", horaInicio=" + horaInicio + ", horaFin=" + horaFin + ", totalRegistrosArchivo1="
				+ totalRegistrosArchivo1 + ", totalRegistrosArchivo2=" + totalRegistrosArchivo2
				+ ", totalRegistrosDuplicados=" + totalRegistrosDuplicados + ", totalRegistrosFusionados="
				+ totalRegistrosFusionados + ", archivosCargados=" + archivosCargados + "]";
	}
}
