package com.hospital.backendHospital.models.dto.appointment;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/** patientId solo lo usan ADMIN y RECEPTIONIST; un PACIENTE siempre agenda para si mismo. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateAppointmentDto {
    private Long patientId;

    @NotNull(message = "El doctor es obligatorio")
    private Long doctorId;

    @NotNull(message = "La fecha es obligatoria")
    @FutureOrPresent(message = "La fecha no puede estar en el pasado")
    private LocalDate date;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime endTime;

    @Size(max = 500, message = "El motivo no puede tener más de 500 caracteres")
    private String reason;
}
