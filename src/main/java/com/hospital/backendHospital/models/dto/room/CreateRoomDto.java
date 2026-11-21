package com.hospital.backendHospital.models.dto.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateRoomDto {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    private String name;

    @NotNull(message = "La capacidad es obligatoria")
    @Positive(message = "La capacidad debe ser positiva")
    @Max(value = 50, message = "La capacidad no puede ser mayor a 50 camas")
    private Integer capacity;
}
