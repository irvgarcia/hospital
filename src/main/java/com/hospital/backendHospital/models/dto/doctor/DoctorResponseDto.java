package com.hospital.backendHospital.models.dto.doctor;

import com.hospital.backendHospital.models.dto.doctorSchedule.DoctorScheduleResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DoctorResponseDto {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private Long specialtyId;
    private String specialty;
    private BigDecimal consultationCost;
    private String phoneNumber;
    private LocalDate registerDate;
    private boolean active;
    private List<DoctorScheduleResponseDto> schedules;
}
