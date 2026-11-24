package com.hospital.backendHospital.models.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/** Horario libre para agendar con un doctor en una fecha. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AvailableSlotDto {
    private LocalTime startTime;
    private LocalTime endTime;
}
