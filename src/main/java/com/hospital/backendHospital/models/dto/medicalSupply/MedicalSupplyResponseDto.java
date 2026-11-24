package com.hospital.backendHospital.models.dto.medicalSupply;

import com.hospital.backendHospital.models.entity.Unit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MedicalSupplyResponseDto {
    private Long id;
    private String name;
    private int quantity;
    private int minimumStock;
    private Unit unit;
    private boolean active;
    /** M11: el stock minimo es un umbral de alerta. true cuando quantity <= minimumStock. */
    private boolean lowStock;
}
