package com.hospital.backendHospital.models.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Datos del usuario autenticado. patientId/doctorId solo vienen si el usuario tiene ese perfil. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountResponseDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private Long patientId;
    private Long doctorId;
    /** Un PACIENTE recien registrado no tiene perfil hasta completarlo (POST /api/v1/patients). */
    private boolean profileCompleted;
}
