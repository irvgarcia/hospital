package com.hospital.backendHospital.repositories;

import com.hospital.backendHospital.models.entity.Patient;
import com.hospital.backendHospital.models.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    Optional<Patient> findByUser(User user);

    Optional<Patient> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    long countByActiveTrue();
}
