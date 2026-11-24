package com.hospital.backendHospital.security;

import com.hospital.backendHospital.AbstractIntegrationTest;
import com.hospital.backendHospital.models.entity.RoleEnum;
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
 * Matriz de acceso por rol (C9, A12) sobre todos los endpoints de la API.
 * - Rol permitido: la seguridad deja pasar (la respuesta no es 401 ni 403; puede ser 2xx, 204, 400 o 404 segun los datos).
 * - Rol no permitido: 403.
 * - Sin token: 401.
 * Al agregar un endpoint hay que agregarlo aqui y en SecurityConfig.
 * Los endpoints cuya propiedad se valida en el servicio (archivos, cita por id) se prueban con datos en los flujos.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleAccessIT extends AbstractIntegrationTest {

    private static final Set<RoleEnum> ALL = Set.of(ADMIN, RECEPTIONIST, DOCTOR, PATIENT);
    private static final Set<RoleEnum> STAFF = Set.of(ADMIN, RECEPTIONIST, DOCTOR);
    private static final String ID = "999999";
    private static final String FILE = "/api/v1/files/00000000-0000-0000-0000-000000000000";

    private final Map<RoleEnum, String> tokens = new EnumMap<>(RoleEnum.class);

    record Endpoint(HttpMethod method, String path, Set<RoleEnum> allowed) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    static List<Endpoint> endpoints() {
        return List.of(
                // Cuenta y panel
                new Endpoint(HttpMethod.GET, "/api/v1/account", ALL),
                new Endpoint(HttpMethod.PATCH, "/api/v1/account/password", ALL),
                new Endpoint(HttpMethod.GET, "/api/v1/dashboard", Set.of(ADMIN, RECEPTIONIST)),
                // Recepcionistas
                new Endpoint(HttpMethod.GET, "/api/v1/receptionists", Set.of(ADMIN)),
                new Endpoint(HttpMethod.GET, "/api/v1/receptionists/" + ID, Set.of(ADMIN)),
                new Endpoint(HttpMethod.POST, "/api/v1/receptionists", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/receptionists/" + ID, Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/receptionists/" + ID + "/deactivate", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/receptionists/" + ID + "/activate", Set.of(ADMIN)),
                // Pacientes
                new Endpoint(HttpMethod.GET, "/api/v1/patients", STAFF),
                new Endpoint(HttpMethod.GET, "/api/v1/patients/" + ID, STAFF),
                new Endpoint(HttpMethod.POST, "/api/v1/patients", Set.of(PATIENT)),
                new Endpoint(HttpMethod.POST, "/api/v1/patients/register", Set.of(ADMIN, RECEPTIONIST)),
                new Endpoint(HttpMethod.GET, "/api/v1/patients/me", Set.of(PATIENT)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/patients/me", Set.of(PATIENT)),
                new Endpoint(HttpMethod.GET, "/api/v1/patients/me/medical-record/pdf", Set.of(PATIENT)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/patients/" + ID, Set.of(ADMIN, RECEPTIONIST)),
                new Endpoint(HttpMethod.GET, "/api/v1/patients/" + ID + "/medical-record/pdf", Set.of(ADMIN, DOCTOR)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/patients/" + ID + "/deactivate", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/patients/" + ID + "/activate", Set.of(ADMIN)),
                // Doctores
                new Endpoint(HttpMethod.GET, "/api/v1/doctors", ALL),
                new Endpoint(HttpMethod.GET, "/api/v1/doctors/me", Set.of(DOCTOR)),
                new Endpoint(HttpMethod.GET, "/api/v1/doctors/" + ID, ALL),
                new Endpoint(HttpMethod.GET, "/api/v1/doctors/" + ID + "/availability?date=2030-01-07", ALL),
                new Endpoint(HttpMethod.POST, "/api/v1/doctors", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/doctors/" + ID, Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/doctors/" + ID + "/deactivate", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/doctors/" + ID + "/activate", Set.of(ADMIN)),
                // Especialidades
                new Endpoint(HttpMethod.GET, "/api/v1/specialties", ALL),
                new Endpoint(HttpMethod.GET, "/api/v1/specialties/" + ID, ALL),
                new Endpoint(HttpMethod.POST, "/api/v1/specialties", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/specialties/" + ID, Set.of(ADMIN)),
                // Horarios
                new Endpoint(HttpMethod.GET, "/api/v1/doctor-schedules", ALL),
                new Endpoint(HttpMethod.POST, "/api/v1/doctor-schedules", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/doctor-schedules/" + ID, Set.of(ADMIN)),
                new Endpoint(HttpMethod.DELETE, "/api/v1/doctor-schedules/" + ID, Set.of(ADMIN)),
                // Citas
                new Endpoint(HttpMethod.GET, "/api/v1/appointments", STAFF),
                new Endpoint(HttpMethod.GET, "/api/v1/appointments/me", Set.of(PATIENT, DOCTOR)),
                new Endpoint(HttpMethod.GET, "/api/v1/appointments/me/next", Set.of(PATIENT)),
                new Endpoint(HttpMethod.GET, "/api/v1/appointments/" + ID, ALL),
                new Endpoint(HttpMethod.POST, "/api/v1/appointments", Set.of(ADMIN, RECEPTIONIST, PATIENT)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/appointments/" + ID + "/reschedule", Set.of(ADMIN, RECEPTIONIST, PATIENT)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/appointments/" + ID + "/complete", Set.of(DOCTOR)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/appointments/" + ID + "/cancel", Set.of(ADMIN, RECEPTIONIST, PATIENT)),
                // Historial clinico
                new Endpoint(HttpMethod.GET, "/api/v1/medical-records/me", Set.of(PATIENT)),
                new Endpoint(HttpMethod.GET, "/api/v1/medical-records/patients/" + ID, Set.of(ADMIN, DOCTOR)),
                new Endpoint(HttpMethod.POST, "/api/v1/medical-records", Set.of(DOCTOR)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/medical-records/" + ID, Set.of(DOCTOR)),
                // Hospitalizaciones y consumos
                new Endpoint(HttpMethod.GET, "/api/v1/hospitalizations", STAFF),
                new Endpoint(HttpMethod.GET, "/api/v1/hospitalizations/me", Set.of(PATIENT)),
                new Endpoint(HttpMethod.GET, "/api/v1/hospitalizations/" + ID, STAFF),
                new Endpoint(HttpMethod.GET, "/api/v1/hospitalizations/" + ID + "/supply-usages", STAFF),
                new Endpoint(HttpMethod.POST, "/api/v1/hospitalizations", STAFF),
                new Endpoint(HttpMethod.PATCH, "/api/v1/hospitalizations/" + ID + "/discharge", STAFF),
                new Endpoint(HttpMethod.POST, "/api/v1/supply-usages", Set.of(ADMIN, DOCTOR)),
                // Salas
                new Endpoint(HttpMethod.GET, "/api/v1/rooms", STAFF),
                new Endpoint(HttpMethod.GET, "/api/v1/rooms/" + ID, STAFF),
                new Endpoint(HttpMethod.POST, "/api/v1/rooms", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/rooms/" + ID, Set.of(ADMIN)),
                // Inventario
                new Endpoint(HttpMethod.GET, "/api/v1/medical-supplies", Set.of(ADMIN, DOCTOR)),
                new Endpoint(HttpMethod.GET, "/api/v1/medical-supplies/low-stock", Set.of(ADMIN)),
                new Endpoint(HttpMethod.GET, "/api/v1/medical-supplies/" + ID, Set.of(ADMIN, DOCTOR)),
                new Endpoint(HttpMethod.POST, "/api/v1/medical-supplies", Set.of(ADMIN)),
                new Endpoint(HttpMethod.PATCH, "/api/v1/medical-supplies/" + ID, Set.of(ADMIN)),
                new Endpoint(HttpMethod.GET, "/api/v1/supply-movements", Set.of(ADMIN)),
                new Endpoint(HttpMethod.POST, "/api/v1/supply-movements", Set.of(ADMIN)),
                // Archivos
                new Endpoint(HttpMethod.GET, "/api/v1/files", ALL),
                new Endpoint(HttpMethod.POST, "/api/v1/files", ALL),
                new Endpoint(HttpMethod.GET, FILE, ALL),
                new Endpoint(HttpMethod.DELETE, FILE, ALL)
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
        int status = mockMvc.perform(build(endpoint).header(HttpHeaders.AUTHORIZATION, bearer(tokens.get(role))))
                .andReturn().getResponse().getStatus();

        if (endpoint.allowed().contains(role)) {
            assertThat(status).as("%s debe poder acceder a %s", role, endpoint).isNotIn(401, 403, 500);
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

    private static MockHttpServletRequestBuilder build(Endpoint endpoint) {
        MockHttpServletRequestBuilder builder = request(endpoint.method(), endpoint.path());
        if (endpoint.path().equals("/api/v1/files") && endpoint.method() == HttpMethod.POST) {
            return builder.contentType(MediaType.MULTIPART_FORM_DATA);
        }
        if (endpoint.method() == HttpMethod.POST || endpoint.method() == HttpMethod.PATCH) {
            builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return builder;
    }
}
