package com.hospital.backendHospital.models.dto.patient;

import com.hospital.backendHospital.models.entity.BloodType;
import com.hospital.backendHospital.models.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PatientResponseDto {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String allergies;
    private BloodType bloodType;
    private String chronicDiseases;
    private String currentMedications;
    private String emergencyContact;
    private String emergencyContactName;
    private String emergencyContactRelation;
    private Double height;
    private Double weight;
    private LocalDate registerDate;
    private boolean smoker;
    private boolean active;
}
