package com.hospital.backendHospital.flows;

import com.hospital.backendHospital.AbstractClinicalIntegrationTest;
import com.hospital.backendHospital.models.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** C6 (solapamiento por doctor y por paciente) y C4 (el doctor completa sus citas). */
class AppointmentIT extends AbstractClinicalIntegrationTest {

    private static final LocalDate DATE = LocalDate.now().plusDays(3);

    private ResultActions book(Patient patient, Doctor doctor, String start, String end) throws Exception {
        String token = accessToken(patient.getUser().getEmail());
        return mockMvc.perform(post("/api/v1/appointments")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "doctorId", doctor.getId(),
                        "date", DATE.toString(),
                        "startTime", start,
                        "endTime", end))));
    }

    @Test
    void differentDoctorsCanAttendAtTheSameTime() throws Exception {
        Doctor doctorA = createDoctor();
        Doctor doctorB = createDoctor();

        book(createPatient(), doctorA, "10:00", "10:30").andExpect(status().isCreated());
        book(createPatient(), doctorB, "10:00", "10:30").andExpect(status().isCreated());
    }

    @Test
    void doctorCannotHaveOverlappingAppointments() throws Exception {
        Doctor doctor = createDoctor();
        book(createPatient(), doctor, "11:00", "11:30").andExpect(status().isCreated());

        book(createPatient(), doctor, "11:15", "11:45")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value(org.hamcrest.Matchers.containsString("already has an appointment")));
    }

    @Test
    void patientCannotHaveTwoAppointmentsAtTheSameTime() throws Exception {
        Patient patient = createPatient();
        book(patient, createDoctor(), "12:00", "12:30").andExpect(status().isCreated());

        book(patient, createDoctor(), "12:15", "12:45")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("The patient already has an appointment for that time and day"));
    }

    @Test
    void cancelledAppointmentsDoNotBlockTheSlot() throws Exception {
        Doctor doctor = createDoctor();
        createAppointment(doctor, createPatient(), DATE, LocalTime.of(13, 0), LocalTime.of(13, 30), AppointmentStatus.CANCELLED);

        book(createPatient(), doctor, "13:00", "13:30").andExpect(status().isCreated());
    }

    @Test
    void doctorCompletesOwnAppointment() throws Exception {
        Doctor doctor = createDoctor();
        Appointment appointment = createAppointment(doctor, createPatient(), LocalDate.now(),
                LocalTime.of(9, 0), LocalTime.of(9, 30), AppointmentStatus.PENDING);
        // El bug comparaba doctor.id con user.id: aqui son distintos
        assertThat(doctor.getId()).isNotEqualTo(doctor.getUser().getId());

        mockMvc.perform(patch("/api/v1/appointments/" + appointment.getId() + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(doctor.getUser().getEmail()))))
                .andExpect(status().isNoContent());

        assertThat(appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void doctorCannotCompleteAnotherDoctorsAppointment() throws Exception {
        Appointment appointment = createAppointment(createDoctor(), createPatient(), LocalDate.now(),
                LocalTime.of(9, 0), LocalTime.of(9, 30), AppointmentStatus.PENDING);
        Doctor otherDoctor = createDoctor();

        mockMvc.perform(patch("/api/v1/appointments/" + appointment.getId() + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(otherDoctor.getUser().getEmail()))))
                .andExpect(status().isForbidden());

        assertThat(appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.PENDING);
    }
}
