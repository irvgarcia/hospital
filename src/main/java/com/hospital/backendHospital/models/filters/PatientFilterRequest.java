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
public class PatientFilterRequest {
    /** Busca en nombre, apellido o email. */
    private String name;
    /** Etiqueta del grupo sanguineo ("O+") o nombre del enum ("O_POSITIVE"). */
    private String bloodType;
    private Boolean status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registerDate;
    private Boolean hospitalized;
}
