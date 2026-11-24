package com.hospital.backendHospital.services;

import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.HospitalizationMapper;
import com.hospital.backendHospital.models.dto.hospitalization.CreateHospitalizationDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.DoctorRepository;
import com.hospital.backendHospital.repositories.HospitalizationRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.repositories.RoomRepository;
import com.hospital.backendHospital.services.impl.HospitalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** A6: capacidad de la sala, estados, alta doble y costo. C1: doctor responsable. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HospitalizationServiceTest {

    @Mock private HospitalizationRepository hospitalizationRepository;
    @Mock private HospitalizationMapper hospitalizationMapper;
    @Mock private PatientRepository patientRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private DoctorRepository doctorRepository;

    @InjectMocks private HospitalizationService service;

    private Patient patient;
    private Doctor doctor;
    private Room room;
    private final User receptionist = TestData.user(9, RoleEnum.RECEPTIONIST);

    @BeforeEach
    void setUp() {
        patient = TestData.patient(2);
        doctor = TestData.doctor(1);
        room = TestData.room(3, 2);
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(roomRepository.findById(3L)).thenReturn(Optional.of(room));
    }

    private CreateHospitalizationDto dto(Long doctorId) {
        return CreateHospitalizationDto.builder().patientId(2L).doctorId(doctorId).roomId(3L)
                .reason("Observacion").costPerDay(BigDecimal.valueOf(1000)).build();
    }

    @Test
    void roomStaysAvailableUntilItIsFull() {
        when(hospitalizationRepository.countByRoomIdAndActiveTrue(3L)).thenReturn(0L);
        service.createHospitalization(receptionist, dto(1L));
        assertThat(room.isAvailable()).isTrue();

        when(hospitalizationRepository.countByRoomIdAndActiveTrue(3L)).thenReturn(1L);
        service.createHospitalization(receptionist, dto(1L));
        assertThat(room.isAvailable()).isFalse();
    }

    @Test
    void fullRoomIsRejected() {
        when(hospitalizationRepository.countByRoomIdAndActiveTrue(3L)).thenReturn(2L);
        assertThatThrownBy(() -> service.createHospitalization(receptionist, dto(1L)))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("camas libres");
    }

    @Test
    void inactivePatientOrDoctorIsRejected() {
        patient.setActive(false);
        assertThatThrownBy(() -> service.createHospitalization(receptionist, dto(1L))).hasMessage("El paciente está inactivo");
        patient.setActive(true);
        doctor.setActive(false);
        assertThatThrownBy(() -> service.createHospitalization(receptionist, dto(1L))).hasMessage("El doctor está inactivo");
    }

    @Test
    void alreadyHospitalizedPatientIsRejected() {
        when(hospitalizationRepository.existsByPatientAndActiveTrue(patient)).thenReturn(true);
        assertThatThrownBy(() -> service.createHospitalization(receptionist, dto(1L))).hasMessage("El paciente ya está hospitalizado");
    }

    @Test
    void receptionistMustIndicateDoctorButDoctorDefaultsToHimself() {
        assertThatThrownBy(() -> service.createHospitalization(receptionist, dto(null)))
                .hasMessage("El doctor responsable es obligatorio");

        when(doctorRepository.findByUser(doctor.getUser())).thenReturn(Optional.of(doctor));
        service.createHospitalization(doctor.getUser(), dto(null));
        verify(hospitalizationRepository).save(org.mockito.ArgumentMatchers.argThat(h -> h.getDoctor() == doctor
                && h.getCostPerDay().intValue() == 1000));
    }

    @Test
    void dischargeComputesTotalAndCannotRepeat() {
        Hospitalization hospitalization = Hospitalization.builder().id(7L).patient(patient).doctor(doctor).room(room)
                .admissionDate(LocalDate.now().minusDays(3)).reason("x").costPerDay(BigDecimal.valueOf(1000)).build();
        room.setAvailable(false);
        when(hospitalizationRepository.findById(7L)).thenReturn(Optional.of(hospitalization));

        service.discharge(7L);

        assertThat(hospitalization.isActive()).isFalse();
        assertThat(hospitalization.getTotalCost()).isEqualByComparingTo("3000");
        assertThat(room.isAvailable()).isTrue();
        assertThatThrownBy(() -> service.discharge(7L)).isInstanceOf(InvalidDataException.class).hasMessageContaining("ya fue dado de alta");
    }

    @Test
    void sameDayDischargeChargesOneDay() {
        Hospitalization hospitalization = Hospitalization.builder().id(8L).patient(patient).doctor(doctor).room(room)
                .admissionDate(LocalDate.now()).reason("x").costPerDay(BigDecimal.valueOf(700)).build();
        when(hospitalizationRepository.findById(8L)).thenReturn(Optional.of(hospitalization));

        service.discharge(8L);

        assertThat(hospitalization.getTotalCost()).isEqualByComparingTo("700");
        verify(hospitalizationRepository).save(any());
    }
}
