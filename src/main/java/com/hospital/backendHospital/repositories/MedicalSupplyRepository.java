package com.hospital.backendHospital.repositories;

import com.hospital.backendHospital.models.entity.MedicalSupply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MedicalSupplyRepository extends JpaRepository<MedicalSupply, Long>, JpaSpecificationExecutor<MedicalSupply> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    /** M11: insumos activos en o por debajo de su stock minimo. */
    @Query("select s from MedicalSupply s where s.active = true and s.quantity <= s.minimumStock order by s.name")
    List<MedicalSupply> findLowStock();

    @Query("select count(s) from MedicalSupply s where s.active = true and s.quantity <= s.minimumStock")
    long countLowStock();
}
