package com.hospital.backendHospital.flows;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospital.backendHospital.AbstractClinicalIntegrationTest;
import com.hospital.backendHospital.models.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** C2 (busqueda de insumos) y C3 (filtros de estado y de hospitalizados). */
class SearchFiltersIT extends AbstractClinicalIntegrationTest {

    private String adminToken;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        adminToken = accessToken(createUser(RoleEnum.ADMIN).getEmail());
    }

    private JsonNode page(MockHttpServletRequestBuilder request) throws Exception {
        String json = mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(adminToken)).param("size", "1000"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private static long total(JsonNode page) {
        return page.get("page").get("totalElements").asLong();
    }

    private static List<Long> ids(JsonNode page, String field) {
        List<Long> ids = new ArrayList<>();
        page.get("content").forEach(item -> ids.add(item.get(field).asLong()));
        return ids;
    }

    @Test
    void supplySearchIsPartialAndCaseInsensitive() throws Exception {
        String tag = unique("lote").toLowerCase();
        createSupply("Gasas Esteriles " + tag.toUpperCase());
        createSupply("Jeringas 5ml");

        JsonNode result = page(get("/api/v1/medicalSupplies").param("name", "esteriles " + tag));

        assertThat(total(result)).isEqualTo(1);
        assertThat(result.get("content").get(0).get("name").asText()).isEqualTo("Gasas Esteriles " + tag.toUpperCase());
    }

    @Test
    void doctorStatusFilterWorks() throws Exception {
        Doctor inactive = createDoctor();
        inactive.setActive(false);
        doctorRepository.save(inactive);
        String lastName = inactive.getUser().getLastName();

        assertThat(total(page(get("/api/v1/doctors").param("status", "false").param("lastName", lastName)))).isEqualTo(1);
        assertThat(total(page(get("/api/v1/doctors").param("status", "true").param("lastName", lastName)))).isZero();
    }

    @Test
    void patientStatusFilterWorks() throws Exception {
        Patient active = createPatient();
        Patient inactive = createPatient();
        inactive.setActive(false);
        patientRepository.save(inactive);

        List<Long> activeIds = ids(page(get("/api/v1/patients").param("status", "true")), "id");
        List<Long> inactiveIds = ids(page(get("/api/v1/patients").param("status", "false")), "id");

        assertThat(activeIds).contains(active.getId()).doesNotContain(inactive.getId());
        assertThat(inactiveIds).contains(inactive.getId()).doesNotContain(active.getId());
    }

    @Test
    void hospitalizedFilterUsesActiveHospitalizations() throws Exception {
        Doctor doctor = createDoctor();
        Patient hospitalized = createPatient();
        createHospitalization(hospitalized, doctor, createRoom());

        Patient discharged = createPatient();
        Hospitalization past = createHospitalization(discharged, doctor, createRoom());
        past.setActive(false);
        hospitalizationRepository.save(past);

        Patient neverHospitalized = createPatient();

        List<Long> yes = ids(page(get("/api/v1/patients").param("hospitalized", "true")), "id");
        List<Long> no = ids(page(get("/api/v1/patients").param("hospitalized", "false")), "id");

        assertThat(yes).contains(hospitalized.getId()).doesNotContain(discharged.getId(), neverHospitalized.getId());
        assertThat(no).contains(discharged.getId(), neverHospitalized.getId()).doesNotContain(hospitalized.getId());
    }
}
