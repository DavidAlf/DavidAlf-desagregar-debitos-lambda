package co.com.aws.lambda.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import co.com.ath.aws.dto.SecretAWSConnectionDto;
import co.com.ath.aws.exception.AthException;
import co.com.ath.aws.secretmanagerutil.ObtenerSecretoDB;
import co.com.aws.lambda.dto.AuditoriaDividendosDto;

@ExtendWith(MockitoExtension.class)
class AuditoriaDividendosDaoTest {

	@Mock
	PreparedStatement preparedStatementMock;

	@Mock
	Connection connectionMock;

	@InjectMocks
	private AuditoriaDividendosDao auditoriaDividendosDao;

	private AuditoriaDividendosDto auditoriaDividendosDto;

	private SecretAWSConnectionDto secretAWSConnectionDto;

	@BeforeEach
	void setUp() {
		auditoriaDividendosDto = new AuditoriaDividendosDto();
		auditoriaDividendosDto.setNombreArchivo1("archivo1.csv");
		auditoriaDividendosDto.setNombreArchivo2("archivo2.csv");
		auditoriaDividendosDto.setHoraInicio(new Timestamp(System.currentTimeMillis()));
		auditoriaDividendosDto.setHoraFin(new Timestamp(System.currentTimeMillis()));
		auditoriaDividendosDto.setTotalRegistrosArchivo1(100);
		auditoriaDividendosDto.setTotalRegistrosArchivo2(200);
		auditoriaDividendosDto.setTotalRegistrosDuplicados(50);
		auditoriaDividendosDto.setTotalRegistrosFusionados(250);
		auditoriaDividendosDto.setArchivosCargados(1);
		secretAWSConnectionDto = new SecretAWSConnectionDto();
		secretAWSConnectionDto.setHost("hostError");
		secretAWSConnectionDto.setPort("3306");
		secretAWSConnectionDto.setUsername("user");
		secretAWSConnectionDto.setPassword("password");
	}

	/**
	 * Test encargado de validar la correcta interaccion de los componentes y
	 * metodos estaticos al momento de realizar una insercion exitosa
	 **/
	@Test
	void registraAuditoriaOk() throws SQLException {
		try (MockedStatic<ObtenerSecretoDB> mockObtenerSecreto = mockStatic(ObtenerSecretoDB.class)) {
			mockObtenerSecreto.when(() -> ObtenerSecretoDB.obtenerSecreto(anyString()))
					.thenReturn(secretAWSConnectionDto);
			try (MockedStatic<DriverManager> mockDriverManager = mockStatic(DriverManager.class)) {
				mockDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
						.thenReturn(connectionMock);
				when(connectionMock.prepareStatement(anyString())).thenReturn(preparedStatementMock);
				when(preparedStatementMock.executeUpdate()).thenReturn(1);
				boolean resultado = auditoriaDividendosDao.registrarAuditoria(auditoriaDividendosDto);
				assertTrue(resultado);
				verify(preparedStatementMock, times(1)).executeUpdate();
			}
		}
	}

	/**
	 * Test encargado de validar la correcta interaccion de los componentes y
	 * metodos estaticos al momento de realizar una insercion no exitosa
	 **/
	@Test
	void noRegistraAuditoria() throws SQLException {
		try (MockedStatic<ObtenerSecretoDB> mockObtenerSecreto = mockStatic(ObtenerSecretoDB.class)) {
			mockObtenerSecreto.when(() -> ObtenerSecretoDB.obtenerSecreto(anyString()))
					.thenReturn(secretAWSConnectionDto);
			try (MockedStatic<DriverManager> mockDriverManager = mockStatic(DriverManager.class)) {
				mockDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
						.thenReturn(connectionMock);
				when(connectionMock.prepareStatement(anyString())).thenReturn(preparedStatementMock);
				when(preparedStatementMock.executeUpdate()).thenReturn(0);
				boolean resultado = auditoriaDividendosDao.registrarAuditoria(auditoriaDividendosDto);
				assertFalse(resultado);
				verify(preparedStatementMock, times(1)).executeUpdate();
			}
		}
	}

	/**
	 * Test encargado de validar la correcta interaccion de los componentes y
	 * metodos estaticos al momento de lanzar una excepcion al obtener una conexion
	 **/
	@Test
	void connectionNull() throws SQLException {
		try (MockedStatic<ObtenerSecretoDB> mockObtenerSecreto = mockStatic(ObtenerSecretoDB.class)) {
			mockObtenerSecreto.when(() -> ObtenerSecretoDB.obtenerSecreto(anyString()))
					.thenReturn(secretAWSConnectionDto);
			try (MockedStatic<DriverManager> mockDriverManager = mockStatic(DriverManager.class)) {
				mockDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
						.thenThrow(SQLException.class);
				assertThrows(AthException.class,
						() -> auditoriaDividendosDao.registrarAuditoria(auditoriaDividendosDto));
			}
		}
	}

	/**
	 * Test encargado de validar la correcta interaccion de los componentes y
	 * metodos estaticos al momento de lanzar una excepcion al realizar la insercion
	 * de un registro
	 **/
	@Test
	void excepcionInsert() throws SQLException {
		try (MockedStatic<ObtenerSecretoDB> mockObtenerSecreto = mockStatic(ObtenerSecretoDB.class)) {
			mockObtenerSecreto.when(() -> ObtenerSecretoDB.obtenerSecreto(anyString()))
					.thenReturn(secretAWSConnectionDto);
			try (MockedStatic<DriverManager> mockDriverManager = mockStatic(DriverManager.class)) {
				mockDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
						.thenReturn(connectionMock);
				when(connectionMock.prepareStatement(anyString())).thenReturn(preparedStatementMock);
				when(preparedStatementMock.executeUpdate()).thenThrow(SQLException.class);
				assertFalse(auditoriaDividendosDao.registrarAuditoria(auditoriaDividendosDto));
			}
		}
	}
}
