package com.hospital.backendHospital.repositories;

import com.hospital.backendHospital.models.entity.Doctor;
import com.hospital.backendHospital.models.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long>, JpaSpecificationExecutor<Doctor> {

    Optional<Doctor> findByUser(User user);

    /**
     * A8: bloquea la fila del doctor hasta el fin de la transaccion. Dos reservas simultaneas con el mismo doctor
     * se atienden una despues de otra, asi la segunda ve la cita de la primera al validar el solapamiento.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);

    long countByActiveTrue();

    long countBySpecialtyIdAndActiveTrue(Long specialtyId);
}
