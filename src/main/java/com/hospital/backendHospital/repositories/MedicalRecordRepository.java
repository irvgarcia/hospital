package com.hospital.backendHospital.repositories;

import com.hospital.backendHospital.models.entity.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Page<MedicalRecord> findByPatientId(Long patientId, Pageable pageable);

    List<MedicalRecord> findByPatientIdOrderByDateDescIdDesc(Long patientId);

    boolean existsByAppointmentId(Long appointmentId);
}
