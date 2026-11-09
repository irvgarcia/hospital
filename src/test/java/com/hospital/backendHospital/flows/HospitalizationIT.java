package com.hospital.backendHospital.flows;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospital.backendHospital.AbstractClinicalIntegrationTest;
import com.hospital.backendHospital.models.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** C1 (crear hospitalizacion con doctor) y C2 (filtros de hospitalizacion). */
class HospitalizationIT extends AbstractClinicalIntegrationTest {

    private Map<String, Object> body(Patient patient, Doctor doctor, Room room) {
        Map<String, Object> body = new HashMap<>();
        body.put("patientId", patient.getId());
        if (doctor != null) {
            body.put("doctorId", doctor.getId());
        }
        body.put("roomId", room.getId());
        body.put("reason", "Observacion postoperatoria");
        body.put("costPerDay", 300);
        return body;
    }

    private Long doctorIdOfHospitalizationOf(Patient patient) {
        return inTransaction(() -> hospitalizationRepository.findAll().stream()
                .filter(h -> h.getPatient().getId().equals(patient.getId()))
                .findFirst().orElseThrow()
                .getDoctor().getId());
    }

    @Test
    void receptionistCreatesHospitalizationWithTheIndicatedDoctor() throws Exception {
        Patient patient = createPatient();
        Doctor doctor = createDoctor();
        String token = accessToken(createUser(RoleEnum.RECEPTIONIST).getEmail());

        mockMvc.perform(post("/api/v1/hospitalizations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(patient, doctor, createRoom()))))
                .andExpect(status().isCreated());

        assertThat(doctorIdOfHospitalizationOf(patient)).isEqualTo(doctor.getId());
    }

    @Test
    void doctorWithoutDoctorIdIsAssignedAsResponsible() throws Exception {
        Patient patient = createPatient();
        Doctor doctor = createDoctor();
        String token = accessToken(doctor.getUser().getEmail());

        mockMvc.perform(post("/api/v1/hospitalizations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(patient, null, createRoom()))))
                .andExpect(status().isCreated());

        assertThat(doctorIdOfHospitalizationOf(patient)).isEqualTo(doctor.getId());
    }

    @Test
    void receptionistWithoutDoctorIdGets400() throws Exception {
        String token = accessToken(createUser(RoleEnum.RECEPTIONIST).getEmail());

        mockMvc.perform(post("/api/v1/hospitalizations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(createPatient(), null, createRoom()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("Doctor is required"));
    }

    @Test
    void inactiveDoctorCannotBeResponsible() throws Exception {
        Doctor doctor = createDoctor();
        doctor.setActive(false);
        doctorRepository.save(doctor);
        String token = accessToken(createUser(RoleEnum.ADMIN).getEmail());

        mockMvc.perform(post("/api/v1/hospitalizations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(createPatient(), doctor, createRoom()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("Doctor is inactive"));
    }

    @Test
    void filtersAreAppliedInsteadOfReturningEverything() throws Exception {
        Doctor doctor = createDoctor();
        Patient target = createPatient();
        createHospitalization(target, doctor, createRoom());
        createHospitalization(createPatient(), doctor, createRoom());
        String token = accessToken(createUser(RoleEnum.ADMIN).getEmail());

        // GET /{id} ejecuta el filtro (el GET real por id llega en la Fase 6)
        JsonNode byPatient = getPage(token, "patientId", target.getId().toString());
        assertThat(byPatient.get("page").get("totalElements").asLong()).isEqualTo(1);

        JsonNode byDoctor = getPage(token, "doctorId", doctor.getId().toString());
        assertThat(byDoctor.get("page").get("totalElements").asLong()).isEqualTo(2);
    }

    private JsonNode getPage(String token, String param, String value) throws Exception {
        String json = mockMvc.perform(get("/api/v1/hospitalizations/0")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param(param, value)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }
}
