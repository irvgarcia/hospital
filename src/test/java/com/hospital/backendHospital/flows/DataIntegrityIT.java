package com.hospital.backendHospital.flows;

import com.fasterxml.jackson.databind.JsonNode;
import com.hospital.backendHospital.AbstractClinicalIntegrationTest;
import com.hospital.backendHospital.models.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fase 3: esquema con Flyway (M12), enums como texto (A1) y protecciones de concurrencia (A8). */
class DataIntegrityIT extends AbstractClinicalIntegrationTest {

    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime NINE_THIRTY = LocalTime.of(9, 30);

    @Autowired private JdbcTemplate jdbcTemplate;

    // ------------------------------------------------------------------ M12: Flyway

    @Test
    void schemaIsCreatedByFlywayWithoutDevData() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank", String.class);
        Integer demoUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email LIKE '%@demo.local'", Integer.class);

        assertThat(versions).startsWith("1", "2", "3").doesNotContain("3.1");
        assertThat(demoUsers).isZero();
    }

    // ------------------------------------------------------------------ A1: enums como texto

    @Test
    void enumsAreStoredByName() {
        Doctor doctor = createDoctor();
        Appointment appointment = createAppointment(doctor, createPatient(), LocalDate.now().plusDays(3),
                NINE, NINE_THIRTY, AppointmentStatus.COMPLETED);

        String storedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM appointments WHERE id = ?", String.class, appointment.getId());
        List<String> storedDays = jdbcTemplate.queryForList(
                "SELECT day_of_week FROM doctor_schedules WHERE doctor_id = ?", String.class, doctor.getId());

        assertThat(storedStatus).isEqualTo("COMPLETED");
        assertThat(storedDays).contains("MONDAY", "SUNDAY");
    }

    @Test
    void appointmentStatusFilterWorks() throws Exception {
        Doctor doctor = createDoctor();
        LocalDate date = LocalDate.now().plusDays(5);
        Appointment pending = createAppointment(doctor, createPatient(), date, NINE, NINE_THIRTY, AppointmentStatus.PENDING);
        Appointment completed = createAppointment(doctor, createPatient(), date, LocalTime.of(10, 0),
                LocalTime.of(10, 30), AppointmentStatus.COMPLETED);
        Appointment cancelled = createAppointment(doctor, createPatient(), date, LocalTime.of(11, 0),
                LocalTime.of(11, 30), AppointmentStatus.CANCELLED);
        String adminToken = accessToken(createUser(RoleEnum.ADMIN).getEmail());

        assertThat(appointmentIds(adminToken, doctor, "PENDING")).containsExactly(pending.getId());
        assertThat(appointmentIds(adminToken, doctor, "COMPLETED")).containsExactly(completed.getId());
        assertThat(appointmentIds(adminToken, doctor, "CANCELLED")).containsExactly(cancelled.getId());
    }

    private List<Long> appointmentIds(String token, Doctor doctor, String status) throws Exception {
        String json = mockMvc.perform(get("/api/v1/appointments")
                        .param("doctorId", doctor.getId().toString())
                        .param("status", status)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode page = objectMapper.readTree(json);
        List<Long> ids = new ArrayList<>();
        page.get("content").forEach(item -> ids.add(item.get("id").asLong()));
        return ids;
    }

    // ------------------------------------------------------------------ A8: indice unico de citas

    @Test
    void doctorCannotHaveTwoActiveAppointmentsStartingAtTheSameTime() {
        Doctor doctor = createDoctor();
        LocalDate date = LocalDate.now().plusDays(7);
        createAppointment(doctor, createPatient(), date, NINE, NINE_THIRTY, AppointmentStatus.PENDING);
        Patient other = createPatient();

        assertThatThrownBy(() -> createAppointment(doctor, other, date, NINE, NINE_THIRTY, AppointmentStatus.PENDING))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cancelledAppointmentsDoNotBlockTheSlot() {
        Doctor doctor = createDoctor();
        LocalDate date = LocalDate.now().plusDays(7);
        createAppointment(doctor, createPatient(), date, NINE, NINE_THIRTY, AppointmentStatus.CANCELLED);
        createAppointment(doctor, createPatient(), date, NINE, NINE_THIRTY, AppointmentStatus.CANCELLED);

        Appointment rebooked = createAppointment(doctor, createPatient(), date, NINE, NINE_THIRTY, AppointmentStatus.PENDING);

        assertThat(rebooked.getId()).isNotNull();
    }

    // ------------------------------------------------------------------ A8: bloqueo optimista en insumos

    @Test
    void concurrentStockUpdateIsRejectedInsteadOfLost() {
        Long id = createSupply(unique("Insumo")).getId();
        // Dos "peticiones" leen el mismo insumo (quantity = 100, version = 0) antes de que cualquiera guarde
        MedicalSupply first = medicalSupplyRepository.findById(id).orElseThrow();
        MedicalSupply second = medicalSupplyRepository.findById(id).orElseThrow();

        first.setQuantity(first.getQuantity() - 30);
        medicalSupplyRepository.save(first);

        second.setQuantity(second.getQuantity() - 50);
        assertThatThrownBy(() -> medicalSupplyRepository.save(second))
                .isInstanceOf(OptimisticLockingFailureException.class);

        MedicalSupply stored = medicalSupplyRepository.findById(id).orElseThrow();
        assertThat(stored.getQuantity()).isEqualTo(70);
        assertThat(stored.getVersion()).isEqualTo(1L);
    }
}
