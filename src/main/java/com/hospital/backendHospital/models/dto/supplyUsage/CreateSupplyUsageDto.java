package com.hospital.backendHospital.models.dto.supplyUsage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateSupplyUsageDto {
    @NotNull(message = "La hospitalización es obligatoria")
    private Long hospitalizationId;

    @NotNull(message = "El insumo es obligatorio")
    private Long supplyId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser positiva")
    private Integer quantityUsed;
}
