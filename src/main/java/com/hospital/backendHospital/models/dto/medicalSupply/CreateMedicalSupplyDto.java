package com.hospital.backendHospital.models.dto.medicalSupply;

import com.hospital.backendHospital.models.entity.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateMedicalSupplyDto {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    private String name;

    @NotNull(message = "La cantidad es obligatoria")
    @PositiveOrZero(message = "La cantidad no puede ser negativa")
    private Integer quantity;

    @NotNull(message = "El stock mínimo es obligatorio")
    @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
    private Integer minimumStock;

    @NotNull(message = "La unidad es obligatoria")
    private Unit unit;
}
