package com.hospital.backendHospital.models.dto.medicalRecord;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateMedicalRecordDto {
    @Size(min = 1, max = 1000, message = "El diagnóstico debe tener entre 1 y 1000 caracteres")
    private String diagnostics;

    @Size(min = 1, max = 1000, message = "El tratamiento debe tener entre 1 y 1000 caracteres")
    private String treatment;

    @Size(max = 1000, message = "Las notas no pueden tener más de 1000 caracteres")
    private String notes;
}
