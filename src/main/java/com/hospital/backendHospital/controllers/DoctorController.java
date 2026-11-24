package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.doctor.AvailableSlotDto;
import com.hospital.backendHospital.models.dto.doctor.CreateDoctorDto;
import com.hospital.backendHospital.models.dto.doctor.DoctorResponseDto;
import com.hospital.backendHospital.models.dto.doctor.UpdateDoctorDto;
import com.hospital.backendHospital.models.filters.DoctorFilterRequest;
import com.hospital.backendHospital.services.IDoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final IDoctorService doctorService;
    private final AuthService authService;

    /** Catalogo de doctores. Un PACIENTE solo recibe los activos. */
    @GetMapping
    public Page<DoctorResponseDto> filterDoctors(DoctorFilterRequest filter, @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return doctorService.filterDoctors(authService.getAuthenticatedUser(), filter, pageable);
    }

    @GetMapping("/me")
    public DoctorResponseDto getMyProfile() {
        return doctorService.getMyProfile(authService.getAuthenticatedUser());
    }

    @GetMapping("/{id}")
    public DoctorResponseDto getDoctor(@PathVariable Long id) {
        return doctorService.getDoctor(authService.getAuthenticatedUser(), id);
    }

    /** Horarios libres del doctor en una fecha (para agendar). */
    @GetMapping("/{id}/availability")
    public List<AvailableSlotDto> getAvailability(@PathVariable Long id,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return doctorService.getAvailability(id, date);
    }

    @PostMapping
    public ResponseEntity<DoctorResponseDto> createDoctor(@Valid @RequestBody CreateDoctorDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createDoctor(dto));
    }

    @PatchMapping("/{id}")
    public DoctorResponseDto updateDoctor(@PathVariable Long id, @Valid @RequestBody UpdateDoctorDto dto) {
        return doctorService.updateDoctor(id, dto);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateDoctor(@PathVariable Long id) {
        doctorService.deactivateDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateDoctor(@PathVariable Long id) {
        doctorService.activateDoctor(id);
        return ResponseEntity.noContent().build();
    }
}
