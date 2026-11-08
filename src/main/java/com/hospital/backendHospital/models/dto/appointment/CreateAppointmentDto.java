package com.hospital.backendHospital.models.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateAppointmentDto {

    /** Obligatorio cuando agenda ADMIN o RECEPTIONIST. Si agenda un PATIENT se ignora: la cita es suya. */
    private Long patientId;

    @NotNull(message = "Doctor is required")
    private Long doctorId;

    @NotNull(message = "Date is required")
    @Future(message = "The date must be in the future")
    private LocalDate date;

    @NotNull(message = "Star time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;
}
