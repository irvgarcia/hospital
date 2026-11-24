package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.models.dto.doctorSchedule.CreateDoctorScheduleDto;
import com.hospital.backendHospital.models.dto.doctorSchedule.DoctorScheduleResponseDto;
import com.hospital.backendHospital.models.dto.doctorSchedule.UpdateDoctorScheduleDto;
import com.hospital.backendHospital.models.filters.DoctorScheduleFilterRequest;
import com.hospital.backendHospital.services.IDoctorScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/doctor-schedules")
public class DoctorScheduleController {

    private final IDoctorScheduleService doctorScheduleService;

    @GetMapping
    public Page<DoctorScheduleResponseDto> filterDoctorSchedules(DoctorScheduleFilterRequest filter,
                                                                 @PageableDefault(size = 20, sort = "dayOfWeek") Pageable pageable) {
        return doctorScheduleService.filterDoctorSchedules(filter, pageable);
    }

    @PostMapping
    public ResponseEntity<DoctorScheduleResponseDto> createSchedule(@Valid @RequestBody CreateDoctorScheduleDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorScheduleService.createDoctorSchedule(dto));
    }

    @PatchMapping("/{id}")
    public DoctorScheduleResponseDto updateSchedule(@PathVariable Long id, @Valid @RequestBody UpdateDoctorScheduleDto dto) {
        return doctorScheduleService.updateDoctorSchedule(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        doctorScheduleService.deleteDoctorSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
