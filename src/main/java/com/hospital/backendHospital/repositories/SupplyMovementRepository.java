package com.hospital.backendHospital.repositories;

import com.hospital.backendHospital.models.entity.SupplyMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplyMovementRepository extends JpaRepository<SupplyMovement, Long>, JpaSpecificationExecutor<SupplyMovement> {
}
