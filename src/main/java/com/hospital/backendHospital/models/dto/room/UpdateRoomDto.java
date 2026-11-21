package com.hospital.backendHospital.models.dto.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateRoomDto {
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    private String name;

    @Positive(message = "La capacidad debe ser positiva")
    @Max(value = 50, message = "La capacidad no puede ser mayor a 50 camas")
    private Integer capacity;
}
