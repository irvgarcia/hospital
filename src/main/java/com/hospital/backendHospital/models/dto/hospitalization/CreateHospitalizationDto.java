package com.hospital.backendHospital.models.dto.hospitalization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateHospitalizationDto {

    @NotNull(message = "Patient is required")
    private Long patientId;

    /** Doctor responsable. Obligatorio para ADMIN y RECEPTIONIST; si lo omite un DOCTOR, queda asignado el mismo. */
    private Long doctorId;

    @NotNull(message = "Room is required")
    private Long roomId;

    @NotBlank(message = "Reason is required")
    private String reason;

    @NotNull(message = "Cost per day is required")
    @Positive(message = "Cost must be positive")
    private BigDecimal costPerDay;
}
