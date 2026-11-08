package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.AppointmentMapper;
import com.hospital.backendHospital.models.filters.AppointmentFilterRequest;
import com.hospital.backendHospital.models.dto.appointment.AppointmentResponseDto;
import com.hospital.backendHospital.models.dto.appointment.CreateAppointmentDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.AppointmentRepository;
import com.hospital.backendHospital.repositories.DoctorRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.services.IAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService implements IAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentMapper appointmentMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDto> filterAppointments(User user, AppointmentFilterRequest filter, Pageable pageable) {
        // Un doctor solo puede ver sus propias citas, sin importar el doctorId que envie
        if (user.hasRole(RoleEnum.DOCTOR) && !user.hasRole(RoleEnum.ADMIN)) {
            Doctor doctor = doctorRepository.findByUser(user).orElseThrow(()-> new EntityNotFoundException("Doctor not found"));
            filter.setDoctorId(doctor.getId());
        }

        Specification<Appointment> spec = Specification.where(null);

        if (filter.getPatientId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("patient").get("id"), filter.getPatientId()));
        }

        if (filter.getDoctorId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("doctor").get("id"), filter.getDoctorId()));
        }

        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("status")), filter.getStatus().toUpperCase()));
        }

        if (filter.getDate() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("date"), filter.getDate()));
        }

        return appointmentRepository.findAll(spec, pageable)
                .map(appointmentMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDto> listAppointmentsByPatient(User user, Pageable pageable) {
        Patient patient = patientRepository.findByUser(user).orElseThrow(()-> new EntityNotFoundException("Patient not found"));

        Page<Appointment> appointments = appointmentRepository.findAllByPatientId(patient.getId(), pageable);

        if (appointments.isEmpty()){
            throw new EntityNotFoundException("Appointments not found");
        }

        return appointments.map(appointmentMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDto listActiveAppointmentByPatient(User user) {
        Patient patient = patientRepository.findByUser(user).orElseThrow(()-> new EntityNotFoundException("Patient not found"));

        Appointment appointment = appointmentRepository.findByPatientIdAndStatus(patient.getId(), AppointmentStatus.PENDING);

        if (appointment == null){
            throw new EntityNotFoundException("Appointment not found");
        }

        return appointmentMapper.toResponseDto(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponseDto createAppointment(User user, CreateAppointmentDto createAppointmentDto) {
        Patient patient = resolvePatientForBooking(user, createAppointmentDto);

        Doctor doctor = doctorRepository.findById(createAppointmentDto.getDoctorId()).orElseThrow(()-> new EntityNotFoundException("Doctor not found"));

        if (createAppointmentDto.getDate().isBefore(LocalDate.now())) {
            throw new InvalidDataException("Cannot create appointments in the past");
        }

        if (appointmentRepository.existsOverlappingAppointment(createAppointmentDto.getDate(), createAppointmentDto.getStartTime(), createAppointmentDto.getEndTime())){
            throw new InvalidDataException("Doctor " + doctor.getUser().getFirstName() + " already has an appointment for that time and day");
        }

        if (!doctor.isActive()){
            throw new InvalidDataException("Doctor " + doctor.getUser().getFirstName() + "is inactive");
        }

        List<DoctorSchedule> doctorSchedules = doctor.getSchedules().stream()
                .filter(doctorSchedule -> doctorSchedule.getDayOfWeek() == createAppointmentDto.getDate().getDayOfWeek())
                .toList();

        if (doctorSchedules.isEmpty()){
            throw new InvalidDataException("Doctor "+ doctor.getUser().getFirstName() +" doesnt work this day");
        }

        boolean isWithinSchedule = doctorSchedules.stream()
                .anyMatch(schedule ->
                        !createAppointmentDto.getStartTime().isBefore(schedule.getStartTime()) &&
                                !createAppointmentDto.getEndTime().isAfter(schedule.getEndTime()));

        if (!isWithinSchedule){
            throw new InvalidDataException("Appointment is not within the doctors " + doctor.getUser().getFirstName() + " available schedule");
        }

        Duration duration = Duration.between(createAppointmentDto.getStartTime(), createAppointmentDto.getEndTime());

        if (duration.toMinutes() < 15){
            throw new InvalidDataException("The appointment should last at least 15 minutes");
        }

        if (duration.toMinutes() > 60){
            throw new InvalidDataException("The appointment should not last more than 1 hours.");
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .date(createAppointmentDto.getDate())
                .startTime(createAppointmentDto.getStartTime())
                .endTime(createAppointmentDto.getEndTime())
                .status(AppointmentStatus.PENDING)
                .total(doctor.getSpecialty().getCost())
                .build();

        appointmentRepository.save(appointment);

        return appointmentMapper.toResponseDto(appointment);
    }

    @Override
    @Transactional
    public void completeAppointment(User user, Long id) {
        Appointment appointment = appointmentRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Appointment not found with id "+ id));

        if (!appointment.getDoctor().getId().equals(user.getId())){
            throw new InvalidDataException("Error cancelling appointment");
        }

        if (appointment.getStatus().equals(AppointmentStatus.COMPLETED) || appointment.getStatus().equals(AppointmentStatus.CANCELLED)){
            throw new InvalidDataException("Appointment has already been completed");
        }

        if (appointment.getDate().isAfter(LocalDate.now())){
            throw new InvalidDataException("Unable to complete a future appointment");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);

        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void cancelAppointment(User user, Long id) {
        Appointment appointment = appointmentRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Appointment not found"));

        // ADMIN y RECEPTIONIST cancelan cualquier cita; el PACIENTE solo las suyas
        if (user.hasRole(RoleEnum.PATIENT) && !appointment.getPatient().getUser().getId().equals(user.getId())){
            throw new AccessDeniedException("You can only cancel your own appointments");
        }

        if (appointment.getStatus().equals(AppointmentStatus.CANCELLED) || appointment.getStatus().equals(AppointmentStatus.COMPLETED)){
            throw new InvalidDataException("This appointment cannot be canceled because it has already finished");
        }

        if (appointment.getDate().isBefore(LocalDate.now())){
            throw new InvalidDataException("Cannot cancel past appointments");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);

        appointmentRepository.save(appointment);
    }

    /** El paciente agenda para si mismo; ADMIN y RECEPTIONIST agendan para el paciente indicado en patientId. */
    private Patient resolvePatientForBooking(User user, CreateAppointmentDto createAppointmentDto) {
        if (user.hasRole(RoleEnum.PATIENT)) {
            return patientRepository.findByUser(user).orElseThrow(()-> new EntityNotFoundException("Patient not found"));
        }

        if (createAppointmentDto.getPatientId() == null) {
            throw new InvalidDataException("Patient is required");
        }

        return patientRepository.findById(createAppointmentDto.getPatientId())
                .orElseThrow(()-> new EntityNotFoundException("Patient not found with id " + createAppointmentDto.getPatientId()));
    }
}
