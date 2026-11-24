package com.hospital.backendHospital.services;

import com.hospital.backendHospital.auth.service.TokenService;
import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.DoctorMapper;
import com.hospital.backendHospital.models.dto.doctor.AvailableSlotDto;
import com.hospital.backendHospital.models.dto.doctor.CreateDoctorDto;
import com.hospital.backendHospital.models.dto.doctor.UpdateDoctorDto;
import com.hospital.backendHospital.models.dto.doctorSchedule.ScheduleSlotDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.*;
import com.hospital.backendHospital.services.impl.DoctorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** A4 (actualizacion de horarios), A5 (desactivacion), disponibilidad y alta. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DoctorServiceTest {

    @Mock private DoctorRepository doctorRepository;
    @Mock private DoctorScheduleRepository doctorScheduleRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private HospitalizationRepository hospitalizationRepository;
    @Mock private UserRepository userRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private DoctorMapper doctorMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;

    @InjectMocks private DoctorService service;

    private Doctor doctor;

    @BeforeEach
    void setUp() {
        doctor = TestData.doctor(1);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findByDoctorId(1L)).thenReturn(List.copyOf(doctor.getSchedules()));
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(TestData.specialty()));
        when(roleRepository.findByRoleEnum(RoleEnum.DOCTOR)).thenReturn(Optional.of(Role.builder().roleEnum(RoleEnum.DOCTOR).build()));
    }

    private static ScheduleSlotDto slot(DayOfWeek day, int from, int to) {
        return new ScheduleSlotDto(day, LocalTime.of(from, 0), LocalTime.of(to, 0));
    }

    @Test
    void updateWithoutSchedulesKeepsThem() {
        service.updateDoctor(1L, UpdateDoctorDto.builder().firstName("Nuevo").build());

        assertThat(doctor.getUser().getFirstName()).isEqualTo("Nuevo");
        verify(doctorScheduleRepository, never()).delete(any(DoctorSchedule.class));
        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void repeatedDaysReturn400() {
        UpdateDoctorDto dto = UpdateDoctorDto.builder()
                .schedules(List.of(slot(DayOfWeek.MONDAY, 8, 12), slot(DayOfWeek.MONDAY, 13, 18))).build();
        assertThatThrownBy(() -> service.updateDoctor(1L, dto))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("repetido");
    }

    @Test
    void invalidScheduleIsRejectedOnUpdate() {
        UpdateDoctorDto dto = UpdateDoctorDto.builder().schedules(List.of(slot(DayOfWeek.MONDAY, 14, 9))).build();
        assertThatThrownBy(() -> service.updateDoctor(1L, dto)).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void cannotRemoveADayWithPendingAppointments() {
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        when(appointmentRepository.findByDoctorIdAndStatusAndDateGreaterThanEqual(eq(1L), eq(AppointmentStatus.PENDING), any()))
                .thenReturn(List.of(TestData.appointment(9, doctor, TestData.patient(2), nextMonday, AppointmentStatus.PENDING)));
        UpdateDoctorDto dto = UpdateDoctorDto.builder().schedules(List.of(slot(DayOfWeek.TUESDAY, 8, 14))).build();

        assertThatThrownBy(() -> service.updateDoctor(1L, dto))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("deja fuera la cita");
    }

    @Test
    void schedulesAreReplaced() {
        UpdateDoctorDto dto = UpdateDoctorDto.builder()
                .schedules(List.of(slot(DayOfWeek.MONDAY, 9, 13), slot(DayOfWeek.TUESDAY, 9, 13))).build();

        service.updateDoctor(1L, dto);

        verify(doctorScheduleRepository, times(5)).delete(any(DoctorSchedule.class));
        assertThat(doctor.getSchedules()).hasSize(2)
                .allMatch(s -> s.getStartTime().equals(LocalTime.of(9, 0)) && s.getEndTime().equals(LocalTime.of(13, 0)));
    }

    @Test
    void emailChangeChecksUniquenessAndRevokesTokens() {
        when(userRepository.existsByEmailAndIdNot("otro@test.com", doctor.getUser().getId())).thenReturn(true);
        assertThatThrownBy(() -> service.updateDoctor(1L, UpdateDoctorDto.builder().email("otro@test.com").build()))
                .isInstanceOf(ConflictException.class);

        service.updateDoctor(1L, UpdateDoctorDto.builder().email("Nuevo@Test.com").build());
        assertThat(doctor.getUser().getEmail()).isEqualTo("nuevo@test.com");
        verify(tokenService).revokeAllUserTokens(doctor.getUser());
    }

    @Test
    void deactivationIsBlockedOnlyByPendingAppointmentsOrActiveHospitalizations() {
        when(appointmentRepository.existsByDoctorIdAndStatusAndDateGreaterThanEqual(1L, AppointmentStatus.PENDING, LocalDate.now()))
                .thenReturn(true);
        assertThatThrownBy(() -> service.deactivateDoctor(1L)).isInstanceOf(InvalidDataException.class).hasMessageContaining("citas pendientes");

        when(appointmentRepository.existsByDoctorIdAndStatusAndDateGreaterThanEqual(1L, AppointmentStatus.PENDING, LocalDate.now()))
                .thenReturn(false);
        when(hospitalizationRepository.existsByDoctorIdAndActiveTrue(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.deactivateDoctor(1L)).isInstanceOf(InvalidDataException.class).hasMessageContaining("hospitalizaciones");

        when(hospitalizationRepository.existsByDoctorIdAndActiveTrue(1L)).thenReturn(false);
        service.deactivateDoctor(1L);
        assertThat(doctor.isActive()).isFalse();
        assertThat(doctor.getUser().isActive()).isFalse();
        verify(tokenService).revokeAllUserTokens(doctor.getUser());
    }

    @Test
    void createRejectsDuplicateEmailAndInvalidSchedules() {
        CreateDoctorDto dto = CreateDoctorDto.builder().email("doc@test.com").firstName("Ana").lastName("Gil").password("12345678")
                .specialtyId(1L).phoneNumber("5512345678").schedules(List.of(slot(DayOfWeek.MONDAY, 8, 14))).build();
        when(userRepository.existsByEmail("doc@test.com")).thenReturn(true);
        assertThatThrownBy(() -> service.createDoctor(dto)).isInstanceOf(ConflictException.class);

        when(userRepository.existsByEmail("doc@test.com")).thenReturn(false);
        dto.setSchedules(List.of(slot(DayOfWeek.MONDAY, 8, 14), slot(DayOfWeek.MONDAY, 15, 18)));
        assertThatThrownBy(() -> service.createDoctor(dto)).isInstanceOf(InvalidDataException.class);

        dto.setSchedules(List.of(slot(DayOfWeek.MONDAY, 8, 14)));
        service.createDoctor(dto);
        verify(doctorRepository).save(any());
        verify(doctorScheduleRepository).save(any());
    }

    @Test
    void availabilityExcludesTakenSlots() {
        LocalDate date = LocalDate.now().plusDays(2);
        Appointment taken = TestData.appointment(9, doctor, TestData.patient(2), date, AppointmentStatus.PENDING);
        when(appointmentRepository.findActiveByDoctorAndDate(1L, date)).thenReturn(List.of(taken));

        List<AvailableSlotDto> slots = service.getAvailability(1L, date);

        // 08:00-20:00 son 24 horarios de 30 minutos; la cita de 10:00-10:30 ocupa uno
        assertThat(slots).hasSize(23);
        assertThat(slots).noneMatch(s -> s.getStartTime().equals(LocalTime.of(10, 0)));
    }

    @Test
    void availabilityIsEmptyForInactiveDoctorOrPastDate() {
        assertThat(service.getAvailability(1L, LocalDate.now().minusDays(1))).isEmpty();
        doctor.setActive(false);
        assertThat(service.getAvailability(1L, LocalDate.now().plusDays(1))).isEmpty();
    }

    @Test
    void patientsOnlySeeActiveDoctors() {
        doctor.setActive(false);
        assertThatThrownBy(() -> service.getDoctor(TestData.user(5, RoleEnum.PATIENT), 1L))
                .isInstanceOf(com.hospital.backendHospital.exceptions.EntityNotFoundException.class);
        service.getDoctor(TestData.user(6, RoleEnum.ADMIN), 1L);
        verify(doctorMapper).toResponseDto(doctor);
    }
}
