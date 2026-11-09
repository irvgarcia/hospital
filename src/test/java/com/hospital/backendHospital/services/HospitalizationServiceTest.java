package com.hospital.backendHospital.services;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.HospitalizationMapper;
import com.hospital.backendHospital.models.dto.hospitalization.CreateHospitalizationDto;
import com.hospital.backendHospital.models.dto.hospitalization.HospitalizationResponseDto;
import com.hospital.backendHospital.models.entity.Doctor;
import com.hospital.backendHospital.models.entity.Hospitalization;
import com.hospital.backendHospital.models.entity.Patient;
import com.hospital.backendHospital.models.entity.Role;
import com.hospital.backendHospital.models.entity.RoleEnum;
import com.hospital.backendHospital.models.entity.Room;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.models.filters.HospitalizationFilterRequest;
import com.hospital.backendHospital.repositories.DoctorRepository;
import com.hospital.backendHospital.repositories.HospitalizationRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.repositories.RoomRepository;
import com.hospital.backendHospital.services.impl.HospitalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HospitalizationServiceTest {

    @Mock private HospitalizationRepository hospitalizationRepository;
    @Mock private HospitalizationMapper hospitalizationMapper;
    @Mock private PatientRepository patientRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private DoctorRepository doctorRepository;

    @InjectMocks
    private HospitalizationService hospitalizationService;

    private Hospitalization hospitalization;
    private CreateHospitalizationDto createDto;
    private HospitalizationResponseDto responseDto;
    private Patient patient;
    private Room room;
    private Doctor doctor;
    private User receptionist;

    private static User userWithRole(long id, RoleEnum roleEnum) {
        return User.builder()
                .id(id)
                .roles(Set.of(Role.builder().roleEnum(roleEnum).build()))
                .build();
    }

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .id(1L)
                .build();

        room = Room.builder()
                .id(1L)
                .build();

        doctor = Doctor.builder()
                .id(1L)
                .build();

        receptionist = userWithRole(10L, RoleEnum.RECEPTIONIST);

        createDto = CreateHospitalizationDto.builder()
                .patientId(patient.getId())
                .doctorId(doctor.getId())
                .roomId(room.getId())
                .reason("no reason")
                .costPerDay(BigDecimal.valueOf(300))
                .build();

        hospitalization = Hospitalization.builder()
                .patient(patient)
                .doctor(new Doctor())
                .room(room)
                .admissionDate(LocalDate.of(2025, 1, 12))
                .dischargeDate(null)
                .reason("no reason")
                .build();

        responseDto = HospitalizationResponseDto.builder()
                .admissionDate(hospitalization.getAdmissionDate())
                .reason("no reason")
                .build();

    }

    @Test
    void testGetHospitalizations() {
        HospitalizationFilterRequest filter = HospitalizationFilterRequest.builder()
                .date(LocalDate.of(2025, 1, 12))
                .patientId(1L)
                .doctorId(1L)
                .roomId(1L)
                .status(true)
                .build();

        Pageable pageable = PageRequest.of(0, 5);

        List<Hospitalization> hospitalizations = List.of(hospitalization);
        Page<Hospitalization> hospitalizationPage = new PageImpl<>(hospitalizations, pageable, hospitalizations.size());

        when(hospitalizationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(hospitalizationPage);
        when(hospitalizationMapper.toDto(hospitalization)).thenReturn(responseDto);

        Page<HospitalizationResponseDto> result = hospitalizationService.filterHospitalization(filter, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(filter.getDate(), result.getContent().get(0).getAdmissionDate());

        verify(hospitalizationRepository).findAll(any(Specification.class), eq(pageable));
        verify(hospitalizationMapper).toDto(hospitalization);
    }

    @Test
    void testGetHospitalizationsWithoutFilters() {
        HospitalizationFilterRequest filter = new HospitalizationFilterRequest();

        Pageable pageable = PageRequest.of(0, 5);

        List<Hospitalization> hospitalizations = List.of(hospitalization);
        Page<Hospitalization> hospitalizationPage = new PageImpl<>(hospitalizations, pageable, hospitalizations.size());

        when(hospitalizationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(hospitalizationPage);
        when(hospitalizationMapper.toDto(hospitalization)).thenReturn(responseDto);

        Page<HospitalizationResponseDto> result = hospitalizationService.filterHospitalization(filter, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(hospitalization.getAdmissionDate(), result.getContent().get(0).getAdmissionDate());

        verify(hospitalizationRepository).findAll(any(Specification.class), eq(pageable));
        verify(hospitalizationMapper).toDto(hospitalization);
    }

    @Test
    void testCreateHospitalization() {
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(hospitalizationRepository.existsByPatientAndActiveTrue(patient)).thenReturn(false);
        when(roomRepository.save(room)).thenReturn(room);
        ArgumentCaptor<Hospitalization> hospitalizationCaptor = ArgumentCaptor.forClass(Hospitalization.class);

        when(hospitalizationRepository.save(any(Hospitalization.class))).thenReturn(hospitalization);
        when(hospitalizationMapper.toDto(any(Hospitalization.class))).thenReturn(responseDto);

        HospitalizationResponseDto result = hospitalizationService.createHospitalization(receptionist, createDto);

        // Verifica que se guardó el Hospitalization
        verify(hospitalizationRepository).save(hospitalizationCaptor.capture());
        Hospitalization savedHospitalization = hospitalizationCaptor.getValue();

        // Verifica los datos del objeto guardado
        assertEquals(patient, savedHospitalization.getPatient());
        assertEquals(doctor, savedHospitalization.getDoctor());
        assertEquals(room, savedHospitalization.getRoom());
        assertEquals("no reason", savedHospitalization.getReason());
        assertNotNull(savedHospitalization.getAdmissionDate());
        assertNull(savedHospitalization.getDischargeDate());
        assertTrue(savedHospitalization.isActive());
    }

    @Test
    void testCreateHospitalization_ShouldThrowEntityNotFoundPatient() {
        when(patientRepository.findById(createDto.getPatientId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, ()-> hospitalizationService.createHospitalization(receptionist, createDto));

        assertEquals("Patient not found", exception.getMessage());
    }

    @Test
    void testCreateHospitalization_DoctorWithoutDoctorIdIsAssignedToThemselves() {
        User doctorUser = userWithRole(20L, RoleEnum.DOCTOR);
        createDto.setDoctorId(null);

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctor));
        when(hospitalizationRepository.existsByPatientAndActiveTrue(patient)).thenReturn(false);
        ArgumentCaptor<Hospitalization> hospitalizationCaptor = ArgumentCaptor.forClass(Hospitalization.class);

        hospitalizationService.createHospitalization(doctorUser, createDto);

        verify(hospitalizationRepository).save(hospitalizationCaptor.capture());
        assertEquals(doctor, hospitalizationCaptor.getValue().getDoctor());
        verify(doctorRepository, never()).findById(any());
    }

    @Test
    void testCreateHospitalization_ShouldRequireDoctorForReceptionist() {
        createDto.setDoctorId(null);

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        InvalidDataException exception = assertThrows(InvalidDataException.class, ()-> hospitalizationService.createHospitalization(receptionist, createDto));

        assertEquals("Doctor is required", exception.getMessage());
        verify(hospitalizationRepository, never()).save(any());
    }

    @Test
    void testCreateHospitalization_ShouldThrowEntityNotFoundDoctor() {
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, ()-> hospitalizationService.createHospitalization(receptionist, createDto));

        assertEquals("Doctor not found with id 1", exception.getMessage());
    }

    @Test
    void testCreateHospitalization_ShouldRejectInactiveDoctor() {
        doctor.setActive(false);

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        InvalidDataException exception = assertThrows(InvalidDataException.class, ()-> hospitalizationService.createHospitalization(receptionist, createDto));

        assertEquals("Doctor is inactive", exception.getMessage());
        verify(hospitalizationRepository, never()).save(any());
    }

    @Test
    void testCreateHospitalization_ShouldThrowEntityNotFoundRoom() {
        when(patientRepository.findById(createDto.getPatientId())).thenReturn(Optional.of(patient));
        when(roomRepository.findById(createDto.getRoomId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, ()-> hospitalizationService.createHospitalization(receptionist, createDto));

        assertEquals("Room not found", exception.getMessage());
    }

    @Test
    void testCreateHospitalization_ShouldThrowInvalidDataIsHospitalized() {
        when(patientRepository.findById(createDto.getPatientId())).thenReturn(Optional.of(patient));
        when(roomRepository.findById(createDto.getRoomId())).thenReturn(Optional.of(room));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(hospitalizationRepository.existsByPatientAndActiveTrue(patient)).thenReturn(true);

        InvalidDataException exception = assertThrows(InvalidDataException.class, ()-> hospitalizationService.createHospitalization(receptionist, createDto));

        assertEquals("Patient is already hospitalized", exception.getMessage());
    }

    @Test
    void testCreateHospitalization_ShouldThrowInvalidDataIsAvailable() {
        room.setAvailable(false);

        when(patientRepository.findById(createDto.getPatientId())).thenReturn(Optional.of(patient));
        when(roomRepository.findById(createDto.getRoomId())).thenReturn(Optional.of(room));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(hospitalizationRepository.existsByPatientAndActiveTrue(patient)).thenReturn(false);


        InvalidDataException exception = assertThrows(InvalidDataException.class, ()-> hospitalizationService.createHospitalization(receptionist, createDto));

        assertEquals("Room isnt available", exception.getMessage());
    }

    @Test
    void testDischargeDate() {
        ArgumentCaptor<Hospitalization> hospitalizationCaptor = ArgumentCaptor.forClass(Hospitalization.class);

        when(hospitalizationRepository.findById(hospitalization.getId())).thenReturn(Optional.of(hospitalization));
        when(hospitalizationRepository.save(any(Hospitalization.class))).thenReturn(hospitalization);

        hospitalizationService.dischargeDateById(hospitalization.getId());

        verify(hospitalizationRepository).save(hospitalizationCaptor.capture());
        Hospitalization dischargedHospitalization = hospitalizationCaptor.getValue();

        assertFalse(dischargedHospitalization.isActive());
        assertNotNull(dischargedHospitalization.getDischargeDate());
        assertTrue(dischargedHospitalization.getRoom().isAvailable());
    }

    @Test
    void testDischargeDate_ShouldThrowEntityNotFound() {
        when(hospitalizationRepository.findById(hospitalization.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, ()-> hospitalizationService.dischargeDateById(hospitalization.getId()));

        assertEquals("Hospitalization not found", exception.getMessage());
    }
}
