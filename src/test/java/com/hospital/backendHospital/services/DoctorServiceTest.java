package com.hospital.backendHospital.services;

import com.hospital.backendHospital.auth.service.TokenService;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.DoctorMapper;
import com.hospital.backendHospital.models.dto.doctor.CreateDoctorAndScheduleDto;
import com.hospital.backendHospital.models.dto.doctor.DoctorResponseDto;
import com.hospital.backendHospital.models.dto.doctor.UpdateDoctorAndScheduleDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.models.filters.DoctorFilterRequest;
import com.hospital.backendHospital.repositories.*;
import com.hospital.backendHospital.services.impl.DoctorService;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private DoctorScheduleRepository doctorScheduleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private DoctorMapper doctorMapper;

    @InjectMocks
    private DoctorService doctorService;

    private DoctorFilterRequest filter;
    private CreateDoctorAndScheduleDto createDto;
    private Doctor doctor;
    private User user;
    private Role doctorRole;
    private Specialty specialty;

    @BeforeEach
    void setUp() {

        CreateDoctorAndScheduleDto.DoctorScheduleDto scheduleDto = new CreateDoctorAndScheduleDto.DoctorScheduleDto();
        scheduleDto.setDayOfWeek(DayOfWeek.MONDAY);
        scheduleDto.setStartTime(LocalTime.of(9, 0));
        scheduleDto.setEndTime(LocalTime.of(17, 0));

        createDto = CreateDoctorAndScheduleDto.builder()
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("encoded")
                .specialtyId(1L)
                .phoneNumber("7771234567")
                .schedules(List.of(scheduleDto))
                .build();

        doctorRole = Role.builder().
                id(1L)
                .roleEnum(RoleEnum.DOCTOR)
                .build();
        specialty = Specialty.builder()
                .id(1L)
                .name("Cardiology")
                .build();

        user = User.builder()
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("encoded")
                .roles(Set.of(doctorRole))
                .build();

        doctor = Doctor.builder()
                .id(1L)
                .user(user)
                .specialty(specialty)
                .phoneNumber("7771234567")
                .registerDate(LocalDate.now())
                .build();

        filter = new DoctorFilterRequest();
    }

    @Test
    void testFilterDoctors() {
        // Arrange
        filter.setFirstName("John");
        filter.setLastName("Doe");
        filter.setSpecialty("Cardiology");
        filter.setStatus(true);
        filter.setRegisterDate(LocalDate.now());

        Pageable pageable = PageRequest.of(0, 10);

        Doctor doctor1 = this.doctor;
        Doctor doctor2 = Doctor.builder()
                .id(2L)
                .user(user)
                .specialty(specialty)
                .phoneNumber("7779876543")
                .registerDate(LocalDate.now())
                .build();

        List<Doctor> doctorList = List.of(doctor1, doctor2);
        Page<Doctor> doctorPage = new PageImpl<>(doctorList, pageable, doctorList.size());

        DoctorResponseDto dto1 = new DoctorResponseDto();
        DoctorResponseDto dto2 = new DoctorResponseDto();

        when(doctorRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(doctorPage);
        when(doctorMapper.toResponseDto(doctor1)).thenReturn(dto1);
        when(doctorMapper.toResponseDto(doctor2)).thenReturn(dto2);

        // Act
        Page<DoctorResponseDto> result = doctorService.filterDoctors(filter, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(doctorRepository).findAll(any(Specification.class), eq(pageable));
        verify(doctorMapper, times(2)).toResponseDto(any(Doctor.class));
    }

    @Test
    void testFilterDoctorsWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 5);

        Page<Doctor> doctorPage = new PageImpl<>(List.of(doctor));

        DoctorResponseDto dto = new DoctorResponseDto();

        when(doctorRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(doctorPage);
        when(doctorMapper.toResponseDto(doctor)).thenReturn(dto);

        Page<DoctorResponseDto> result = doctorService.filterDoctors(filter, pageable);

        assertEquals(1, result.getContent().size());
        verify(doctorRepository).findAll(any(Specification.class), eq(pageable));
        verify(doctorMapper).toResponseDto(doctor);
    }

    @Test
    void testCreateDoctor() {
        when(userRepository.existsByEmail(createDto.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleEnum(RoleEnum.DOCTOR)).thenReturn(Optional.of(doctorRole));
        when(specialtyRepository.findById(createDto.getSpecialtyId())).thenReturn(Optional.of(specialty));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(doctorScheduleRepository.existsByDoctorIdAndDayOfWeek(any(), any())).thenReturn(false);
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);
        when(doctorMapper.toResponseDto(any(Doctor.class))).thenReturn(new DoctorResponseDto());

        DoctorResponseDto result = doctorService.createDoctorAndSchedule(createDto);

        assertNotNull(result);
        verify(passwordEncoder).encode(createDto.getPassword());
        verify(doctorRepository).save(any());
        verify(doctorScheduleRepository, times(createDto.getSchedules().size())).save(any());
        verify(doctorMapper).toResponseDto(any());
    }

    @Test
    void shouldThrowIfEmailAlreadyExists() {
        when(userRepository.existsByEmail(createDto.getEmail())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> doctorService.createDoctorAndSchedule(createDto));
    }

    @Test
    void shouldThrowIfDoctorRoleNotFound() {
        when(userRepository.existsByEmail(createDto.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleEnum(RoleEnum.DOCTOR)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> doctorService.createDoctorAndSchedule(createDto));
    }

    @Test
    void shouldThrowIfSpecialtyNotFound() {
        when(userRepository.existsByEmail(createDto.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleEnum(RoleEnum.DOCTOR)).thenReturn(Optional.of(doctorRole));
        when(specialtyRepository.findById(createDto.getSpecialtyId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> doctorService.createDoctorAndSchedule(createDto));
    }

    @Test
    void shouldThrowIfScheduleAlreadyExists() {
        when(userRepository.existsByEmail(createDto.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleEnum(RoleEnum.DOCTOR)).thenReturn(Optional.of(doctorRole));
        when(specialtyRepository.findById(createDto.getSpecialtyId())).thenReturn(Optional.of(specialty));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(doctorRepository.save(any())).thenReturn(doctor);
        when(doctorScheduleRepository.existsByDoctorIdAndDayOfWeek(any(), eq(DayOfWeek.MONDAY))).thenReturn(true);

        assertThrows(InvalidDataException.class,
                () -> doctorService.createDoctorAndSchedule(createDto));
    }

    @Test
    void shouldThrowIfStartTimeAfter22() {
        createDto.getSchedules().get(0).setStartTime(LocalTime.of(23, 0));

        when(userRepository.existsByEmail(createDto.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleEnum(RoleEnum.DOCTOR)).thenReturn(Optional.of(doctorRole));
        when(specialtyRepository.findById(createDto.getSpecialtyId())).thenReturn(Optional.of(specialty));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(doctorRepository.save(any())).thenReturn(doctor);
        when(doctorScheduleRepository.existsByDoctorIdAndDayOfWeek(any(), any())).thenReturn(false);

        assertThrows(InvalidDataException.class,
                () -> doctorService.createDoctorAndSchedule(createDto));
    }

    @Test
    void shouldThrowIfEndTimeBeforeStartTime() {
        createDto.getSchedules().get(0).setStartTime(LocalTime.of(15, 0));
        createDto.getSchedules().get(0).setEndTime(LocalTime.of(10, 0));

        when(userRepository.existsByEmail(createDto.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleEnum(RoleEnum.DOCTOR)).thenReturn(Optional.of(doctorRole));
        when(specialtyRepository.findById(createDto.getSpecialtyId())).thenReturn(Optional.of(specialty));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(doctorRepository.save(any())).thenReturn(doctor);
        when(doctorScheduleRepository.existsByDoctorIdAndDayOfWeek(any(), any())).thenReturn(false);

        assertThrows(InvalidDataException.class,
                () -> doctorService.createDoctorAndSchedule(createDto));
    }

    @Test
    void testUpdateDoctorAndSchedule() {
        UpdateDoctorAndScheduleDto.DoctorScheduleDto newScheduleDto = new UpdateDoctorAndScheduleDto.DoctorScheduleDto();
        newScheduleDto.setDayOfWeek(DayOfWeek.SATURDAY);
        newScheduleDto.setStartTime(LocalTime.of(10, 0));
        newScheduleDto.setEndTime(LocalTime.of(18, 0));

        UpdateDoctorAndScheduleDto updateDto = UpdateDoctorAndScheduleDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .newPassword("newpass123")
                .phoneNumber("5551234567")
                .specialtyId(2L)
                .schedules(List.of(newScheduleDto))
                .build();

        Specialty newSpecialty = Specialty.builder().id(2L).name("Neurology").build();

        DoctorSchedule existingSchedule = DoctorSchedule.builder()
                .id(1L)
                .doctor(doctor)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build();

        DoctorResponseDto expectedResponse = DoctorResponseDto.builder()
                .doctorId(1L)
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("5551234567")
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(specialtyRepository.findById(2L)).thenReturn(Optional.of(newSpecialty));
        when(passwordEncoder.encode("newpass123")).thenReturn("encodedNewPass");
        when(doctorScheduleRepository.findByDoctorId(1L)).thenReturn(List.of(existingSchedule));
        when(doctorMapper.toResponseDto(any(Doctor.class))).thenReturn(expectedResponse);

        DoctorResponseDto response = doctorService.updateDoctorAndSchedule(1L, updateDto);

        assertEquals("Jane", doctor.getUser().getFirstName());
        assertEquals("Smith", doctor.getUser().getLastName());
        assertEquals("encodedNewPass", doctor.getUser().getPassword());
        assertEquals("5551234567", doctor.getPhoneNumber());
        assertEquals(newSpecialty, doctor.getSpecialty());
        assertEquals(expectedResponse.getFirstName(), response.getFirstName());

        verify(doctorRepository).save(doctor);
        verify(doctorScheduleRepository).save(any(DoctorSchedule.class));
        verify(doctorScheduleRepository).delete(existingSchedule);
    }

    @Test
    void testUpdateDoctorAndSchedule_invalidStartTimeAfter22() {
        UpdateDoctorAndScheduleDto.DoctorScheduleDto scheduleDto = new UpdateDoctorAndScheduleDto.DoctorScheduleDto();
        scheduleDto.setDayOfWeek(DayOfWeek.TUESDAY);
        scheduleDto.setStartTime(LocalTime.of(22, 30));
        scheduleDto.setEndTime(LocalTime.of(23, 0));

        UpdateDoctorAndScheduleDto dto = UpdateDoctorAndScheduleDto.builder()
                .schedules(List.of(scheduleDto))
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findByDoctorId(1L)).thenReturn(Collections.emptyList());

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> doctorService.updateDoctorAndSchedule(1L, dto));
        assertEquals("Start time must be before 22:00", ex.getMessage());
    }

    @Test
    void testUpdateDoctorAndSchedule_endTimeBeforeStartTime() {
        UpdateDoctorAndScheduleDto.DoctorScheduleDto scheduleDto = new UpdateDoctorAndScheduleDto.DoctorScheduleDto();
        scheduleDto.setDayOfWeek(DayOfWeek.TUESDAY);
        scheduleDto.setStartTime(LocalTime.of(14, 0));
        scheduleDto.setEndTime(LocalTime.of(12, 0));

        UpdateDoctorAndScheduleDto dto = UpdateDoctorAndScheduleDto.builder()
                .schedules(List.of(scheduleDto))
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findByDoctorId(1L)).thenReturn(Collections.emptyList());

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> doctorService.updateDoctorAndSchedule(1L, dto));
        assertEquals("End time must be after start time", ex.getMessage());
    }

    @Test
    void testUpdateDoctorAndSchedule_doctorNotFound() {
        UpdateDoctorAndScheduleDto dto = UpdateDoctorAndScheduleDto.builder().build();
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> doctorService.updateDoctorAndSchedule(99L, dto));
    }

    @Test
    void testUpdateDoctorAndSchedule_specialtyNotFound() {
        UpdateDoctorAndScheduleDto dto = UpdateDoctorAndScheduleDto.builder()
                .specialtyId(88L)
                .schedules(Collections.emptyList())
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(specialtyRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> doctorService.updateDoctorAndSchedule(1L, dto));
    }

    @Test
    void shouldDeactivateDoctorSuccessfully() {
        // Dado que no hay citas futuras
        doctor.setAppointments(new ArrayList<>());

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

        doctorService.deactivateDoctor(1L);

        assertFalse(doctor.isActive());
        assertFalse(user.isActive());
        verify(doctorRepository).save(doctor);
        verify(tokenService).revokeAllUserTokens(user);
    }

    @Test
    void shouldThrowIfDoctorNotFound() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> doctorService.deactivateDoctor(1L));

        verify(doctorRepository, never()).save(any());
    }

    @Test
    void shouldThrowIfDoctorHasFutureAppointments() {
        Appointment futureAppointment = Appointment.builder()
                .date(LocalDate.now().plusDays(1)) // cita futura
                .build();

        doctor.setAppointments(List.of(futureAppointment));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

        assertThrows(InvalidDataException.class, () -> doctorService.deactivateDoctor(1L));

        verify(doctorRepository, never()).save(any());
    }
}