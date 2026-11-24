package com.hospital.backendHospital.models.dto.specialty;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Todos los campos son opcionales. active=false desactiva la especialidad (ya no se puede agendar con ella). */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateSpecialtyDto {
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    private String name;

    @Size(min = 1, max = 255, message = "La descripción debe tener entre 1 y 255 caracteres")
    private String description;

    @Positive(message = "El costo debe ser positivo")
    private BigDecimal cost;

    private Boolean active;
}
