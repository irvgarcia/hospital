package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.entity.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Objetos de prueba para las pruebas unitarias de servicios. */
final class TestData {

    private TestData() {
    }

    static User user(long id, RoleEnum role) {
        return User.builder()
                .id(id)
                .email(role.name().toLowerCase() + id + "@test.com")
                .firstName("Nombre" + id)
                .lastName("Apellido" + id)
                .password("hash")
                .roles(new HashSet<>(Set.of(Role.builder().roleEnum(role).build())))
                .build();
    }

    static Specialty specialty() {
        return Specialty.builder().id(1L).name("Cardiologia").description("Corazon").cost(BigDecimal.valueOf(500)).build();
    }

    /** Doctor activo que atiende todos los dias de 08:00 a 20:00. */
    static Doctor doctor(long id) {
        Doctor doctor = Doctor.builder()
                .id(id)
                .user(user(100 + id, RoleEnum.DOCTOR))
                .specialty(specialty())
                .phoneNumber("5512345678")
                .registerDate(LocalDate.now())
                .schedules(new ArrayList<>())
                .build();
        for (DayOfWeek day : DayOfWeek.values()) {
            doctor.getSchedules().add(DoctorSchedule.builder()
                    .id(id * 10 + day.getValue())
                    .doctor(doctor)
                    .dayOfWeek(day)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(20, 0))
                    .build());
        }
        return doctor;
    }

    static Patient patient(long id) {
        return Patient.builder()
                .id(id)
                .user(user(200 + id, RoleEnum.PATIENT))
                .phoneNumber("5587654321")
                .address("Calle 1")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.FEMALE)
                .bloodType(BloodType.O_POSITIVE)
                .allergies("Ninguna")
                .chronicDiseases("Ninguna")
                .currentMedications("Ninguno")
                .emergencyContact("5500000000")
                .emergencyContactName("Contacto")
                .emergencyContactRelation("Familiar")
                .height(1.7)
                .weight(70.0)
                .registerDate(LocalDate.now())
                .build();
    }

    static Appointment appointment(long id, Doctor doctor, Patient patient, LocalDate date, AppointmentStatus status) {
        return Appointment.builder()
                .id(id)
                .doctor(doctor)
                .patient(patient)
                .date(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .status(status)
                .total(BigDecimal.valueOf(500))
                .build();
    }

    static Room room(long id, int capacity) {
        return Room.builder().id(id).name("Sala " + id).capacity(capacity).available(true).build();
    }

    static MedicalSupply supply(long id, int quantity, int minimum) {
        return MedicalSupply.builder().id(id).name("Insumo " + id).quantity(quantity).minimumStock(minimum).unit(Unit.BOX).build();
    }
}
