package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.AppointmentMapper;
import com.hospital.backendHospital.models.dto.appointment.AppointmentResponseDto;
import com.hospital.backendHospital.models.dto.appointment.CreateAppointmentDto;
import com.hospital.backendHospital.models.dto.appointment.RescheduleAppointmentDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.models.filters.AppointmentFilterRequest;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppointmentService implements IAppointmentService {

    static final int MIN_APPOINTMENT_MINUTES = 15;
    static final int MAX_APPOINTMENT_MINUTES = 60;
    /** Valor que nunca es un id: las validaciones de solapamiento no excluyen ninguna cita. */
    private static final long NO_APPOINTMENT = -1L;

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentMapper appointmentMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDto> filterAppointments(User user, AppointmentFilterRequest filter, Pageable pageable) {
        // Un doctor solo puede ver sus propias citas, sin importar el doctorId que envie
        if (user.hasRole(RoleEnum.DOCTOR) && !user.hasRole(RoleEnum.ADMIN)) {
            filter.setDoctorId(currentDoctor(user).getId());
        }
        return appointmentRepository.findAll(toSpecification(filter), pageable)
                .map(appointmentMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDto> listMyAppointments(User user, AppointmentFilterRequest filter, Pageable pageable) {
        if (user.hasRole(RoleEnum.PATIENT)) {
            filter.setPatientId(currentPatient(user).getId());
            filter.setDoctorId(null);
        } else if (user.hasRole(RoleEnum.DOCTOR)) {
            filter.setDoctorId(currentDoctor(user).getId());
            filter.setPatientId(null);
        } else {
            throw new AccessDeniedException("Solo pacientes y doctores tienen citas propias");
        }
        return appointmentRepository.findAll(toSpecification(filter), pageable)
                .map(appointmentMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppointmentResponseDto> getNextAppointment(User user) {
        Patient patient = currentPatient(user);
        // A2: el paciente puede tener varias citas pendientes (con doctores distintos); se devuelve la proxima
        return appointmentRepository
                .findFirstByPatientIdAndStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                        patient.getId(), AppointmentStatus.PENDING, LocalDate.now())
                .map(appointmentMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDto getAppointment(User user, Long id) {
        Appointment appointment = findAppointment(id);
        checkCanView(user, appointment);
        return appointmentMapper.toResponseDto(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponseDto createAppointment(User user, CreateAppointmentDto dto) {
        validateTimeRange(dto.getDate(), dto.getStartTime(), dto.getEndTime());

        Patient patient = resolvePatientForBooking(user, dto);
        if (!patient.isActive()) {
            throw new InvalidDataException("El paciente está inactivo");
        }
        // A8: bloquea al doctor para que dos reservas simultaneas no pasen las dos la validacion de solapamiento
        Doctor doctor = doctorRepository.findByIdForUpdate(dto.getDoctorId())
                .orElseThrow(() -> new EntityNotFoundException("No existe el doctor con id " + dto.getDoctorId()));
        validateDoctorAvailability(doctor, dto.getDate(), dto.getStartTime(), dto.getEndTime());

        // A2: varias citas pendientes estan permitidas, pero no dos con el mismo doctor
        if (appointmentRepository.existsByPatientIdAndDoctorIdAndStatusAndDateGreaterThanEqual(
                patient.getId(), doctor.getId(), AppointmentStatus.PENDING, LocalDate.now())) {
            throw new InvalidDataException("El paciente ya tiene una cita pendiente con este doctor");
        }
        validateNoOverlap(doctor, patient, dto.getDate(), dto.getStartTime(), dto.getEndTime(), NO_APPOINTMENT);

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .date(dto.getDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(AppointmentStatus.PENDING)
                .total(doctor.getSpecialty().getCost())
                .reason(blankToNull(dto.getReason()))
                .build();

        appointmentRepository.save(appointment);
        return appointmentMapper.toResponseDto(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponseDto rescheduleAppointment(User user, Long id, RescheduleAppointmentDto dto) {
        Appointment appointment = findAppointment(id);
        if (user.hasRole(RoleEnum.PATIENT) && !isOwnPatientAppointment(user, appointment)) {
            throw new AccessDeniedException("Solo puedes reprogramar tus propias citas");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new InvalidDataException("Solo se pueden reprogramar citas pendientes");
        }
        validateTimeRange(dto.getDate(), dto.getStartTime(), dto.getEndTime());

        Doctor doctor = doctorRepository.findByIdForUpdate(appointment.getDoctor().getId())
                .orElseThrow(() -> new EntityNotFoundException("No existe el doctor de la cita"));
        validateDoctorAvailability(doctor, dto.getDate(), dto.getStartTime(), dto.getEndTime());
        validateNoOverlap(doctor, appointment.getPatient(), dto.getDate(), dto.getStartTime(), dto.getEndTime(), appointment.getId());

        appointment.setDate(dto.getDate());
        appointment.setStartTime(dto.getStartTime());
        appointment.setEndTime(dto.getEndTime());
        appointmentRepository.save(appointment);
        return appointmentMapper.toResponseDto(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponseDto completeAppointment(User user, Long id) {
        Appointment appointment = findAppointment(id);
        // Se compara contra el usuario del doctor: doctor.id (tabla doctors) y user.id (tabla users) son ids distintos
        if (!appointment.getDoctor().getUser().getId().equals(user.getId())){
            throw new AccessDeniedException("Solo puedes completar tus propias citas");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING){
            throw new InvalidDataException("La cita ya está " + statusLabel(appointment.getStatus()));
        }
        if (appointment.getDate().isAfter(LocalDate.now())){
            throw new InvalidDataException("No se puede completar una cita futura");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
        return appointmentMapper.toResponseDto(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponseDto cancelAppointment(User user, Long id) {
        Appointment appointment = findAppointment(id);
        // ADMIN y RECEPTIONIST cancelan cualquier cita; el PACIENTE solo las suyas
        if (user.hasRole(RoleEnum.PATIENT) && !isOwnPatientAppointment(user, appointment)){
            throw new AccessDeniedException("Solo puedes cancelar tus propias citas");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING){
            throw new InvalidDataException("La cita ya está " + statusLabel(appointment.getStatus()));
        }
        if (appointment.getDate().isBefore(LocalDate.now())){
            throw new InvalidDataException("No se pueden cancelar citas pasadas");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        return appointmentMapper.toResponseDto(appointment);
    }

    // ---------------------------------------------------------------- reglas

    /** M2: fecha no pasada, inicio antes que fin, duracion de 15 a 60 minutos y, si es hoy, hora aun no pasada. */
    private void validateTimeRange(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date.isBefore(LocalDate.now())) {
            throw new InvalidDataException("No se pueden agendar citas en el pasado");
        }
        if (!startTime.isBefore(endTime)) {
            throw new InvalidDataException("La hora de inicio debe ser anterior a la hora de fin");
        }
        if (date.isEqual(LocalDate.now()) && !startTime.isAfter(LocalTime.now())) {
            throw new InvalidDataException("La hora de inicio ya pasó");
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes < MIN_APPOINTMENT_MINUTES) {
            throw new InvalidDataException("La cita debe durar al menos " + MIN_APPOINTMENT_MINUTES + " minutos");
        }
        if (minutes > MAX_APPOINTMENT_MINUTES) {
            throw new InvalidDataException("La cita no puede durar más de " + MAX_APPOINTMENT_MINUTES + " minutos");
        }
    }

    /** El doctor esta activo, su especialidad tambien y la cita cae dentro de su horario de ese dia. */
    private void validateDoctorAvailability(Doctor doctor, LocalDate date, LocalTime startTime, LocalTime endTime) {
        String doctorName = fullName(doctor.getUser());
        if (!doctor.isActive()){
            throw new InvalidDataException("El doctor " + doctorName + " está inactivo");
        }
        if (!doctor.getSpecialty().isActive()) {
            throw new InvalidDataException("La especialidad " + doctor.getSpecialty().getName() + " no está disponible");
        }
        List<DoctorSchedule> schedules = doctor.getSchedules().stream()
                .filter(schedule -> schedule.getDayOfWeek() == date.getDayOfWeek())
                .toList();
        if (schedules.isEmpty()){
            throw new InvalidDataException("El doctor " + doctorName + " no atiende ese día");
        }
        boolean isWithinSchedule = schedules.stream()
                .anyMatch(schedule -> !startTime.isBefore(schedule.getStartTime()) && !endTime.isAfter(schedule.getEndTime()));
        if (!isWithinSchedule){
            throw new InvalidDataException("La cita está fuera del horario del doctor " + doctorName);
        }
    }

    /** C6: ni el doctor ni el paciente pueden tener otra cita que se cruce (excludeId: la cita que se reprograma). */
    private void validateNoOverlap(Doctor doctor, Patient patient, LocalDate date, LocalTime startTime, LocalTime endTime, long excludeId) {
        if (appointmentRepository.existsOverlappingAppointmentForDoctor(doctor.getId(), date, startTime, endTime, excludeId)){
            throw new InvalidDataException("El doctor " + fullName(doctor.getUser()) + " ya tiene una cita en ese horario");
        }
        if (appointmentRepository.existsOverlappingAppointmentForPatient(patient.getId(), date, startTime, endTime, excludeId)){
            throw new InvalidDataException("El paciente ya tiene una cita en ese horario");
        }
    }

    // ---------------------------------------------------------------- utilidades

    private Specification<Appointment> toSpecification(AppointmentFilterRequest filter) {
        Specification<Appointment> spec = Specification.where(null);
        if (filter.getPatientId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), filter.getPatientId()));
        }
        if (filter.getDoctorId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("doctor").get("id"), filter.getDoctorId()));
        }
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            AppointmentStatus status = parseStatus(filter.getStatus());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (filter.getDate() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("date"), filter.getDate()));
        }
        if (filter.getDateFrom() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), filter.getDateFrom()));
        }
        if (filter.getDateTo() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), filter.getDateTo()));
        }
        return spec;
    }

    private static AppointmentStatus parseStatus(String status) {
        try {
            return AppointmentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Estado de cita no válido: " + status);
        }
    }

    private void checkCanView(User user, Appointment appointment) {
        if (user.hasRole(RoleEnum.ADMIN) || user.hasRole(RoleEnum.RECEPTIONIST)) {
            return;
        }
        boolean ownAsDoctor = user.hasRole(RoleEnum.DOCTOR) && appointment.getDoctor().getUser().getId().equals(user.getId());
        boolean ownAsPatient = user.hasRole(RoleEnum.PATIENT) && isOwnPatientAppointment(user, appointment);
        if (!ownAsDoctor && !ownAsPatient) {
            throw new AccessDeniedException("No tienes acceso a esta cita");
        }
    }

    private boolean isOwnPatientAppointment(User user, Appointment appointment) {
        return appointment.getPatient().getUser().getId().equals(user.getId());
    }

    /** El paciente agenda para si mismo; ADMIN y RECEPTIONIST agendan para el paciente indicado en patientId. */
    private Patient resolvePatientForBooking(User user, CreateAppointmentDto dto) {
        if (user.hasRole(RoleEnum.PATIENT)) {
            return currentPatient(user);
        }
        if (dto.getPatientId() == null) {
            throw new InvalidDataException("El paciente es obligatorio");
        }
        return patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new EntityNotFoundException("No existe el paciente con id " + dto.getPatientId()));
    }

    private Appointment findAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe la cita con id " + id));
    }

    private Patient currentPatient(User user) {
        return patientRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("Completa tu perfil de paciente antes de continuar"));
    }

    private Doctor currentDoctor(User user) {
        return doctorRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("No existe el perfil de doctor del usuario"));
    }

    private static String statusLabel(AppointmentStatus status) {
        return switch (status) {
            case PENDING -> "pendiente";
            case COMPLETED -> "completada";
            case CANCELLED -> "cancelada";
        };
    }

    private static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
