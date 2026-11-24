package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.appointment.AppointmentResponseDto;
import com.hospital.backendHospital.models.dto.appointment.CreateAppointmentDto;
import com.hospital.backendHospital.models.dto.appointment.RescheduleAppointmentDto;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.models.filters.AppointmentFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface IAppointmentService {

    Page<AppointmentResponseDto> filterAppointments(User user, AppointmentFilterRequest filter, Pageable pageable);

    Page<AppointmentResponseDto> listMyAppointments(User user, AppointmentFilterRequest filter, Pageable pageable);

    Optional<AppointmentResponseDto> getNextAppointment(User user);

    AppointmentResponseDto getAppointment(User user, Long id);

    AppointmentResponseDto createAppointment(User user, CreateAppointmentDto createAppointmentDto);

    AppointmentResponseDto rescheduleAppointment(User user, Long id, RescheduleAppointmentDto rescheduleAppointmentDto);

    AppointmentResponseDto completeAppointment(User user, Long id);

    AppointmentResponseDto cancelAppointment(User user, Long id);
}
