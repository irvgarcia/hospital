package com.hospital.backendHospital.repositories;

import com.hospital.backendHospital.models.entity.Appointment;
import com.hospital.backendHospital.models.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    Page<Appointment> findAllByPatientId(Long id, Pageable pageable);

    /** Proxima cita pendiente del paciente (A2: puede tener varias, con doctores distintos). */
    Optional<Appointment> findFirstByPatientIdAndStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            Long patientId, AppointmentStatus status, LocalDate date);

    /** A5: citas que bloquean la desactivacion (pendientes de hoy en adelante). */
    boolean existsByDoctorIdAndStatusAndDateGreaterThanEqual(Long doctorId, AppointmentStatus status, LocalDate date);

    boolean existsByPatientIdAndStatusAndDateGreaterThanEqual(Long patientId, AppointmentStatus status, LocalDate date);

    /** A2: un paciente no puede tener dos citas pendientes con el mismo doctor. */
    boolean existsByPatientIdAndDoctorIdAndStatusAndDateGreaterThanEqual(
            Long patientId, Long doctorId, AppointmentStatus status, LocalDate date);

    /** El doctor atendio o va a atender a este paciente (acceso a sus archivos). */
    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);

    /** A4: citas pendientes futuras de un doctor, para validar cambios en sus horarios. */
    List<Appointment> findByDoctorIdAndStatusAndDateGreaterThanEqual(Long doctorId, AppointmentStatus status, LocalDate date);

    List<Appointment> findTop20ByPatientIdOrderByDateDescStartTimeDesc(Long patientId);

    long countByDateAndStatusNot(LocalDate date, AppointmentStatus status);

    long countByStatusAndDateGreaterThanEqual(AppointmentStatus status, LocalDate date);

    /** Citas no canceladas de un doctor en un dia (para calcular los horarios libres). */
    @Query("""
    SELECT a FROM Appointment a
    WHERE a.doctor.id = :doctorId
      AND a.date = :date
      AND a.status != com.hospital.backendHospital.models.entity.AppointmentStatus.CANCELLED
    ORDER BY a.startTime
""")
    List<Appointment> findActiveByDoctorAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    /** El doctor ya tiene una cita (no cancelada) que se cruza con ese horario. excludeId: la cita que se reprograma. */
    @Query("""
    SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
    FROM Appointment a
    WHERE a.doctor.id = :doctorId
      AND a.date = :date
      AND a.status != com.hospital.backendHospital.models.entity.AppointmentStatus.CANCELLED
      AND a.startTime < :endTime AND a.endTime > :startTime
      AND a.id <> :excludeId
""")
    boolean existsOverlappingAppointmentForDoctor(@Param("doctorId") Long doctorId,
                                                  @Param("date") LocalDate date,
                                                  @Param("startTime") LocalTime startTime,
                                                  @Param("endTime") LocalTime endTime,
                                                  @Param("excludeId") Long excludeId);

    /** El paciente ya tiene una cita (no cancelada, con cualquier doctor) que se cruza con ese horario. */
    @Query("""
    SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
    FROM Appointment a
    WHERE a.patient.id = :patientId
      AND a.date = :date
      AND a.status != com.hospital.backendHospital.models.entity.AppointmentStatus.CANCELLED
      AND a.startTime < :endTime AND a.endTime > :startTime
      AND a.id <> :excludeId
""")
    boolean existsOverlappingAppointmentForPatient(@Param("patientId") Long patientId,
                                                   @Param("date") LocalDate date,
                                                   @Param("startTime") LocalTime startTime,
                                                   @Param("endTime") LocalTime endTime,
                                                   @Param("excludeId") Long excludeId);
}
