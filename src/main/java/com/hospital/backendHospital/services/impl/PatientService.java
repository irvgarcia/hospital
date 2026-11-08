package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.PatientMapper;
import com.hospital.backendHospital.models.dto.patient.CreatePatientDto;
import com.hospital.backendHospital.models.dto.patient.PatientResponseDto;
import com.hospital.backendHospital.models.dto.patient.PatientSummaryDto;
import com.hospital.backendHospital.models.dto.patient.UpdatePatientDto;
import com.hospital.backendHospital.models.entity.Appointment;
import com.hospital.backendHospital.models.entity.Patient;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.models.filters.PatientFilterRequest;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.repositories.UserRepository;
import com.hospital.backendHospital.services.IPatientService;
import com.hospital.backendHospital.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService implements IPatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;


    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponseDto> filterPatients(PatientFilterRequest filter, Pageable pageable) {
        Specification<Patient> spec = Specification.where(null);

        if (filter.getBloodType() != null){
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("bloodType"), filter.getBloodType()));
        }

        if (filter.getStatus() != null){
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), filter.getStatus()));
        }

        if (filter.getRegisterDate() != null){
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("registerDate"), filter.getRegisterDate()));
        }

        if (filter.getHospitalized() != null){
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("hospitalized"), filter.getHospitalized()));
        }

        return patientRepository.findAll(spec, pageable)
                .map(patientMapper::toResponseDto);
    }

    @Override
    @Transactional
    public PatientResponseDto createPatient(User user, CreatePatientDto createPatientDto) {
        Patient patient = Patient.builder()
                .user(user)
                .phoneNumber(createPatientDto.getPhoneNumber())
                .address(createPatientDto.getAddress())
                .dateOfBirth(createPatientDto.getDateOfBirth())
                .gender(createPatientDto.getGender())
                .allergies(createPatientDto.getAllergies())
                .bloodType(createPatientDto.getBloodType())
                .chronicDiseases(createPatientDto.getChronicDiseases())
                .currentMedications(createPatientDto.getCurrentMedications())
                .emergencyContact(createPatientDto.getEmergencyContact())
                .emergencyContactName(createPatientDto.getEmergencyContactName())
                .emergencyContactRelation(createPatientDto.getEmergencyContactRelation())
                .height(createPatientDto.getHeight())
                .weight(createPatientDto.getWeight())
                .smoker(createPatientDto.isSmoker())
                .registerDate(LocalDate.now())
                .build();

        patientRepository.save(patient);

        return patientMapper.toResponseDto(patient);
    }

    @Override
    @Transactional
    public PatientResponseDto updatePatient(User user, UpdatePatientDto updatePatientDto) {
        Patient patient = patientRepository.findByUser(user).orElseThrow(()-> new EntityNotFoundException("Patient not found"));

        if (updatePatientDto.getFirstName() != null){
            patient.getUser().setFirstName(updatePatientDto.getFirstName());
        }
        if (updatePatientDto.getLastName() != null){
            patient.getUser().setLastName(updatePatientDto.getLastName());
        }
        if (updatePatientDto.getNewPassword() != null){
            patient.getUser().setPassword(passwordEncoder.encode(updatePatientDto.getNewPassword()));
        }
        if (updatePatientDto.getNewEmergencyContact() != null){
            patient.setEmergencyContact(updatePatientDto.getNewEmergencyContact());
        }

        patientRepository.save(patient);

        return patientMapper.toResponseDto(patient);
    }

    @Override
    @Transactional
    public void deactivatePatient(Long id) {
        Patient patient = patientRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Patient not found with id "+ id));

        List<Appointment> appointments = patient.getAppointments().stream()
                        .filter(appointment -> appointment.getDate().isAfter(LocalDate.now()))
                        .toList();

        if (!appointments.isEmpty()){
            throw new InvalidDataException("Patient " + patient.getUser().getFirstName() + " cannot be deactivated because he has scheduled appointments");
        }

        patient.setActive(false);
        patient.getUser().setActive(false);
        tokenService.revokeAllUserTokens(patient.getUser());

        patientRepository.save(patient);
    }
}
