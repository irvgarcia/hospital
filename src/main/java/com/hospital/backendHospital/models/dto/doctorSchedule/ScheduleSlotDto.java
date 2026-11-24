package com.hospital.backendHospital.models.dto.doctorSchedule;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Turno de un dia, usado al crear o actualizar un doctor con sus horarios. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScheduleSlotDto {
    @NotNull(message = "El día es obligatorio")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime endTime;
}
