package com.hospital.backendHospital.models.dto.patient;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Todos los campos son opcionales: solo se cambia lo que viene. La contraseña se cambia en /account/password. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdatePatientDto {
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String firstName;

    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String lastName;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
    private String phoneNumber;

    @Size(min = 1, max = 255, message = "La dirección debe tener entre 1 y 255 caracteres")
    private String address;

    @Size(min = 1, max = 255, message = "Las alergias deben tener entre 1 y 255 caracteres")
    private String allergies;

    @Size(min = 1, max = 255, message = "Las enfermedades crónicas deben tener entre 1 y 255 caracteres")
    private String chronicDiseases;

    @Size(min = 1, max = 255, message = "Los medicamentos deben tener entre 1 y 255 caracteres")
    private String currentMedications;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono de emergencia debe tener 10 dígitos")
    private String emergencyContact;

    @Size(min = 1, max = 100, message = "El nombre del contacto debe tener entre 1 y 100 caracteres")
    private String emergencyContactName;

    @Size(min = 1, max = 50, message = "La relación debe tener entre 1 y 50 caracteres")
    private String emergencyContactRelation;

    @Positive(message = "La estatura debe ser positiva")
    @Max(value = 3, message = "La estatura se indica en metros")
    private Double height;

    @Positive(message = "El peso debe ser positivo")
    @Max(value = 500, message = "El peso se indica en kilogramos")
    private Double weight;

    private Boolean smoker;
}
