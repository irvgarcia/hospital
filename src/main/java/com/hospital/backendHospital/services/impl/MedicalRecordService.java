package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.MedicalRecordMapper;
import com.hospital.backendHospital.models.dto.medicalRecord.CreateMedicalRecordDto;
import com.hospital.backendHospital.models.dto.medicalRecord.MedicalRecordResponseDto;
import com.hospital.backendHospital.models.entity.MedicalRecord;
import com.hospital.backendHospital.models.entity.Patient;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.repositories.MedicalRecordRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.services.IMedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MedicalRecordService implements IMedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    private final PatientRepository patientRepository;

    private final MedicalRecordMapper medicalRecordMapper;

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponseDto listMedicalRecordByPatientId(Long id) {
        MedicalRecord medicalRecord = medicalRecordRepository.findByPatientId(id).orElseThrow(() -> new EntityNotFoundException("Medical record not found"));

        return medicalRecordMapper.toResponseDto(medicalRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponseDto getMedicalRecordOfPatientUser(User user) {
        // El usuario autenticado se traduce a su perfil de paciente: user.id y patient.id son ids distintos
        Patient patient = patientRepository.findByUser(user).orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        return listMedicalRecordByPatientId(patient.getId());
    }

    // Considerar crear historial medico solo para citas completadas
    @Override
    @Transactional
    public MedicalRecordResponseDto createMedicalRecord(CreateMedicalRecordDto createMedicalRecordDto) {
        Patient patient = patientRepository.findById(createMedicalRecordDto.getPatientId()).orElseThrow(()-> new EntityNotFoundException("Patient not found"));

        if (medicalRecordRepository.existsByPatientId(patient.getId())){
            throw new InvalidDataException("Medical record already exists for this patient");
        }

        MedicalRecord medicalRecord = MedicalRecord.builder()
                .patient(patient)
                .date(LocalDate.now())
                .diagnostics(createMedicalRecordDto.getDiagnostics())
                .treatment(createMedicalRecordDto.getTreatment())
                .build();

        medicalRecordRepository.save(medicalRecord);

        return medicalRecordMapper.toResponseDto(medicalRecord);
    }
}
