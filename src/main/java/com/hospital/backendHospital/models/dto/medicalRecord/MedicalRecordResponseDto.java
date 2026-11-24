package com.hospital.backendHospital.models.dto.medicalRecord;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MedicalRecordResponseDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String specialty;
    private Long appointmentId;
    private LocalDate date;
    private String diagnostics;
    private String treatment;
    private String notes;
}
