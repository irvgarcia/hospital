package com.hospital.backendHospital.models.filters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MedicalSupplyFilterRequest {
    private String name;
    private Boolean active;
    /** true: solo insumos con cantidad menor o igual al stock minimo. */
    private Boolean lowStock;
}
