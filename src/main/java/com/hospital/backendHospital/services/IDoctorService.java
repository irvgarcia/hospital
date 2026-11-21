package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.doctor.AvailableSlotDto;
import com.hospital.backendHospital.models.dto.doctor.CreateDoctorDto;
import com.hospital.backendHospital.models.dto.doctor.DoctorResponseDto;
import com.hospital.backendHospital.models.dto.doctor.UpdateDoctorDto;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.models.filters.DoctorFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface IDoctorService {

    Page<DoctorResponseDto> filterDoctors(User user, DoctorFilterRequest filter, Pageable pageable);

    DoctorResponseDto getDoctor(User user, Long id);

    DoctorResponseDto getMyProfile(User user);

    List<AvailableSlotDto> getAvailability(Long id, LocalDate date);

    DoctorResponseDto createDoctor(CreateDoctorDto createDoctorDto);

    DoctorResponseDto updateDoctor(Long id, UpdateDoctorDto updateDoctorDto);

    void deactivateDoctor(Long id);

    void activateDoctor(Long id);
}
