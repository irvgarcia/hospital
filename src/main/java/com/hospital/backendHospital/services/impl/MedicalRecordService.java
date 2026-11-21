package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.MedicalRecordMapper;
import com.hospital.backendHospital.models.dto.medicalRecord.CreateMedicalRecordDto;
import com.hospital.backendHospital.models.dto.medicalRecord.MedicalRecordResponseDto;
import com.hospital.backendHospital.models.dto.medicalRecord.UpdateMedicalRecordDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.AppointmentRepository;
import com.hospital.backendHospital.repositories.MedicalRecordRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.services.IMedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historial clinico (M8): una entrada por cita completada, escrita por el doctor que la atendio.
 */
@Service
@RequiredArgsConstructor
public class MedicalRecordService implements IMedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordMapper medicalRecordMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MedicalRecordResponseDto> listByPatient(Long patientId, Pageable pageable) {
        if (!patientRepository.existsById(patientId)) {
            throw new EntityNotFoundException("No existe el paciente con id " + patientId);
        }
        return medicalRecordRepository.findByPatientId(patientId, pageable).map(medicalRecordMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MedicalRecordResponseDto> listMine(User user, Pageable pageable) {
        // El usuario autenticado se traduce a su perfil de paciente: user.id y patient.id son ids distintos (C5)
        Patient patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("Completa tu perfil de paciente antes de continuar"));
        return medicalRecordRepository.findByPatientId(patient.getId(), pageable).map(medicalRecordMapper::toResponseDto);
    }

    @Override
    @Transactional
    public MedicalRecordResponseDto createMedicalRecord(User user, CreateMedicalRecordDto dto) {
        Appointment appointment = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new EntityNotFoundException("No existe la cita con id " + dto.getAppointmentId()));
        if (!appointment.getDoctor().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Solo puedes registrar el historial de tus propias citas");
        }
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new InvalidDataException("Solo se registra historial de citas completadas");
        }
        if (medicalRecordRepository.existsByAppointmentId(appointment.getId())) {
            throw new ConflictException("La cita ya tiene una entrada en el historial; edítala en lugar de crear otra");
        }
        MedicalRecord record = MedicalRecord.builder()
                .patient(appointment.getPatient())
                .doctor(appointment.getDoctor())
                .appointment(appointment)
                .date(appointment.getDate())
                .diagnostics(dto.getDiagnostics().trim())
                .treatment(dto.getTreatment().trim())
                .notes(blankToNull(dto.getNotes()))
                .build();
        medicalRecordRepository.save(record);
        return medicalRecordMapper.toResponseDto(record);
    }

    @Override
    @Transactional
    public MedicalRecordResponseDto updateMedicalRecord(User user, Long id, UpdateMedicalRecordDto dto) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe la entrada de historial con id " + id));
        if (record.getDoctor() == null || !record.getDoctor().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Solo el doctor que escribió la entrada puede editarla");
        }
        if (dto.getDiagnostics() != null) record.setDiagnostics(dto.getDiagnostics().trim());
        if (dto.getTreatment() != null) record.setTreatment(dto.getTreatment().trim());
        if (dto.getNotes() != null) record.setNotes(blankToNull(dto.getNotes()));
        medicalRecordRepository.save(record);
        return medicalRecordMapper.toResponseDto(record);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
