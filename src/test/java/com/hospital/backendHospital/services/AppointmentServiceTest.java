package com.hospital.backendHospital.services;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.AppointmentMapper;
import com.hospital.backendHospital.models.dto.appointment.AppointmentResponseDto;
import com.hospital.backendHospital.models.dto.appointment.CreateAppointmentDto;
import com.hospital.backendHospital.models.dto.appointment.RescheduleAppointmentDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.AppointmentRepository;
import com.hospital.backendHospital.repositories.DoctorRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.services.impl.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** A2, M2, C4, C6 y reprogramacion. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AppointmentMapper appointmentMapper;

    @InjectMocks private AppointmentService service;

    private Doctor doctor;
    private Patient patient;
    private User patientUser;
    private final LocalDate future = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() {
        doctor = TestData.doctor(1);
        patient = TestData.patient(2);
        patientUser = patient.getUser();
        when(patientRepository.findByUser(patientUser)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(doctor));
        when(appointmentMapper.toResponseDto(any())).thenReturn(new AppointmentResponseDto());
    }

    private CreateAppointmentDto dto(LocalDate date, String start, String end) {
        return CreateAppointmentDto.builder().doctorId(1L).date(date)
                .startTime(LocalTime.parse(start)).endTime(LocalTime.parse(end)).build();
    }

    @Test
    void patientBooksAValidAppointment() {
        service.createAppointment(patientUser, dto(future, "10:00", "10:30"));

        verify(appointmentRepository).save(argThat(a -> a.getStatus() == AppointmentStatus.PENDING
                && a.getPatient() == patient && a.getTotal().intValue() == 500));
    }

    @Test
    void startMustBeBeforeEnd() {
        assertThatThrownBy(() -> service.createAppointment(patientUser, dto(future, "11:00", "10:30")))
                .isInstanceOf(InvalidDataException.class).hasMessage("La hora de inicio debe ser anterior a la hora de fin");
    }

    @Test
    void durationMustBeBetween15And60Minutes() {
        assertThatThrownBy(() -> service.createAppointment(patientUser, dto(future, "10:00", "10:10")))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("al menos 15");
        assertThatThrownBy(() -> service.createAppointment(patientUser, dto(future, "10:00", "11:30")))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("más de 60");
    }

    @Test
    void pastDatesAreRejected() {
        assertThatThrownBy(() -> service.createAppointment(patientUser, dto(LocalDate.now().minusDays(1), "10:00", "10:30")))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("pasado");
    }

    @Test
    void todayRequiresAFutureTime() {
        LocalTime past = LocalTime.now().minusHours(1).withSecond(0).withNano(0);
        if (past.isAfter(LocalTime.now())) {
            return; // justo despues de medianoche no hay hora pasada que probar
        }
        assertThatThrownBy(() -> service.createAppointment(patientUser,
                dto(LocalDate.now(), past.toString(), past.plusMinutes(30).toString())))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void inactivePatientCannotBook() {
        patient.setActive(false);
        assertThatThrownBy(() -> service.createAppointment(patientUser, dto(future, "10:00", "10:30")))
                .isInstanceOf(InvalidDataException.class).hasMessage("El paciente está inactivo");
    }

    @Test
    void inactiveDoctorOrSpecialtyCannotBeBooked() {
        doctor.setActive(false);
        assertThatThrownBy(() -> service.createAppointment(patientUser, dto(future, "10:00", "10:30")))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("inactivo");
        doctor.setActive(true);
        doctor.getSpecialty().setActive(false);
        assertThatThrownBy(() -> service.createAppointment(patientUser, dto(future, "10:00", "10:30")))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("no está disponible");
    }

    @Test
    void appointmentMustBeInsideTheDoctorSchedule() {
        assertThatThrownBy(() -> service.createAppointment(patientUser, dto(future, "07:30", "08:00")))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("fuera del horario");
    }

    @Test
    void onlyOnePendingAppointmentWithTheSameDoctor() {
        when(appointmentRepository.existsByPatientIdAndDoctorIdAndStatusAndDateGreaterThanEqual(
                eq(2L), eq(1L), eq(AppointmentStatus.PENDING), any())).thenReturn(true);
        assertThatThrownBy(() -> service.createAppointment(patientUser, dto(future, "10:00", "10:30")))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("pendiente con este doctor");
    }

    @Test
    void overlappingAppointmentsAreRejected() {
        when(appointmentRepository.existsOverlappingAppointmentForDoctor(eq(1L), any(), any(), any(), anyLong())).thenReturn(true);
        assertThatThrownBy(() -> service.createAppointment(patientUser, dto(future, "10:00", "10:30")))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("ya tiene una cita");
    }

    @Test
    void receptionistMustIndicateThePatient() {
        User receptionist = TestData.user(9, RoleEnum.RECEPTIONIST);
        assertThatThrownBy(() -> service.createAppointment(receptionist, dto(future, "10:00", "10:30")))
                .isInstanceOf(InvalidDataException.class).hasMessage("El paciente es obligatorio");
    }

    @Test
    void doctorCompletesOnlyOwnPastOrTodayAppointments() {
        Appointment today = TestData.appointment(5, doctor, patient, LocalDate.now(), AppointmentStatus.PENDING);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(today));

        assertThatThrownBy(() -> service.completeAppointment(TestData.user(999, RoleEnum.DOCTOR), 5L))
                .isInstanceOf(AccessDeniedException.class);
        service.completeAppointment(doctor.getUser(), 5L);
        assertThat(today.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThatThrownBy(() -> service.completeAppointment(doctor.getUser(), 5L))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("completada");
    }

    @Test
    void futureAppointmentsCannotBeCompleted() {
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(TestData.appointment(5, doctor, patient, future, AppointmentStatus.PENDING)));
        assertThatThrownBy(() -> service.completeAppointment(doctor.getUser(), 5L))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("futura");
    }

    @Test
    void patientCancelsOnlyOwnAppointments() {
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(TestData.appointment(5, doctor, patient, future, AppointmentStatus.PENDING)));
        assertThatThrownBy(() -> service.cancelAppointment(TestData.user(777, RoleEnum.PATIENT), 5L))
                .isInstanceOf(AccessDeniedException.class);
        service.cancelAppointment(patientUser, 5L);
        verify(appointmentRepository).save(argThat(a -> a.getStatus() == AppointmentStatus.CANCELLED));
    }

    @Test
    void pastAppointmentsCannotBeCancelled() {
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(
                TestData.appointment(5, doctor, patient, LocalDate.now().minusDays(1), AppointmentStatus.PENDING)));
        assertThatThrownBy(() -> service.cancelAppointment(patientUser, 5L))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("pasadas");
    }

    @Test
    void rescheduleExcludesTheSameAppointmentFromOverlap() {
        Appointment appointment = TestData.appointment(5, doctor, patient, future, AppointmentStatus.PENDING);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        RescheduleAppointmentDto dto = new RescheduleAppointmentDto(future.plusDays(1), LocalTime.of(12, 0), LocalTime.of(12, 30));

        service.rescheduleAppointment(patientUser, 5L, dto);

        verify(appointmentRepository).existsOverlappingAppointmentForDoctor(1L, future.plusDays(1), LocalTime.of(12, 0), LocalTime.of(12, 30), 5L);
        assertThat(appointment.getDate()).isEqualTo(future.plusDays(1));
        assertThat(appointment.getStartTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void onlyPendingAppointmentsCanBeRescheduled() {
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(TestData.appointment(5, doctor, patient, future, AppointmentStatus.CANCELLED)));
        assertThatThrownBy(() -> service.rescheduleAppointment(patientUser, 5L,
                new RescheduleAppointmentDto(future, LocalTime.of(12, 0), LocalTime.of(12, 30))))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void nextAppointmentIsEmptyWhenThereIsNone() {
        when(appointmentRepository.findFirstByPatientIdAndStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(eq(2L), any(), any()))
                .thenReturn(Optional.empty());
        assertThat(service.getNextAppointment(patientUser)).isEmpty();
    }

    @Test
    void patientCannotSeeAnotherPatientsAppointment() {
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(TestData.appointment(5, doctor, patient, future, AppointmentStatus.PENDING)));
        assertThatThrownBy(() -> service.getAppointment(TestData.user(777, RoleEnum.PATIENT), 5L))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(service.getAppointment(patientUser, 5L)).isNotNull();
    }

    @Test
    void patientWithoutProfileGets404() {
        User noProfile = TestData.user(555, RoleEnum.PATIENT);
        when(patientRepository.findByUser(noProfile)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getNextAppointment(noProfile)).isInstanceOf(EntityNotFoundException.class);
    }
}
