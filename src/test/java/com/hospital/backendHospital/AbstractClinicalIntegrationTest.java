package com.hospital.backendHospital;

import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Base para pruebas de integracion que necesitan datos clinicos reales en MySQL
 * (especialidades, doctores con horario, pacientes, salas, citas, hospitalizaciones...).
 * Cada metodo crea registros nuevos con nombres unicos, asi las pruebas no dependen entre si.
 */
public abstract class AbstractClinicalIntegrationTest extends AbstractIntegrationTest {

    @Autowired protected SpecialtyRepository specialtyRepository;
    @Autowired protected DoctorRepository doctorRepository;
    @Autowired protected DoctorScheduleRepository doctorScheduleRepository;
    @Autowired protected PatientRepository patientRepository;
    @Autowired protected RoomRepository roomRepository;
    @Autowired protected AppointmentRepository appointmentRepository;
    @Autowired protected HospitalizationRepository hospitalizationRepository;
    @Autowired protected MedicalRecordRepository medicalRecordRepository;
    @Autowired protected MedicalSupplyRepository medicalSupplyRepository;
    @Autowired protected TransactionTemplate transactionTemplate;

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Ejecuta dentro de una transaccion (para leer relaciones LAZY desde la prueba). */
    protected <T> T inTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    protected Specialty createSpecialty() {
        return specialtyRepository.save(Specialty.builder()
                .name(unique("Especialidad"))
                .description("Especialidad de prueba")
                .cost(BigDecimal.valueOf(500))
                .doctor(new ArrayList<>())
                .build());
    }

    /** Doctor activo con su usuario (rol DOCTOR) y horario de 08:00 a 20:00 todos los dias. */
    protected Doctor createDoctor() {
        Role role = roleRepository.findByRoleEnum(RoleEnum.DOCTOR).orElseThrow();
        User user = User.builder()
                .email(unique("doctor") + "@test.com")
                .firstName("Doctor")
                .lastName(unique("Prueba"))
                .password(passwordEncoder.encode(PASSWORD))
                .roles(new java.util.HashSet<>(java.util.Set.of(role)))
                .build();

        Doctor doctor = doctorRepository.save(Doctor.builder()
                .user(user)
                .phoneNumber("5512345678")
                .specialty(createSpecialty())
                .registerDate(LocalDate.now())
                .appointments(new ArrayList<>())
                .schedules(new ArrayList<>())
                .build());

        for (DayOfWeek day : DayOfWeek.values()) {
            doctorScheduleRepository.save(DoctorSchedule.builder()
                    .doctor(doctor)
                    .dayOfWeek(day)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(20, 0))
                    .build());
        }
        return doctor;
    }

    /** Paciente activo con perfil completo y su usuario (rol PATIENT). */
    protected Patient createPatient() {
        User user = createUser(RoleEnum.PATIENT);
        return patientRepository.save(Patient.builder()
                .user(user)
                .phoneNumber("5587654321")
                .address("Calle de prueba 123")
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .gender("F")
                .allergies("Ninguna")
                .bloodType("O+")
                .chronicDiseases("Ninguna")
                .currentMedications("Ninguno")
                .emergencyContact("5500000000")
                .emergencyContactName("Contacto")
                .emergencyContactRelation("Familiar")
                .height(1.65)
                .weight(60.0)
                .registerDate(LocalDate.now())
                .smoker(false)
                .appointments(new ArrayList<>())
                .build());
    }

    protected Room createRoom() {
        return roomRepository.save(Room.builder()
                .name(unique("Sala"))
                .capacity(2)
                .build());
    }

    protected Appointment createAppointment(Doctor doctor, Patient patient, LocalDate date,
                                            LocalTime start, LocalTime end, AppointmentStatus status) {
        return appointmentRepository.save(Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .date(date)
                .startTime(start)
                .endTime(end)
                .status(status)
                .total(BigDecimal.valueOf(500))
                .build());
    }

    protected Hospitalization createHospitalization(Patient patient, Doctor doctor, Room room) {
        return hospitalizationRepository.save(Hospitalization.builder()
                .patient(patient)
                .doctor(doctor)
                .room(room)
                .admissionDate(LocalDate.now())
                .reason("Observacion")
                .build());
    }

    protected MedicalRecord createMedicalRecord(Patient patient, String diagnostics) {
        return medicalRecordRepository.save(MedicalRecord.builder()
                .patient(patient)
                .date(LocalDate.now())
                .diagnostics(diagnostics)
                .treatment("Reposo")
                .build());
    }

    protected MedicalSupply createSupply(String name) {
        return medicalSupplyRepository.save(MedicalSupply.builder()
                .name(name)
                .quantity(100)
                .minimumStock(10)
                .unity(UnityEnum.BOX)
                .build());
    }
}
