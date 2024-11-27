package co.com.aws.lambda.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import co.com.aws.lambda.constants.Constantes;

class ClasificaRegistrosTest {

        @InjectMocks
        private ClasificaRegistros clasificaRegistros;

        private Map<String, Map<String, String>> typesMaps;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
        }

        @Test
        void testCreateMapsUniqueAndDuplicate_successfulProcessing() {
                // Arrange
                String nombreArchivo = "test-file.txt";
                String fileContent = "line1line2line3";
                InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
                typesMaps = new HashMap<>();
                // Simulamos algunos registros de ejemplo
                Map<String, String> innerMap = new HashMap<>();
                innerMap.put("line1", "value1");
                innerMap.put("line2", "value2");
                typesMaps.put("type1", innerMap);
                ClasificaRegistros realClasificaRegistros = new ClasificaRegistros();
                ClasificaRegistros spyClasificaRegistros = spy(realClasificaRegistros);
                // Act
                spyClasificaRegistros.createMapsUniqueAndDuplicate(nombreArchivo, inputStream, typesMaps);
                // Assert
                assertNotNull(typesMaps.get(Constantes.ARCHIVO_FUSIONADO),
                                "El mapa de ARCHIVO_FUSIONADO no debe ser nulo");
                assertTrue(typesMaps.get(Constantes.ARCHIVO_FUSIONADO).containsKey("test-file.txt_0"),
                                "El mapa de ARCHIVO_FUSIONADO debe contener la clave esperada");
        }

        @Test
        void testProcessFiles_emptyOrInvalidFile() {
                // Arrange
                String nombreArchivo = "test-file.txt";
                String fileContent = "";
                InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
                typesMaps = new HashMap<>();
                ClasificaRegistros realClasificaRegistros = new ClasificaRegistros();
                ClasificaRegistros spyClasificaRegistros = spy(realClasificaRegistros);
                // Act
                Integer totalRecords = spyClasificaRegistros.processFiles(nombreArchivo, inputStream, typesMaps);
                // Assert
                assertEquals(0, totalRecords,
                                "El número total de registros procesados debe ser 0 para un archivo vacío o inválido.");
        }

        @Test
        void testInitializeMaps() {
                // Arrange
                typesMaps = new HashMap<>();
                clasificaRegistros = new ClasificaRegistros();
                // Act
                clasificaRegistros.initializeMaps(typesMaps);
                // Assert
                assertNotNull(typesMaps.get(Constantes.ARCHIVO_FUSIONADO),
                                "El mapa 'ARCHIVO_FUSIONADO' debe estar inicializado.");
                assertNotNull(typesMaps.get(Constantes.ARCHIVO_DIVIDENDOS),
                                "El mapa 'ARCHIVO_DIVIDENDOS' debe estar inicializado.");
                assertTrue(typesMaps.get(Constantes.ARCHIVO_FUSIONADO) instanceof LinkedHashMap,
                                "El mapa 'ARCHIVO_FUSIONADO' debe ser un LinkedHashMap.");
                assertTrue(typesMaps.get(Constantes.ARCHIVO_DIVIDENDOS) instanceof HashMap,
                                "El mapa 'ARCHIVO_DIVIDENDOS' debe ser un HashMap.");
        }

        @Test
        void testProcessFirstLine() {
                // Arrange
                typesMaps = new HashMap<>();
                Map<String, String> fusionadoMap = new LinkedHashMap<>();
                typesMaps.put(Constantes.ARCHIVO_FUSIONADO, fusionadoMap);
                clasificaRegistros = new ClasificaRegistros();
                String key = "test-key";
                String value = "test-value";
                // Act
                clasificaRegistros.processFirstLine(key, value, typesMaps);
                // Assert
                assertTrue(typesMaps.get(Constantes.ARCHIVO_FUSIONADO).containsKey(key),
                                "El mapa 'ARCHIVO_FUSIONADO' debe contener la clave del primer registro.");
                assertEquals(value, typesMaps.get(Constantes.ARCHIVO_FUSIONADO).get(key),
                                "El valor asociado con la clave debe ser el esperado.");
        }

        @Test
        void testCreateOutText() {
                // Arrange
                String key = "test-key";
                String value = "line1" + " ".repeat(75) + "1234567890123456789012345678901234567890" + " ".repeat(40);
                clasificaRegistros = new ClasificaRegistros();
                // Act
                String result = clasificaRegistros.createOutText(key, value);
                // Assert
                assertNotNull(result, "El texto generado no debe ser nulo.");
                assertTrue(result.contains("El numero de factura"),
                                "El mensaje generado debe contener 'El numero de factura'.");
                assertTrue(result.contains("con el valor"), "El mensaje generado debe contener 'con el valor'.");
        }
}
