package com.hospital.backendHospital.repositories;

import com.hospital.backendHospital.models.entity.Hospitalization;
import com.hospital.backendHospital.models.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface HospitalizationRepository extends JpaRepository<Hospitalization, Long>, JpaSpecificationExecutor<Hospitalization> {

    boolean existsByPatientAndActiveTrue(Patient patient);

    boolean existsByPatientIdAndActiveTrue(Long patientId);

    boolean existsByDoctorIdAndActiveTrue(Long doctorId);

    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);

    /** Camas ocupadas de una sala (A6). */
    long countByRoomIdAndActiveTrue(Long roomId);

    long countByActiveTrue();

    List<Hospitalization> findByPatientIdOrderByAdmissionDateDesc(Long patientId);
}
