package com.hospital.backendHospital.flows;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospital.backendHospital.AbstractClinicalIntegrationTest;
import com.hospital.backendHospital.models.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Checklist final (seccion 7): el flujo completo a traves de la API, con los roles reales.
 * Registrar paciente -> completar perfil -> listar doctores -> agendar -> el doctor completa -> historial
 * -> hospitalizar -> consumir insumos -> alta -> PDF del expediente; y los archivos del paciente.
 */
class EndToEndFlowIT extends AbstractClinicalIntegrationTest {

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String token, Object body) throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(request);
    }

    private static Map<String, Object> profile() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("phoneNumber", "5511112222");
        profile.put("address", "Av. Siempre Viva 742");
        profile.put("dateOfBirth", "1990-04-12");
        profile.put("gender", "FEMALE");
        profile.put("allergies", "Ninguna");
        profile.put("bloodType", "A+");
        profile.put("chronicDiseases", "Ninguna");
        profile.put("currentMedications", "Ninguno");
        profile.put("emergencyContact", "5533334444");
        profile.put("emergencyContactName", "Mario");
        profile.put("emergencyContactRelation", "Hermano");
        profile.put("height", 1.6);
        profile.put("weight", 58.0);
        profile.put("smoker", false);
        return profile;
    }

    @Test
    void fullClinicalFlow() throws Exception {
        // 1. Registro publico y perfil
        String email = unique("paciente") + "@test.com";
        JsonNode tokens = json(mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "firstName", "Ana", "lastName", "Gil",
                        "password", PASSWORD)))).andExpect(status().isCreated()));
        String patientToken = tokens.get("accessToken").asText();

        call(get("/api/v1/account"), patientToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleted").value(false));
        call(post("/api/v1/patients"), patientToken, profile())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloodType").value("A+"))
                .andExpect(jsonPath("$.fullName").value("Ana Gil"));
        // A3: el perfil solo se crea una vez
        call(post("/api/v1/patients"), patientToken, profile()).andExpect(status().isConflict());

        // 2. Catalogo de doctores y horarios libres
        Doctor doctor = createDoctor();
        String doctorToken = accessToken(doctor.getUser().getEmail());
        LocalDate date = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
        JsonNode doctors = json(call(get("/api/v1/doctors").param("specialtyId", doctor.getSpecialty().getId().toString()), patientToken, null)
                .andExpect(status().isOk()));
        assertThat(doctors.get("content").get(0).get("id").asLong()).isEqualTo(doctor.getId());
        JsonNode slots = json(call(get("/api/v1/doctors/" + doctor.getId() + "/availability").param("date", date.toString()), patientToken, null)
                .andExpect(status().isOk()));
        assertThat(slots).isNotEmpty();

        // 3. Agendar con el primer horario libre
        JsonNode appointment = json(call(post("/api/v1/appointments"), patientToken, Map.of(
                "doctorId", doctor.getId(), "date", date.toString(),
                "startTime", slots.get(0).get("startTime").asText(), "endTime", slots.get(0).get("endTime").asText(),
                "reason", "Dolor de cabeza")).andExpect(status().isCreated()));
        long appointmentId = appointment.get("id").asLong();
        call(get("/api/v1/appointments/me/next"), patientToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId));
        JsonNode after = json(call(get("/api/v1/doctors/" + doctor.getId() + "/availability").param("date", date.toString()), patientToken, null));
        assertThat(after.size()).isEqualTo(slots.size() - 1);

        // 4. El doctor completa la cita (se mueve a hoy para poder completarla) y registra el historial
        Appointment stored = appointmentRepository.findById(appointmentId).orElseThrow();
        stored.setDate(LocalDate.now());
        appointmentRepository.save(stored);
        call(patch("/api/v1/appointments/" + appointmentId + "/complete"), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        JsonNode record = json(call(post("/api/v1/medical-records"), doctorToken, Map.of(
                "appointmentId", appointmentId, "diagnostics", "Migrana", "treatment", "Reposo e ibuprofeno"))
                .andExpect(status().isCreated()));
        call(post("/api/v1/medical-records"), doctorToken, Map.of(
                "appointmentId", appointmentId, "diagnostics", "Otra", "treatment", "Otra"))
                .andExpect(status().isConflict());
        call(get("/api/v1/medical-records/me"), patientToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(record.get("id").asLong()))
                .andExpect(jsonPath("$.content[0].doctorName").value(doctor.getUser().getFirstName() + " " + doctor.getUser().getLastName()));

        // 5. Hospitalizar, consumir insumos y dar de alta
        long patientId = json(call(get("/api/v1/patients/me"), patientToken, null)).get("id").asLong();
        Room room = createRoom();
        String receptionistToken = accessToken(createUser(RoleEnum.RECEPTIONIST).getEmail());
        JsonNode hospitalization = json(call(post("/api/v1/hospitalizations"), receptionistToken, Map.of(
                "patientId", patientId, "doctorId", doctor.getId(), "roomId", room.getId(),
                "reason", "Observacion", "costPerDay", 800)).andExpect(status().isCreated()));
        long hospitalizationId = hospitalization.get("id").asLong();

        MedicalSupply supply = createSupply(unique("Gasas"));
        call(post("/api/v1/supply-usages"), doctorToken, Map.of(
                "hospitalizationId", hospitalizationId, "supplyId", supply.getId(), "quantityUsed", 5))
                .andExpect(status().isCreated());
        call(post("/api/v1/supply-usages"), doctorToken, Map.of(
                "hospitalizationId", hospitalizationId, "supplyId", supply.getId(), "quantityUsed", -5))
                .andExpect(status().isBadRequest());
        call(get("/api/v1/hospitalizations/" + hospitalizationId + "/supply-usages"), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        assertThat(medicalSupplyRepository.findById(supply.getId()).orElseThrow().getQuantity()).isEqualTo(95);

        call(patch("/api/v1/hospitalizations/" + hospitalizationId + "/discharge"), receptionistToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.totalCost").value(800.0));
        call(patch("/api/v1/hospitalizations/" + hospitalizationId + "/discharge"), receptionistToken, null)
                .andExpect(status().isBadRequest());

        // 6. PDF del expediente: el paciente el suyo, el doctor el de cualquiera
        byte[] pdf = call(get("/api/v1/patients/me/medical-record/pdf"), patientToken, null)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        call(get("/api/v1/patients/" + patientId + "/medical-record/pdf"), doctorToken, null).andExpect(status().isOk());
        call(get("/api/v1/patients/999999/medical-record/pdf"), doctorToken, null)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void filesBelongToPatientsAndRespectOwnership() throws Exception {
        Patient patient = createPatient();
        Patient other = createPatient();
        Doctor doctor = createDoctor();
        String patientToken = accessToken(patient.getUser().getEmail());
        String otherToken = accessToken(other.getUser().getEmail());
        String doctorToken = accessToken(doctor.getUser().getEmail());

        MockMultipartFile file = new MockMultipartFile("file", "estudio.pdf", "application/pdf", "%PDF-1.4 prueba".getBytes());
        JsonNode uploaded = json(mockMvc.perform(multipart("/api/v1/files").file(file)
                .header(HttpHeaders.AUTHORIZATION, bearer(patientToken))).andExpect(status().isCreated()));
        String url = uploaded.get("url").asText();
        assertThat(url).startsWith("/api/v1/files/");

        call(get(url), patientToken, null).andExpect(status().isOk());
        call(get(url), otherToken, null).andExpect(status().isForbidden());
        // El doctor solo accede cuando atiende al paciente
        call(get(url), doctorToken, null).andExpect(status().isForbidden());
        createAppointment(doctor, patient, LocalDate.now().plusDays(2), LocalTime.of(9, 0), LocalTime.of(9, 30), AppointmentStatus.PENDING);
        call(get(url), doctorToken, null).andExpect(status().isOk());
        call(get("/api/v1/files").param("patientId", patient.getId().toString()), doctorToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Tipo no permitido
        MockMultipartFile exe = new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[]{1, 2});
        mockMvc.perform(multipart("/api/v1/files").file(exe).header(HttpHeaders.AUTHORIZATION, bearer(patientToken)))
                .andExpect(status().isBadRequest());

        List<JsonNode> mine = List.of(json(call(get("/api/v1/files"), otherToken, null)));
        assertThat(mine.get(0)).isEmpty();
    }
}
