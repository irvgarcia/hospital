package com.hospital.backendHospital;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Clase base de las pruebas de integracion (*IT). Requiere Docker en ejecucion.
 * Todas las subclases comparten el mismo contexto de Spring y, por lo tanto, el mismo contenedor MySQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class AbstractIntegrationTest {
}
