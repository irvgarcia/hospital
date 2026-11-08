package com.hospital.backendHospital.services;

import com.hospital.backendHospital.auth.service.TokenService;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.PatientMapper;
import com.hospital.backendHospital.models.dto.patient.CreatePatientDto;
import com.hospital.backendHospital.models.dto.patient.PatientResponseDto;
import com.hospital.backendHospital.models.dto.patient.UpdatePatientDto;
import com.hospital.backendHospital.models.entity.Appointment;
import com.hospital.backendHospital.models.entity.Patient;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.models.filters.PatientFilterRequest;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.services.impl.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PatientMapper patientMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;

    @InjectMocks
    private PatientService patientService;

    private CreatePatientDto createDto;
    private UpdatePatientDto updateDto;
    private User user;
    private Patient patient;

    @BeforeEach
    void setUp() {
        createDto = CreatePatientDto.builder()
                .phoneNumber("7772202142")
                .address("lacorona")
                .dateOfBirth(LocalDate.of(2004, 06, 28))
                .gender("MEN")
                .allergies("NO")
                .bloodType("A+")
                .chronicDiseases("dsadsa")
                .currentMedications("dsadsadas")
                .emergencyContact("7772381255")
                .emergencyContactName("ISRAEL GARCIA")
                .emergencyContactRelation("DAD")
                .height(1.70)
                .weight(68.2)
                .smoker(false)
                .build();

        user = User.builder()
                .email("dsadsadsadas@gmail.com")
                .firstName("ELvergalinda")
                .lastName("Elveralarga")
                .password("dsadasdsadasdsadsa")
                .build();

        patient = Patient.builder()
                .user(user)
                .phoneNumber(createDto.getPhoneNumber())
                .address(createDto.getAddress())
                .dateOfBirth(createDto.getDateOfBirth())
                .gender(createDto.getGender())
                .allergies(createDto.getAllergies())
                .bloodType(createDto.getBloodType())
                .chronicDiseases(createDto.getChronicDiseases())
                .currentMedications(createDto.getCurrentMedications())
                .emergencyContact(createDto.getEmergencyContact())
                .emergencyContactName(createDto.getEmergencyContactName())
                .emergencyContactRelation(createDto.getEmergencyContactRelation())
                .height(createDto.getHeight())
                .weight(createDto.getWeight())
                .smoker(createDto.isSmoker())
                .build();
    }

    @Test
    void testFilterPatients() {
        // Arrange
        PatientFilterRequest filter = new PatientFilterRequest();
        filter.setBloodType("A+");
        filter.setStatus(true);
        filter.setRegisterDate(LocalDate.of(2023, 7, 30));
        filter.setHospitalized(false);

        Pageable pageable = PageRequest.of(0, 10);

        Patient patient1 = patient;

        // Creamos un segundo paciente con valores también compatibles con los filtros
        Patient patient2 = Patient.builder()
                .user(user)
                .phoneNumber("7771112233")
                .address("avenida juarez")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("WOMAN")
                .allergies("PENICILINA")
                .bloodType("A+")
                .chronicDiseases("HIPERTENSIÓN")
                .currentMedications("ASPIRINA")
                .emergencyContact("7773334444")
                .emergencyContactName("MARIA LOPEZ")
                .emergencyContactRelation("MOM")
                .height(1.65)
                .weight(60.5)
                .smoker(false)
                .active(true)
                .build();

        List<Patient> patients = List.of(patient1, patient2);
        Page<Patient> patientPage = new PageImpl<>(patients, pageable, patients.size());

        PatientResponseDto dto1 = new PatientResponseDto();
        PatientResponseDto dto2 = new PatientResponseDto();

        when(patientRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(patientPage);
        when(patientMapper.toResponseDto(patient1)).thenReturn(dto1);
        when(patientMapper.toResponseDto(patient2)).thenReturn(dto2);

        // Act
        Page<PatientResponseDto> result = patientService.filterPatients(filter, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(patientRepository).findAll(any(Specification.class), eq(pageable));
        verify(patientMapper, times(2)).toResponseDto(any(Patient.class));
    }


    @Test
    void testFilterPatientsWithoutFilters() {
        PatientFilterRequest filter = new PatientFilterRequest(); // todos los campos null
        Pageable pageable = PageRequest.of(0, 5);

        Patient patient = new Patient();
        Page<Patient> patientPage = new PageImpl<>(List.of(patient));

        when(patientRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(patientPage);
        when(patientMapper.toResponseDto(patient)).thenReturn(new PatientResponseDto());

        Page<PatientResponseDto> result = patientService.filterPatients(filter, pageable);

        assertEquals(1, result.getContent().size());
        verify(patientRepository).findAll(any(Specification.class), eq(pageable));
    }



    @Test
    void testCreatePatient() {
        when(patientRepository.save(any())).thenReturn(patient);
        when(patientMapper.toResponseDto(any())).thenReturn(new PatientResponseDto());

        PatientResponseDto result = patientService.createPatient(user, createDto);

        assertNotNull(result);
        verify(patientRepository).save(any());
    }


    @Test
    void testUpdatePatient() {
        // Arrange
        updateDto = UpdatePatientDto.builder()
                .firstName("NuevoNombre")
                .lastName("NuevoApellido")
                .newPassword("nuevaPassword123")
                .newEmergencyContact("7771112233")
                .build();

        when(patientRepository.findByUser(user)).thenReturn(Optional.of(patient));
        when(patientMapper.toResponseDto(any(Patient.class))).thenReturn(new PatientResponseDto());
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(updateDto.getNewPassword())).thenReturn("hashedPassword");

        // Act
        PatientResponseDto result = patientService.updatePatient(user, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("NuevoNombre", patient.getUser().getFirstName());
        assertEquals("NuevoApellido", patient.getUser().getLastName());
        assertEquals("hashedPassword", patient.getUser().getPassword());
        assertEquals("7771112233", patient.getEmergencyContact());

        verify(patientRepository).save(patient);
        verify(patientMapper).toResponseDto(patient);
    }

    @Test
    void testUpdatePatient_shouldNotUpdateAnythingWhenFieldsAreNull() {
        updateDto = UpdatePatientDto.builder().build(); // todos los campos null

        when(patientRepository.findByUser(user)).thenReturn(Optional.of(patient));
        when(patientMapper.toResponseDto(any())).thenReturn(new PatientResponseDto());

        PatientResponseDto result = patientService.updatePatient(user, updateDto);

        assertNotNull(result);
        verify(patientRepository).save(patient);
    }


    @Test
    void testUpdatePatient_shouldThrowWhenPatientNotFound() {
        // Arrange
        updateDto = UpdatePatientDto.builder()
                .firstName("NuevoNombre")
                .build();

        when(patientRepository.findByUser(user)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> patientService.updatePatient(user, updateDto));
    }

    @Test
    void testDeactivatePatient_successfully() {
        // Arrange
        Appointment appointment = new Appointment();
        appointment.setDate(LocalDate.now().minusDays(1)); // fecha pasada → se permite desactivar

        patient.setAppointments(List.of(appointment));

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        // Act
        patientService.deactivatePatient(1L);

        // Assert
        assertFalse(patient.isActive());
        assertFalse(patient.getUser().isActive());
        verify(patientRepository).save(patient);
        verify(tokenService).revokeAllUserTokens(patient.getUser());
    }

    @Test
    void testDeactivatePatient_shouldThrowWhenPatientNotFound() {
        // Arrange
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> patientService.deactivatePatient(1L));

        assertEquals("Patient not found with id 1", ex.getMessage());
    }

    @Test
    void testDeactivatePatient_shouldThrowWhenPatientHasFutureAppointments() {
        // Arrange
        Appointment appointment = new Appointment();
        appointment.setDate(LocalDate.now().plusDays(1));

        patient.setAppointments(List.of(appointment));

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        // Act & Assert
        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> patientService.deactivatePatient(1L));

        assertTrue(ex.getMessage().contains("cannot be deactivated"));
    }
}
