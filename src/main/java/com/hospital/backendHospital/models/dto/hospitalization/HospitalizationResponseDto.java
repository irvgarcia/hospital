package com.hospital.backendHospital.models.dto.hospitalization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HospitalizationResponseDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private Long roomId;
    private String roomName;
    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    private String reason;
    private BigDecimal costPerDay;
    private BigDecimal totalCost;
    private boolean active;
}
