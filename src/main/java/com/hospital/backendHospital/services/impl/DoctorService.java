package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.auth.service.TokenService;
import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.DoctorMapper;
import com.hospital.backendHospital.models.dto.doctor.AvailableSlotDto;
import com.hospital.backendHospital.models.dto.doctor.CreateDoctorDto;
import com.hospital.backendHospital.models.dto.doctor.DoctorResponseDto;
import com.hospital.backendHospital.models.dto.doctor.UpdateDoctorDto;
import com.hospital.backendHospital.models.dto.doctorSchedule.ScheduleSlotDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.models.filters.DoctorFilterRequest;
import com.hospital.backendHospital.repositories.*;
import com.hospital.backendHospital.services.IDoctorService;
import com.hospital.backendHospital.services.validation.ScheduleValidator;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService implements IDoctorService {

    /** Duracion de cada horario libre que se ofrece al agendar. */
    static final int SLOT_MINUTES = 30;

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final HospitalizationRepository hospitalizationRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final RoleRepository roleRepository;
    private final DoctorMapper doctorMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponseDto> filterDoctors(User user, DoctorFilterRequest filter, Pageable pageable) {
        // El paciente solo ve el catalogo de doctores activos (A12)
        if (isPatientOnly(user)) {
            filter.setStatus(true);
        }
        Specification<Doctor> spec = Specification.where(null);
        if (hasText(filter.getName())) {
            String pattern = like(filter.getName());
            spec = spec.and((root, query, cb) -> {
                Join<Object, Object> userJoin = root.join("user");
                return cb.or(
                        cb.like(cb.lower(userJoin.get("firstName")), pattern),
                        cb.like(cb.lower(userJoin.get("lastName")), pattern),
                        cb.like(cb.lower(cb.concat(cb.concat(userJoin.get("firstName"), " "), userJoin.get("lastName"))), pattern),
                        cb.like(cb.lower(userJoin.get("email")), pattern));
            });
        }
        if (hasText(filter.getFirstName())) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.join("user").get("firstName")), like(filter.getFirstName())));
        }
        if (hasText(filter.getLastName())) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.join("user").get("lastName")), like(filter.getLastName())));
        }
        if (filter.getSpecialtyId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("specialty").get("id"), filter.getSpecialtyId()));
        }
        if (hasText(filter.getSpecialty())) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.join("specialty").get("name")), filter.getSpecialty().trim().toLowerCase()));
        }
        if (filter.getStatus() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), filter.getStatus()));
        }
        if (filter.getRegisterDate() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("registerDate"), filter.getRegisterDate()));
        }
        return doctorRepository.findAll(spec, pageable).map(doctorMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponseDto getDoctor(User user, Long id) {
        Doctor doctor = findDoctor(id);
        if (isPatientOnly(user) && !doctor.isActive()) {
            throw new EntityNotFoundException("No existe el doctor con id " + id);
        }
        return doctorMapper.toResponseDto(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponseDto getMyProfile(User user) {
        Doctor doctor = doctorRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("No existe el perfil de doctor del usuario"));
        return doctorMapper.toResponseDto(doctor);
    }

    /**
     * Horarios libres de SLOT_MINUTES dentro del turno del doctor para esa fecha, sin las citas ya tomadas
     * y, si la fecha es hoy, sin las horas que ya pasaron. Lista vacia si el doctor no atiende ese dia.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotDto> getAvailability(Long id, LocalDate date) {
        Doctor doctor = findDoctor(id);
        if (!doctor.isActive() || !doctor.getSpecialty().isActive() || date.isBefore(LocalDate.now())) {
            return List.of();
        }
        List<Appointment> taken = appointmentRepository.findActiveByDoctorAndDate(id, date);
        LocalTime now = LocalTime.now();
        List<AvailableSlotDto> slots = new ArrayList<>();
        doctor.getSchedules().stream()
                .filter(schedule -> schedule.getDayOfWeek() == date.getDayOfWeek())
                .sorted(Comparator.comparing(DoctorSchedule::getStartTime))
                .forEach(schedule -> {
                    LocalTime start = schedule.getStartTime();
                    while (!start.plusMinutes(SLOT_MINUTES).isAfter(schedule.getEndTime())
                            && start.plusMinutes(SLOT_MINUTES).isAfter(start)) {
                        LocalTime end = start.plusMinutes(SLOT_MINUTES);
                        LocalTime slotStart = start;
                        boolean free = taken.stream().noneMatch(a -> a.getStartTime().isBefore(end) && a.getEndTime().isAfter(slotStart));
                        boolean notPast = !date.isEqual(LocalDate.now()) || slotStart.isAfter(now);
                        if (free && notPast) {
                            slots.add(new AvailableSlotDto(slotStart, end));
                        }
                        start = end;
                    }
                });
        return slots;
    }

    @Override
    @Transactional
    public DoctorResponseDto createDoctor(CreateDoctorDto dto) {
        String email = normalizeEmail(dto.getEmail());
        if (userRepository.existsByEmail(email)){
            throw new ConflictException("Ya existe un usuario con el email " + email);
        }
        validateSchedules(dto.getSchedules());

        Role doctorRole = roleRepository.findByRoleEnum(RoleEnum.DOCTOR)
                .orElseThrow(() -> new EntityNotFoundException("No existe el rol DOCTOR"));
        Specialty specialty = findActiveSpecialty(dto.getSpecialtyId());

        User user = User.builder()
                .email(email)
                .firstName(dto.getFirstName().trim())
                .lastName(dto.getLastName().trim())
                .password(passwordEncoder.encode(dto.getPassword()))
                .roles(new HashSet<>(Set.of(doctorRole)))
                .build();

        Doctor doctor = Doctor.builder()
                .user(user)
                .specialty(specialty)
                .phoneNumber(dto.getPhoneNumber())
                .registerDate(LocalDate.now())
                .build();
        doctorRepository.save(doctor);

        for (ScheduleSlotDto slot : dto.getSchedules()) {
            DoctorSchedule schedule = DoctorSchedule.builder()
                    .doctor(doctor)
                    .dayOfWeek(slot.getDayOfWeek())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .build();
            doctorScheduleRepository.save(schedule);
            doctor.getSchedules().add(schedule);
        }
        return doctorMapper.toResponseDto(doctor);
    }

    @Override
    @Transactional
    public DoctorResponseDto updateDoctor(Long id, UpdateDoctorDto dto) {
        Doctor doctor = findDoctor(id);
        User user = doctor.getUser();

        if (dto.getEmail() != null) {
            String email = normalizeEmail(dto.getEmail());
            if (!email.equals(user.getEmail()) && userRepository.existsByEmailAndIdNot(email, user.getId())) {
                throw new ConflictException("Ya existe un usuario con el email " + email);
            }
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                // El email es el subject del JWT: los tokens emitidos con el anterior dejan de servir
                tokenService.revokeAllUserTokens(user);
            }
        }
        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName().trim());
        }
        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName().trim());
        }
        if (dto.getNewPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }
        if (dto.getSpecialtyId() != null && !dto.getSpecialtyId().equals(doctor.getSpecialty().getId())) {
            doctor.setSpecialty(findActiveSpecialty(dto.getSpecialtyId()));
        }
        if (dto.getPhoneNumber() != null) {
            doctor.setPhoneNumber(dto.getPhoneNumber());
        }
        // A4: los horarios solo se tocan si vienen en la peticion
        if (dto.getSchedules() != null) {
            replaceSchedules(doctor, dto.getSchedules());
        }
        doctorRepository.save(doctor);
        return doctorMapper.toResponseDto(doctor);
    }

    @Override
    @Transactional
    public void deactivateDoctor(Long id) {
        Doctor doctor = findDoctor(id);
        // A5: solo bloquean las citas PENDIENTES de hoy en adelante (no las canceladas ni las completadas)
        if (appointmentRepository.existsByDoctorIdAndStatusAndDateGreaterThanEqual(id, AppointmentStatus.PENDING, LocalDate.now())) {
            throw new InvalidDataException("No se puede desactivar al doctor " + fullName(doctor)
                    + " porque tiene citas pendientes; cancélalas o reprográmalas primero");
        }
        if (hospitalizationRepository.existsByDoctorIdAndActiveTrue(id)) {
            throw new InvalidDataException("No se puede desactivar al doctor " + fullName(doctor)
                    + " porque es responsable de hospitalizaciones activas");
        }
        doctor.setActive(false);
        doctor.getUser().setActive(false);
        tokenService.revokeAllUserTokens(doctor.getUser());
        doctorRepository.save(doctor);
    }

    @Override
    @Transactional
    public void activateDoctor(Long id) {
        Doctor doctor = findDoctor(id);
        doctor.setActive(true);
        doctor.getUser().setActive(true);
        doctorRepository.save(doctor);
    }

    /**
     * A4: reemplaza los horarios. Rechaza dias repetidos o turnos invalidos, y no deja quitar ni recortar
     * un dia en el que el doctor tiene citas pendientes que quedarian fuera del nuevo horario.
     */
    private void replaceSchedules(Doctor doctor, List<ScheduleSlotDto> newSlots) {
        validateSchedules(newSlots);
        Map<DayOfWeek, ScheduleSlotDto> byDay = newSlots.stream()
                .collect(Collectors.toMap(ScheduleSlotDto::getDayOfWeek, Function.identity()));

        List<Appointment> pending = appointmentRepository.findByDoctorIdAndStatusAndDateGreaterThanEqual(
                doctor.getId(), AppointmentStatus.PENDING, LocalDate.now());
        for (Appointment appointment : pending) {
            ScheduleSlotDto slot = byDay.get(appointment.getDate().getDayOfWeek());
            boolean stillFits = slot != null
                    && !appointment.getStartTime().isBefore(slot.getStartTime())
                    && !appointment.getEndTime().isAfter(slot.getEndTime());
            if (!stillFits) {
                throw new InvalidDataException("El nuevo horario deja fuera la cita pendiente del "
                        + appointment.getDate() + " a las " + appointment.getStartTime()
                        + "; cancélala o reprográmala primero");
            }
        }

        List<DoctorSchedule> current = doctorScheduleRepository.findByDoctorId(doctor.getId());
        for (DoctorSchedule existing : current) {
            ScheduleSlotDto slot = byDay.remove(existing.getDayOfWeek());
            if (slot == null) {
                doctor.getSchedules().remove(existing);
                doctorScheduleRepository.delete(existing);
            } else {
                existing.setStartTime(slot.getStartTime());
                existing.setEndTime(slot.getEndTime());
                doctorScheduleRepository.save(existing);
            }
        }
        for (ScheduleSlotDto slot : byDay.values()) {
            DoctorSchedule schedule = DoctorSchedule.builder()
                    .doctor(doctor)
                    .dayOfWeek(slot.getDayOfWeek())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .build();
            doctorScheduleRepository.save(schedule);
            doctor.getSchedules().add(schedule);
        }
    }

    private static void validateSchedules(List<ScheduleSlotDto> slots) {
        ScheduleValidator.validateNoDuplicateDays(slots.stream().map(ScheduleSlotDto::getDayOfWeek).toList());
        slots.forEach(slot -> ScheduleValidator.validate(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()));
    }

    private Specialty findActiveSpecialty(Long specialtyId) {
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new EntityNotFoundException("No existe la especialidad con id " + specialtyId));
        if (!specialty.isActive()) {
            throw new InvalidDataException("La especialidad " + specialty.getName() + " está desactivada");
        }
        return specialty;
    }

    private Doctor findDoctor(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe el doctor con id " + id));
    }

    private static boolean isPatientOnly(User user) {
        return user != null && user.hasRole(RoleEnum.PATIENT)
                && !user.hasRole(RoleEnum.ADMIN) && !user.hasRole(RoleEnum.RECEPTIONIST) && !user.hasRole(RoleEnum.DOCTOR);
    }

    private static String fullName(Doctor doctor) {
        return doctor.getUser().getFirstName() + " " + doctor.getUser().getLastName();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
