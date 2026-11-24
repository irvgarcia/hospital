package com.hospital.backendHospital.services;

import com.hospital.backendHospital.auth.service.TokenService;
import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.PatientMapper;
import com.hospital.backendHospital.models.dto.patient.CreatePatientDto;
import com.hospital.backendHospital.models.dto.patient.RegisterPatientDto;
import com.hospital.backendHospital.models.dto.patient.UpdatePatientDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.*;
import com.hospital.backendHospital.services.impl.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** A3 (perfil unico y registro en un paso) y A5 (desactivacion). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private HospitalizationRepository hospitalizationRepository;
    @Mock private PatientMapper patientMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;

    @InjectMocks private PatientService service;

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = TestData.patient(2);
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));
        when(roleRepository.findByRoleEnum(RoleEnum.PATIENT)).thenReturn(Optional.of(Role.builder().roleEnum(RoleEnum.PATIENT).build()));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static CreatePatientDto profile() {
        return CreatePatientDto.builder().phoneNumber("5511112222").address(" Calle 1 ").dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE).allergies("Ninguna").bloodType(BloodType.AB_NEGATIVE).chronicDiseases("Ninguna")
                .currentMedications("Ninguno").emergencyContact("5500000000").emergencyContactName("Ana")
                .emergencyContactRelation("Madre").height(1.8).weight(80.0).smoker(true).build();
    }

    @Test
    void profileCanOnlyBeCreatedOnce() {
        User user = TestData.user(50, RoleEnum.PATIENT);
        when(patientRepository.existsByUserId(50L)).thenReturn(true);
        assertThatThrownBy(() -> service.createPatient(user, profile())).isInstanceOf(ConflictException.class);

        when(patientRepository.existsByUserId(50L)).thenReturn(false);
        service.createPatient(user, profile());
        verify(patientRepository).save(argThat(p -> p.getUser() == user && p.getBloodType() == BloodType.AB_NEGATIVE
                && p.getAddress().equals("Calle 1") && p.isSmoker()));
    }

    @Test
    void staffRegistersUserAndProfileTogether() {
        RegisterPatientDto dto = RegisterPatientDto.builder().email(" NUEVO@test.com ").firstName("Luis").lastName("Paz")
                .password("12345678").profile(profile()).build();
        service.registerPatient(dto);
        verify(userRepository).save(argThat(u -> u.getEmail().equals("nuevo@test.com") && u.hasRole(RoleEnum.PATIENT)));
        verify(patientRepository).save(any());

        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(true);
        assertThatThrownBy(() -> service.registerPatient(dto)).isInstanceOf(ConflictException.class);
    }

    @Test
    void deactivationIsBlockedByPendingAppointmentsOrHospitalization() {
        when(appointmentRepository.existsByPatientIdAndStatusAndDateGreaterThanEqual(2L, AppointmentStatus.PENDING, LocalDate.now()))
                .thenReturn(true);
        assertThatThrownBy(() -> service.deactivatePatient(2L)).isInstanceOf(InvalidDataException.class).hasMessageContaining("citas pendientes");

        when(appointmentRepository.existsByPatientIdAndStatusAndDateGreaterThanEqual(2L, AppointmentStatus.PENDING, LocalDate.now()))
                .thenReturn(false);
        when(hospitalizationRepository.existsByPatientIdAndActiveTrue(2L)).thenReturn(true);
        assertThatThrownBy(() -> service.deactivatePatient(2L)).hasMessageContaining("hospitalizado");

        when(hospitalizationRepository.existsByPatientIdAndActiveTrue(2L)).thenReturn(false);
        service.deactivatePatient(2L);
        assertThat(patient.isActive()).isFalse();
        verify(tokenService).revokeAllUserTokens(patient.getUser());

        service.activatePatient(2L);
        assertThat(patient.isActive()).isTrue();
        assertThat(patient.getUser().isActive()).isTrue();
    }

    @Test
    void updateChangesOnlyTheGivenFields() {
        service.updatePatient(2L, UpdatePatientDto.builder().weight(65.5).allergies(" Polen ").build());
        assertThat(patient.getWeight()).isEqualTo(65.5);
        assertThat(patient.getAllergies()).isEqualTo("Polen");
        assertThat(patient.getHeight()).isEqualTo(1.7);
    }
}
