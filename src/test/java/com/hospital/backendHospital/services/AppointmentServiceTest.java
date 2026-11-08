package com.hospital.backendHospital.services;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.AppointmentMapper;
import com.hospital.backendHospital.models.dto.appointment.CreateAppointmentDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.models.filters.AppointmentFilterRequest;
import com.hospital.backendHospital.repositories.AppointmentRepository;
import com.hospital.backendHospital.repositories.DoctorRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.services.impl.AppointmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Reglas de acceso por rol dentro de AppointmentService (Fase 1). */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AppointmentMapper appointmentMapper;

    @InjectMocks
    private AppointmentService appointmentService;

    private static User userWithRole(long id, RoleEnum roleEnum) {
        return User.builder()
                .id(id)
                .email(roleEnum.name().toLowerCase() + "@test.com")
                .roles(Set.of(Role.builder().roleEnum(roleEnum).build()))
                .build();
    }

    private static CreateAppointmentDto booking(Long patientId) {
        return CreateAppointmentDto.builder()
                .patientId(patientId)
                .doctorId(1L)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .build();
    }

    private static Appointment pendingAppointmentOf(User patientUser) {
        return Appointment.builder()
                .id(10L)
                .patient(Patient.builder().id(5L).user(patientUser).build())
                .date(LocalDate.now().plusDays(1))
                .status(AppointmentStatus.PENDING)
                .build();
    }

    // ---- Agendar ----

    @Test
    void receptionistMustIndicatePatient() {
        User receptionist = userWithRole(2L, RoleEnum.RECEPTIONIST);

        InvalidDataException ex = assertThrows(InvalidDataException.class,
                () -> appointmentService.createAppointment(receptionist, booking(null)));

        assertThat(ex.getMessage()).isEqualTo("Patient is required");
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void receptionistBooksForTheIndicatedPatient() {
        User receptionist = userWithRole(2L, RoleEnum.RECEPTIONIST);
        when(patientRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> appointmentService.createAppointment(receptionist, booking(7L)));

        verify(patientRepository).findById(7L);
        verify(patientRepository, never()).findByUser(any());
    }

    @Test
    void patientAlwaysBooksForThemselvesIgnoringPatientId() {
        User patientUser = userWithRole(3L, RoleEnum.PATIENT);
        when(patientRepository.findByUser(patientUser)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> appointmentService.createAppointment(patientUser, booking(99L)));

        verify(patientRepository).findByUser(patientUser);
        verify(patientRepository, never()).findById(any());
    }

    // ---- Cancelar ----

    @Test
    void patientCannotCancelAnotherPatientsAppointment() {
        User owner = userWithRole(3L, RoleEnum.PATIENT);
        User intruder = userWithRole(4L, RoleEnum.PATIENT);
        Appointment appointment = pendingAppointmentOf(owner);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        assertThrows(AccessDeniedException.class, () -> appointmentService.cancelAppointment(intruder, 10L));

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void patientCancelsOwnAppointment() {
        User owner = userWithRole(3L, RoleEnum.PATIENT);
        Appointment appointment = pendingAppointmentOf(owner);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        appointmentService.cancelAppointment(owner, 10L);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void receptionistCancelsAnyAppointment() {
        User receptionist = userWithRole(2L, RoleEnum.RECEPTIONIST);
        Appointment appointment = pendingAppointmentOf(userWithRole(3L, RoleEnum.PATIENT));
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

        appointmentService.cancelAppointment(receptionist, 10L);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    // ---- Listar ----

    @Test
    @SuppressWarnings("unchecked")
    void doctorOnlySeesOwnAppointments() {
        User doctorUser = userWithRole(6L, RoleEnum.DOCTOR);
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(Doctor.builder().id(20L).user(doctorUser).build()));
        when(appointmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        AppointmentFilterRequest filter = AppointmentFilterRequest.builder().doctorId(99L).build();

        appointmentService.filterAppointments(doctorUser, filter, Pageable.unpaged());

        assertThat(filter.getDoctorId()).isEqualTo(20L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminFilterIsNotRestricted() {
        User admin = userWithRole(1L, RoleEnum.ADMIN);
        when(appointmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        AppointmentFilterRequest filter = AppointmentFilterRequest.builder().doctorId(99L).build();

        appointmentService.filterAppointments(admin, filter, Pageable.unpaged());

        assertThat(filter.getDoctorId()).isEqualTo(99L);
        verifyNoInteractions(doctorRepository);
    }
}
