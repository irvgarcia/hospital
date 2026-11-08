package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.filters.AppointmentFilterRequest;
import com.hospital.backendHospital.models.dto.appointment.AppointmentResponseDto;
import com.hospital.backendHospital.models.dto.appointment.CreateAppointmentDto;
import com.hospital.backendHospital.models.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAppointmentService {

    Page<AppointmentResponseDto> filterAppointments(User user, AppointmentFilterRequest filter, Pageable pageable);

    Page<AppointmentResponseDto> listAppointmentsByPatient(User user, Pageable pageable);

    AppointmentResponseDto listActiveAppointmentByPatient(User user);

    AppointmentResponseDto createAppointment(User user, CreateAppointmentDto createAppointmentDto);

    void completeAppointment(User user, Long id);

    void cancelAppointment(User user, Long id);
}
