package com.hospital.backendHospital.services;

import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.MedicalRecordMapper;
import com.hospital.backendHospital.models.dto.medicalRecord.CreateMedicalRecordDto;
import com.hospital.backendHospital.models.dto.medicalRecord.UpdateMedicalRecordDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.AppointmentRepository;
import com.hospital.backendHospital.repositories.MedicalRecordRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.services.impl.MedicalRecordService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** M8: entradas del historial a partir de citas completadas del doctor autenticado. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicalRecordServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private MedicalRecordMapper medicalRecordMapper;

    @InjectMocks private MedicalRecordService service;

    private Doctor doctor;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        doctor = TestData.doctor(1);
        appointment = TestData.appointment(5, doctor, TestData.patient(2), LocalDate.now(), AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
    }

    private static CreateMedicalRecordDto dto() {
        return CreateMedicalRecordDto.builder().appointmentId(5L).diagnostics("Gripe").treatment("Reposo").notes(" ").build();
    }

    @Test
    void doctorWritesTheEntryOfHisCompletedAppointment() {
        service.createMedicalRecord(doctor.getUser(), dto());
        verify(medicalRecordRepository).save(argThat(r -> r.getDoctor() == doctor && r.getAppointment() == appointment
                && r.getPatient() == appointment.getPatient() && r.getNotes() == null));
    }

    @Test
    void otherDoctorsCannotWriteIt() {
        assertThatThrownBy(() -> service.createMedicalRecord(TestData.user(999, RoleEnum.DOCTOR), dto()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void onlyCompletedAppointments() {
        appointment.setStatus(AppointmentStatus.PENDING);
        assertThatThrownBy(() -> service.createMedicalRecord(doctor.getUser(), dto())).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void oneEntryPerAppointment() {
        when(medicalRecordRepository.existsByAppointmentId(5L)).thenReturn(true);
        assertThatThrownBy(() -> service.createMedicalRecord(doctor.getUser(), dto())).isInstanceOf(ConflictException.class);
    }

    @Test
    void onlyTheAuthorCanEdit() {
        MedicalRecord record = MedicalRecord.builder().id(9L).doctor(doctor).patient(appointment.getPatient())
                .diagnostics("a").treatment("b").date(LocalDate.now()).build();
        when(medicalRecordRepository.findById(9L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.updateMedicalRecord(TestData.user(999, RoleEnum.DOCTOR), 9L, new UpdateMedicalRecordDto("x", null, null)))
                .isInstanceOf(AccessDeniedException.class);
        service.updateMedicalRecord(doctor.getUser(), 9L, new UpdateMedicalRecordDto("Nuevo", null, "Nota"));
        assertThat(record.getDiagnostics()).isEqualTo("Nuevo");
        assertThat(record.getTreatment()).isEqualTo("b");
        assertThat(record.getNotes()).isEqualTo("Nota");
    }
}
