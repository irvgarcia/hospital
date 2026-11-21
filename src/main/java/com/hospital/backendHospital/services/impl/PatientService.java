package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.auth.service.TokenService;
import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.PatientMapper;
import com.hospital.backendHospital.models.dto.patient.CreatePatientDto;
import com.hospital.backendHospital.models.dto.patient.PatientResponseDto;
import com.hospital.backendHospital.models.dto.patient.RegisterPatientDto;
import com.hospital.backendHospital.models.dto.patient.UpdatePatientDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.models.filters.PatientFilterRequest;
import com.hospital.backendHospital.repositories.AppointmentRepository;
import com.hospital.backendHospital.repositories.HospitalizationRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.repositories.RoleRepository;
import com.hospital.backendHospital.repositories.UserRepository;
import com.hospital.backendHospital.services.IPatientService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PatientService implements IPatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AppointmentRepository appointmentRepository;
    private final HospitalizationRepository hospitalizationRepository;
    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponseDto> filterPatients(PatientFilterRequest filter, Pageable pageable) {
        Specification<Patient> spec = Specification.where(null);
        if (filter.getName() != null && !filter.getName().isBlank()) {
            String pattern = "%" + filter.getName().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                Join<Object, Object> user = root.join("user");
                return cb.or(
                        cb.like(cb.lower(user.get("firstName")), pattern),
                        cb.like(cb.lower(user.get("lastName")), pattern),
                        cb.like(cb.lower(cb.concat(cb.concat(user.get("firstName"), " "), user.get("lastName"))), pattern),
                        cb.like(cb.lower(user.get("email")), pattern));
            });
        }
        if (filter.getBloodType() != null && !filter.getBloodType().isBlank()) {
            BloodType bloodType = BloodType.parse(filter.getBloodType())
                    .orElseThrow(() -> new InvalidDataException("Grupo sanguíneo no válido: " + filter.getBloodType()));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("bloodType"), bloodType));
        }
        if (filter.getStatus() != null){
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), filter.getStatus()));
        }
        if (filter.getRegisterDate() != null){
            spec = spec.and((root, query, cb) -> cb.equal(root.get("registerDate"), filter.getRegisterDate()));
        }
        // "Hospitalizado" no es un campo del paciente: es tener una hospitalizacion activa
        if (filter.getHospitalized() != null){
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> activeHospitalization = query.subquery(Long.class);
                Root<Hospitalization> hospitalization = activeHospitalization.from(Hospitalization.class);
                activeHospitalization.select(hospitalization.get("id"))
                        .where(cb.equal(hospitalization.get("patient"), root),
                                cb.isTrue(hospitalization.get("active")));
                return filter.getHospitalized()
                        ? cb.exists(activeHospitalization)
                        : cb.not(cb.exists(activeHospitalization));
            });
        }
        return patientRepository.findAll(spec, pageable).map(patientMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponseDto getPatient(Long id) {
        return patientMapper.toResponseDto(findPatient(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponseDto getMyProfile(User user) {
        return patientMapper.toResponseDto(findByUser(user));
    }

    /** Segundo paso del registro publico: el usuario PATIENT completa su perfil (A3: solo una vez). */
    @Override
    @Transactional
    public PatientResponseDto createPatient(User user, CreatePatientDto dto) {
        if (patientRepository.existsByUserId(user.getId())) {
            throw new ConflictException("Tu perfil de paciente ya existe; usa la edición de perfil para cambiarlo");
        }
        Patient patient = buildPatient(user, dto);
        patientRepository.save(patient);
        return patientMapper.toResponseDto(patient);
    }

    /** A3: registro en un paso (recepcion/admin): usuario y perfil en la misma transaccion. */
    @Override
    @Transactional
    public PatientResponseDto registerPatient(RegisterPatientDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Ya existe un usuario con el email " + email);
        }
        Role patientRole = roleRepository.findByRoleEnum(RoleEnum.PATIENT)
                .orElseThrow(() -> new EntityNotFoundException("No existe el rol PATIENT"));
        User user = userRepository.save(User.builder()
                .email(email)
                .firstName(dto.getFirstName().trim())
                .lastName(dto.getLastName().trim())
                .password(passwordEncoder.encode(dto.getPassword()))
                .roles(new HashSet<>(Set.of(patientRole)))
                .build());
        Patient patient = buildPatient(user, dto.getProfile());
        patientRepository.save(patient);
        return patientMapper.toResponseDto(patient);
    }

    @Override
    @Transactional
    public PatientResponseDto updateMyProfile(User user, UpdatePatientDto dto) {
        Patient patient = findByUser(user);
        applyUpdate(patient, dto);
        patientRepository.save(patient);
        return patientMapper.toResponseDto(patient);
    }

    @Override
    @Transactional
    public PatientResponseDto updatePatient(Long id, UpdatePatientDto dto) {
        Patient patient = findPatient(id);
        applyUpdate(patient, dto);
        patientRepository.save(patient);
        return patientMapper.toResponseDto(patient);
    }

    @Override
    @Transactional
    public void deactivatePatient(Long id) {
        Patient patient = findPatient(id);
        // A5: solo bloquean las citas PENDIENTES de hoy en adelante, y estar hospitalizado
        if (appointmentRepository.existsByPatientIdAndStatusAndDateGreaterThanEqual(id, AppointmentStatus.PENDING, LocalDate.now())) {
            throw new InvalidDataException("No se puede desactivar al paciente porque tiene citas pendientes; cancélalas primero");
        }
        if (hospitalizationRepository.existsByPatientIdAndActiveTrue(id)) {
            throw new InvalidDataException("No se puede desactivar al paciente porque está hospitalizado");
        }
        patient.setActive(false);
        patient.getUser().setActive(false);
        tokenService.revokeAllUserTokens(patient.getUser());
        patientRepository.save(patient);
    }

    @Override
    @Transactional
    public void activatePatient(Long id) {
        Patient patient = findPatient(id);
        patient.setActive(true);
        patient.getUser().setActive(true);
        patientRepository.save(patient);
    }

    private Patient buildPatient(User user, CreatePatientDto dto) {
        return Patient.builder()
                .user(user)
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress().trim())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .allergies(dto.getAllergies().trim())
                .bloodType(dto.getBloodType())
                .chronicDiseases(dto.getChronicDiseases().trim())
                .currentMedications(dto.getCurrentMedications().trim())
                .emergencyContact(dto.getEmergencyContact())
                .emergencyContactName(dto.getEmergencyContactName().trim())
                .emergencyContactRelation(dto.getEmergencyContactRelation().trim())
                .height(dto.getHeight())
                .weight(dto.getWeight())
                .smoker(Boolean.TRUE.equals(dto.getSmoker()))
                .registerDate(LocalDate.now())
                .build();
    }

    private void applyUpdate(Patient patient, UpdatePatientDto dto) {
        if (dto.getFirstName() != null) patient.getUser().setFirstName(dto.getFirstName().trim());
        if (dto.getLastName() != null) patient.getUser().setLastName(dto.getLastName().trim());
        if (dto.getPhoneNumber() != null) patient.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getAddress() != null) patient.setAddress(dto.getAddress().trim());
        if (dto.getAllergies() != null) patient.setAllergies(dto.getAllergies().trim());
        if (dto.getChronicDiseases() != null) patient.setChronicDiseases(dto.getChronicDiseases().trim());
        if (dto.getCurrentMedications() != null) patient.setCurrentMedications(dto.getCurrentMedications().trim());
        if (dto.getEmergencyContact() != null) patient.setEmergencyContact(dto.getEmergencyContact());
        if (dto.getEmergencyContactName() != null) patient.setEmergencyContactName(dto.getEmergencyContactName().trim());
        if (dto.getEmergencyContactRelation() != null) patient.setEmergencyContactRelation(dto.getEmergencyContactRelation().trim());
        if (dto.getHeight() != null) patient.setHeight(dto.getHeight());
        if (dto.getWeight() != null) patient.setWeight(dto.getWeight());
        if (dto.getSmoker() != null) patient.setSmoker(dto.getSmoker());
    }

    private Patient findPatient(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe el paciente con id " + id));
    }

    private Patient findByUser(User user) {
        return patientRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("Completa tu perfil de paciente antes de continuar"));
    }
}
