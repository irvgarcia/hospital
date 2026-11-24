package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.patient.CreatePatientDto;
import com.hospital.backendHospital.models.dto.patient.PatientResponseDto;
import com.hospital.backendHospital.models.dto.patient.RegisterPatientDto;
import com.hospital.backendHospital.models.dto.patient.UpdatePatientDto;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.models.filters.PatientFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IPatientService {

    Page<PatientResponseDto> filterPatients(PatientFilterRequest filter, Pageable pageable);

    PatientResponseDto getPatient(Long id);

    PatientResponseDto getMyProfile(User user);

    PatientResponseDto createPatient(User user, CreatePatientDto createPatientDto);

    PatientResponseDto registerPatient(RegisterPatientDto registerPatientDto);

    PatientResponseDto updateMyProfile(User user, UpdatePatientDto updatePatientDto);

    PatientResponseDto updatePatient(Long id, UpdatePatientDto updatePatientDto);

    void deactivatePatient(Long id);

    void activatePatient(Long id);
}
