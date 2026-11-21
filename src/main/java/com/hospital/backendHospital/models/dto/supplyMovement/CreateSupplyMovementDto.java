package com.hospital.backendHospital.models.dto.supplyMovement;

import com.hospital.backendHospital.models.entity.MovementType;
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
public class CreateSupplyMovementDto {
    @NotNull(message = "El insumo es obligatorio")
    private Long supplyId;

    @NotNull(message = "El tipo de movimiento es obligatorio (IN u OUT)")
    private MovementType type;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser positiva")
    private Integer quantity;
}
