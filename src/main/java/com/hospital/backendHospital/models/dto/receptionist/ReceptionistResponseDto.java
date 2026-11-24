package com.hospital.backendHospital.models.dto.receptionist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceptionistResponseDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean active;
}
