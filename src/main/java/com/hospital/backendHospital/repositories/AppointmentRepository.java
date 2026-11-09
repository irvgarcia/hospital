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

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {
    Page<Appointment> findAllByPatientId(Long id, Pageable pageable);

    Appointment findByPatientIdAndStatus(Long id, AppointmentStatus status);

    /** El doctor ya tiene una cita (no cancelada) que se cruza con ese horario. */
    @Query("""
    SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
    FROM Appointment a
    WHERE a.doctor.id = :doctorId
      AND a.date = :date
      AND a.status != com.hospital.backendHospital.models.entity.AppointmentStatus.CANCELLED
      AND a.startTime < :endTime AND a.endTime > :startTime
""")
    boolean existsOverlappingAppointmentForDoctor(@Param("doctorId") Long doctorId,
                                                  @Param("date") LocalDate date,
                                                  @Param("startTime") LocalTime startTime,
                                                  @Param("endTime") LocalTime endTime);

    /** El paciente ya tiene una cita (no cancelada, con cualquier doctor) que se cruza con ese horario. */
    @Query("""
    SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
    FROM Appointment a
    WHERE a.patient.id = :patientId
      AND a.date = :date
      AND a.status != com.hospital.backendHospital.models.entity.AppointmentStatus.CANCELLED
      AND a.startTime < :endTime AND a.endTime > :startTime
""")
    boolean existsOverlappingAppointmentForPatient(@Param("patientId") Long patientId,
                                                   @Param("date") LocalDate date,
                                                   @Param("startTime") LocalTime startTime,
                                                   @Param("endTime") LocalTime endTime);

}
