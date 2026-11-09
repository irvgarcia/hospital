package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.medicalRecord.CreateMedicalRecordDto;
import com.hospital.backendHospital.models.dto.medicalRecord.MedicalRecordResponseDto;
import com.hospital.backendHospital.models.entity.User;

import java.util.List;

public interface IMedicalRecordService {
    MedicalRecordResponseDto listMedicalRecordByPatientId(Long id);

    MedicalRecordResponseDto getMedicalRecordOfPatientUser(User user);

    MedicalRecordResponseDto createMedicalRecord(CreateMedicalRecordDto createMedicalRecordDto);
}
