package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.medicalRecord.CreateMedicalRecordDto;
import com.hospital.backendHospital.models.dto.medicalRecord.MedicalRecordResponseDto;
import com.hospital.backendHospital.models.dto.medicalRecord.UpdateMedicalRecordDto;
import com.hospital.backendHospital.models.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IMedicalRecordService {

    Page<MedicalRecordResponseDto> listByPatient(Long patientId, Pageable pageable);

    Page<MedicalRecordResponseDto> listMine(User user, Pageable pageable);

    MedicalRecordResponseDto createMedicalRecord(User user, CreateMedicalRecordDto createMedicalRecordDto);

    MedicalRecordResponseDto updateMedicalRecord(User user, Long id, UpdateMedicalRecordDto updateMedicalRecordDto);
}
