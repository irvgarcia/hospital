package com.hospital.backendHospital.models.dto.medicalRecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entrada del historial a partir de una cita COMPLETED del doctor autenticado (M8). */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateMedicalRecordDto {
    @NotNull(message = "La cita es obligatoria")
    private Long appointmentId;

    @NotBlank(message = "El diagnóstico es obligatorio")
    @Size(max = 1000, message = "El diagnóstico no puede tener más de 1000 caracteres")
    private String diagnostics;

    @NotBlank(message = "El tratamiento es obligatorio")
    @Size(max = 1000, message = "El tratamiento no puede tener más de 1000 caracteres")
    private String treatment;

    @Size(max = 1000, message = "Las notas no pueden tener más de 1000 caracteres")
    private String notes;
}
