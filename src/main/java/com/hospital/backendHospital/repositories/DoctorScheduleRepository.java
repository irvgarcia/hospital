package com.hospital.backendHospital.repositories;

import com.hospital.backendHospital.models.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.DayOfWeek;
import java.util.List;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long>, JpaSpecificationExecutor<DoctorSchedule> {

    List<DoctorSchedule> findByDoctorId(Long id);

    boolean existsByDoctorIdAndDayOfWeek(Long id, DayOfWeek dayOfWeek);
}
