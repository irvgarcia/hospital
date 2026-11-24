package com.hospital.backendHospital.models.filters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DoctorFilterRequest {
    /** Busca en nombre, apellido o email. */
    private String name;
    private String firstName;
    private String lastName;
    private Boolean status;
    private Long specialtyId;
    /** Nombre exacto de la especialidad (sin distinguir mayusculas). */
    private String specialty;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registerDate;
}
