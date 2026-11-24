package com.hospital.backendHospital.models.dto.hospitalization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** doctorId es obligatorio para ADMIN y RECEPTIONIST; si hospitaliza un DOCTOR y no lo indica, queda el mismo. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateHospitalizationDto {
    @NotNull(message = "El paciente es obligatorio")
    private Long patientId;

    private Long doctorId;

    @NotNull(message = "La sala es obligatoria")
    private Long roomId;

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 255, message = "El motivo no puede tener más de 255 caracteres")
    private String reason;

    @NotNull(message = "El costo por día es obligatorio")
    @Positive(message = "El costo por día debe ser positivo")
    private BigDecimal costPerDay;
}
