package com.hospital.backendHospital.models.dto.patient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Registro en un solo paso (A3) que hace recepcion o un admin: usuario y perfil en la misma transaccion. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterPatientDto {
    @Email(message = "El email no es válido")
    @NotBlank(message = "El email es obligatorio")
    @Size(max = 100, message = "El email no puede tener más de 100 caracteres")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String lastName;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    private String password;

    @Valid
    @NotNull(message = "El perfil del paciente es obligatorio")
    private CreatePatientDto profile;
}
