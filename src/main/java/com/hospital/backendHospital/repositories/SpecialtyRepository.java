package com.hospital.backendHospital.repositories;

import com.hospital.backendHospital.models.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    Optional<Specialty> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Specialty> findAllByOrderByNameAsc();

    List<Specialty> findByActiveOrderByNameAsc(boolean active);
}
