package com.hospital.backendHospital.models.dto.medicalSupply;

import com.hospital.backendHospital.models.entity.Unit;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** La cantidad no se edita aqui: cambia solo con movimientos de entrada/salida o consumos. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateMedicalSupplyDto {
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    private String name;

    @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
    private Integer minimumStock;

    private Unit unit;

    private Boolean active;
}
