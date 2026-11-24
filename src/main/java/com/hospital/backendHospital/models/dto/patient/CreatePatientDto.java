package com.hospital.backendHospital.models.dto.patient;

import com.hospital.backendHospital.models.entity.BloodType;
import com.hospital.backendHospital.models.entity.Gender;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Perfil clinico del paciente (M1: enums para sexo y grupo sanguineo, medidas positivas, fecha pasada). */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatePatientDto {
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
    private String phoneNumber;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255, message = "La dirección no puede tener más de 255 caracteres")
    private String address;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe estar en el pasado")
    private LocalDate dateOfBirth;

    @NotNull(message = "El sexo es obligatorio")
    private Gender gender;

    @NotBlank(message = "Las alergias son obligatorias (escribe Ninguna si no tiene)")
    @Size(max = 255, message = "Las alergias no pueden tener más de 255 caracteres")
    private String allergies;

    @NotNull(message = "El grupo sanguíneo es obligatorio")
    private BloodType bloodType;

    @NotBlank(message = "Las enfermedades crónicas son obligatorias (escribe Ninguna si no tiene)")
    @Size(max = 255, message = "Las enfermedades crónicas no pueden tener más de 255 caracteres")
    private String chronicDiseases;

    @NotBlank(message = "Los medicamentos actuales son obligatorios (escribe Ninguno si no toma)")
    @Size(max = 255, message = "Los medicamentos no pueden tener más de 255 caracteres")
    private String currentMedications;

    @NotBlank(message = "El teléfono de emergencia es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono de emergencia debe tener 10 dígitos")
    private String emergencyContact;

    @NotBlank(message = "El nombre del contacto de emergencia es obligatorio")
    @Size(max = 100, message = "El nombre del contacto no puede tener más de 100 caracteres")
    private String emergencyContactName;

    @NotBlank(message = "La relación con el contacto de emergencia es obligatoria")
    @Size(max = 50, message = "La relación no puede tener más de 50 caracteres")
    private String emergencyContactRelation;

    @NotNull(message = "La estatura es obligatoria")
    @Positive(message = "La estatura debe ser positiva")
    @Max(value = 3, message = "La estatura se indica en metros")
    private Double height;

    @NotNull(message = "El peso es obligatorio")
    @Positive(message = "El peso debe ser positivo")
    @Max(value = 500, message = "El peso se indica en kilogramos")
    private Double weight;

    @NotNull(message = "Indica si el paciente fuma")
    private Boolean smoker;
}
