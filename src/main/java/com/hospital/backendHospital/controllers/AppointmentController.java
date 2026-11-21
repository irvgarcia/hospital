package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.appointment.AppointmentResponseDto;
import com.hospital.backendHospital.models.dto.appointment.CreateAppointmentDto;
import com.hospital.backendHospital.models.dto.appointment.RescheduleAppointmentDto;
import com.hospital.backendHospital.models.filters.AppointmentFilterRequest;
import com.hospital.backendHospital.services.IAppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final IAppointmentService appointmentService;
    private final AuthService authService;

    /** ADMIN y RECEPTIONIST ven todas; un DOCTOR solo las suyas. */
    @GetMapping
    public Page<AppointmentResponseDto> filterAppointments(
            AppointmentFilterRequest filter,
            @PageableDefault(size = 10)
            @SortDefault.SortDefaults({@SortDefault(sort = "date", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "startTime", direction = Sort.Direction.DESC)}) Pageable pageable) {
        return appointmentService.filterAppointments(authService.getAuthenticatedUser(), filter, pageable);
    }

    /** Citas del paciente o del doctor autenticado. */
    @GetMapping("/me")
    public Page<AppointmentResponseDto> getMyAppointments(
            AppointmentFilterRequest filter,
            @PageableDefault(size = 10)
            @SortDefault.SortDefaults({@SortDefault(sort = "date", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "startTime", direction = Sort.Direction.DESC)}) Pageable pageable) {
        return appointmentService.listMyAppointments(authService.getAuthenticatedUser(), filter, pageable);
    }

    /** Proxima cita pendiente del paciente; 204 si no tiene. */
    @GetMapping("/me/next")
    public ResponseEntity<AppointmentResponseDto> getNextAppointment() {
        return appointmentService.getNextAppointment(authService.getAuthenticatedUser())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public AppointmentResponseDto getAppointment(@PathVariable Long id) {
        return appointmentService.getAppointment(authService.getAuthenticatedUser(), id);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> createAppointment(@Valid @RequestBody CreateAppointmentDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(authService.getAuthenticatedUser(), dto));
    }

    @PatchMapping("/{id}/reschedule")
    public AppointmentResponseDto rescheduleAppointment(@PathVariable Long id, @Valid @RequestBody RescheduleAppointmentDto dto) {
        return appointmentService.rescheduleAppointment(authService.getAuthenticatedUser(), id, dto);
    }

    @PatchMapping("/{id}/complete")
    public AppointmentResponseDto completeAppointment(@PathVariable Long id) {
        return appointmentService.completeAppointment(authService.getAuthenticatedUser(), id);
    }

    @PatchMapping("/{id}/cancel")
    public AppointmentResponseDto cancelAppointment(@PathVariable Long id) {
        return appointmentService.cancelAppointment(authService.getAuthenticatedUser(), id);
    }
}
