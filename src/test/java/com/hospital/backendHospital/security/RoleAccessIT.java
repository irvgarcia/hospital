package com.hospital.backendHospital.security;

import com.hospital.backendHospital.AbstractIntegrationTest;
import com.hospital.backendHospital.models.entity.RoleEnum;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.hospital.backendHospital.models.entity.RoleEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Matriz de acceso por rol (C9, A12) sobre todos los endpoints existentes.
 * - Rol permitido: la seguridad deja pasar (la respuesta no es 401 ni 403; puede ser 2xx, 400 o 404 segun los datos).
 * - Rol no permitido: 403.
 * - Sin token: 401.
 * Al agregar un endpoint hay que agregarlo aqui y en SecurityConfig.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleAccessIT extends AbstractIntegrationTest {

    private static final Set<RoleEnum> ALL = Set.of(ADMIN, RECEPTIONIST, DOCTOR, PATIENT);
    private static final Set<RoleEnum> STAFF = Set.of(ADMIN, RECEPTIONIST, DOCTOR);
    private static final String MISSING_ID = "999999";

    private final Map<RoleEnum, String> tokens = new EnumMap<>(RoleEnum.class);

    record Endpoint(HttpMethod method, String path, Set<RoleEnum> allowed) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    static List<Endpoint> endpoints() {
        return List.of(
                // Recepcionistas
                new Endpoint(HttpMethod.GET, "/api/v1/receptionists", Set.of(ADMIN)),
                new Endpoint(HttpMethod.POST, "/api/v1/receptionists/create", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/receptionists/" + MISSING_ID + "/update", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/receptionists/" + MISSING_ID + "/deactivate", Set.of(ADMIN)),
                // Pacientes
                new Endpoint(HttpMethod.GET, "/api/v1/patients", STAFF),
                new Endpoint(HttpMethod.POST, "/api/v1/patients/create", Set.of(PATIENT)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/patients/update/me", Set.of(PATIENT)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/patients/" + MISSING_ID + "/deactivate", Set.of(ADMIN)),
                // Doctores
                new Endpoint(HttpMethod.GET, "/api/v1/doctors", ALL),
                new Endpoint(HttpMethod.POST, "/api/v1/doctors", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/doctors/" + MISSING_ID + "/update", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/doctors/" + MISSING_ID + "/deactivate", Set.of(ADMIN)),
                // Especialidades
                new Endpoint(HttpMethod.GET, "/api/v1/specialties", ALL),
                new Endpoint(HttpMethod.GET, "/api/v1/specialties/search?name=x", ALL),
                new Endpoint(HttpMethod.POST, "/api/v1/specialties", Set.of(ADMIN)),
                // Horarios
                new Endpoint(HttpMethod.GET, "/api/v1/doctorSchedules", ALL),
                new Endpoint(HttpMethod.POST, "/api/v1/doctorSchedules", Set.of(ADMIN)),
                // Citas
                new Endpoint(HttpMethod.GET, "/api/v1/appointments", STAFF),
                new Endpoint(HttpMethod.GET, "/api/v1/appointments/patients/me", Set.of(PATIENT)),
                new Endpoint(HttpMethod.GET, "/api/v1/appointments/patients/active/me", Set.of(PATIENT)),
                new Endpoint(HttpMethod.POST, "/api/v1/appointments", Set.of(ADMIN, RECEPTIONIST, PATIENT)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/appointments/" + MISSING_ID + "/complete", Set.of(DOCTOR)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/appointments/" + MISSING_ID + "/cancel", Set.of(ADMIN, RECEPTIONIST, PATIENT)),
                // Historial clinico
                new Endpoint(HttpMethod.GET, "/api/v1/medicalRecords/patients/me", Set.of(PATIENT)),
                new Endpoint(HttpMethod.GET, "/api/v1/medicalRecords/patients/" + MISSING_ID, Set.of(ADMIN, DOCTOR)),
                new Endpoint(HttpMethod.POST, "/api/v1/medicalRecords", Set.of(DOCTOR)),
                // Hospitalizaciones
                new Endpoint(HttpMethod.GET, "/api/v1/hospitalizations/" + MISSING_ID, STAFF),
                new Endpoint(HttpMethod.POST, "/api/v1/hospitalizations", STAFF),
                new Endpoint(HttpMethod.PATCH, "/api/v1/hospitalizations/" + MISSING_ID + "/discharge", STAFF),
                // Salas
                new Endpoint(HttpMethod.GET, "/api/v1/rooms", STAFF),
                new Endpoint(HttpMethod.GET, "/api/v1/rooms/" + MISSING_ID, STAFF),
                new Endpoint(HttpMethod.POST, "/api/v1/rooms", Set.of(ADMIN)),
                // Inventario
                new Endpoint(HttpMethod.GET, "/api/v1/medicalSupplies", Set.of(ADMIN)),
                new Endpoint(HttpMethod.POST, "/api/v1/medicalSupplies/create", Set.of(ADMIN)),
                new Endpoint(HttpMethod.GET, "/api/v1/supplyMovements", Set.of(ADMIN)),
                new Endpoint(HttpMethod.POST, "/api/v1/supplyMovements/entry", Set.of(ADMIN)),
                new Endpoint(HttpMethod.POST, "/api/v1/supplyMovements/exit", Set.of(ADMIN)),
                new Endpoint(HttpMethod.GET, "/api/supply-usage/hospitalization/" + MISSING_ID, Set.of(ADMIN, DOCTOR)),
                new Endpoint(HttpMethod.POST, "/api/supply-usage", Set.of(ADMIN, DOCTOR)),
                // Archivos y PDF
                new Endpoint(HttpMethod.GET, "/api/v1/fileManager/pdf/" + MISSING_ID + "/generate", Set.of(ADMIN, DOCTOR)),
                new Endpoint(HttpMethod.POST, "/api/v1/fileManager/upload", STAFF),
                new Endpoint(HttpMethod.GET, "/api/v1/fileManager/files", STAFF),
                new Endpoint(HttpMethod.GET, "/api/v1/fileManager/files/00000000-0000-0000-0000-000000000000", STAFF)
        );
    }

    static Stream<Arguments> endpointsByRole() {
        return endpoints().stream()
                .flatMap(endpoint -> Arrays.stream(RoleEnum.values()).map(role -> Arguments.of(endpoint, role)));
    }

    @BeforeAll
    void loginOneUserPerRole() throws Exception {
        for (RoleEnum role : RoleEnum.values()) {
            tokens.put(role, accessToken(createUser(role).getEmail()));
        }
    }

    @ParameterizedTest(name = "{0} como {1}")
    @MethodSource("endpointsByRole")
    void roleAccessMatchesMatrix(Endpoint endpoint, RoleEnum role) throws Exception {
        int status = perform(build(endpoint).header(HttpHeaders.AUTHORIZATION, bearer(tokens.get(role))));

        if (endpoint.allowed().contains(role)) {
            assertThat(status).as("%s debe poder acceder a %s", role, endpoint).isNotIn(401, 403);
        } else {
            assertThat(status).as("%s no debe poder acceder a %s", role, endpoint).isEqualTo(403);
        }
    }

    @ParameterizedTest(name = "{0} sin token")
    @MethodSource("endpoints")
    void anonymousRequestsReturn401(Endpoint endpoint) throws Exception {
        int status = mockMvc.perform(build(endpoint)).andReturn().getResponse().getStatus();

        assertThat(status).as("%s sin token", endpoint).isEqualTo(401);
    }

    /**
     * Si el controlador lanza una excepcion que el GlobalExceptionHandler no puede escribir
     * (por ejemplo el PDF, que fija Content-Type application/pdf antes de buscar al paciente),
     * MockMvc la propaga. Llegar al controlador significa que la seguridad dejo pasar: se cuenta como 500.
     */
    private int perform(MockHttpServletRequestBuilder builder) throws Exception {
        try {
            return mockMvc.perform(builder).andReturn().getResponse().getStatus();
        } catch (ServletException e) {
            return 500;
        }
    }

    private static MockHttpServletRequestBuilder build(Endpoint endpoint) {
        MockHttpServletRequestBuilder builder = request(endpoint.method(), endpoint.path());
        if (endpoint.method() == HttpMethod.POST || endpoint.method() == HttpMethod.PATCH) {
            builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return builder;
    }
}
