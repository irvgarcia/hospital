package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.filters.AppointmentFilterRequest;
import com.hospital.backendHospital.models.dto.appointment.AppointmentResponseDto;
import com.hospital.backendHospital.models.dto.appointment.CreateAppointmentDto;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.services.IAppointmentService;
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
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final IAppointmentService appointmentService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<Page<AppointmentResponseDto>> filterAppointments
            (AppointmentFilterRequest filter,@PageableDefault(page = 0, size = 10, sort = "date") Pageable pageable){
        User user = authService.getAuthenticatedUser();
        Page<AppointmentResponseDto> result = appointmentService.filterAppointments(user, filter, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/patients/me")
    public ResponseEntity<Page<AppointmentResponseDto>> getAllAppointments(@PageableDefault(page = 0, size = 10, sort = "date") Pageable pageable){
        User user = authService.getAuthenticatedUser();

        return ResponseEntity.ok(appointmentService.listAppointmentsByPatient(user, pageable));
    }

    @GetMapping("/patients/active/me")
    public ResponseEntity<AppointmentResponseDto> getActiveAppointment(){
        User user = authService.getAuthenticatedUser();

        return ResponseEntity.ok(appointmentService.listActiveAppointmentByPatient(user));
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> createAppointment(@Valid @RequestBody CreateAppointmentDto createAppointmentDto){
        User user = authService.getAuthenticatedUser();

        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.createAppointment(user, createAppointmentDto));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> completeAppointment(@PathVariable Long id){
        User user = authService.getAuthenticatedUser();

        appointmentService.completeAppointment(user, id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id){
        User user = authService.getAuthenticatedUser();

        appointmentService.cancelAppointment(user, id);

        return ResponseEntity.noContent().build();
    }
}
