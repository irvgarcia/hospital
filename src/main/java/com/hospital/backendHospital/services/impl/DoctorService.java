package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.DoctorMapper;
import com.hospital.backendHospital.models.dto.doctor.CreateDoctorAndScheduleDto;
import com.hospital.backendHospital.models.dto.doctor.DoctorResponseDto;
import com.hospital.backendHospital.models.dto.doctor.UpdateDoctorAndScheduleDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.models.filters.DoctorFilterRequest;
import com.hospital.backendHospital.repositories.*;
import com.hospital.backendHospital.services.IDoctorService;
import jakarta.persistence.criteria.Join;
import com.hospital.backendHospital.auth.service.TokenService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService implements IDoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final RoleRepository roleRepository;
    private final DoctorMapper doctorMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponseDto> filterDoctors(DoctorFilterRequest filter, Pageable pageable) {
        Specification<Doctor> spec = Specification.where(null);

        if (filter.getFirstName() != null) {
            spec = spec.and((root, query, cb) -> {
                Join<Object, Object> userJoin = root.join("user");
                return cb.like(cb.lower(userJoin.get("firstName")), "%" + filter.getFirstName().toLowerCase() + "%");
            });
        }

        if (filter.getLastName() != null) {
            spec = spec.and((root, query, cb) -> {
                Join<Object, Object> userJoin = root.join("user");
                return cb.like(cb.lower(userJoin.get("lastName")), "%" + filter.getLastName().toLowerCase() + "%");
            });
        }

        if (filter.getSpecialty() != null) {
            spec = spec.and((root, query, cb) -> {
                Join<Object, Object> specialtyJoin = root.join("specialty");
                return cb.equal(cb.lower(specialtyJoin.get("name")), filter.getSpecialty().toLowerCase());
            });
        }

        if (filter.getStatus() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), filter.getStatus()));
        }

        if (filter.getRegisterDate() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("registerDate"), filter.getRegisterDate()));
        }

        return doctorRepository.findAll(spec, pageable)
                .map(doctorMapper::toResponseDto);
    }

    @Override
    @Transactional
    public DoctorResponseDto createDoctorAndSchedule(CreateDoctorAndScheduleDto createDoctorAndScheduleDto) {
        if (userRepository.existsByEmail(createDoctorAndScheduleDto.getEmail())){
            throw new IllegalArgumentException("Doctor already exists");
        }

        Role doctorRole = roleRepository.findByRoleEnum(RoleEnum.DOCTOR).orElseThrow(()-> new EntityNotFoundException("Role not found"));

        Specialty specialty = specialtyRepository.findById(createDoctorAndScheduleDto.getSpecialtyId()).orElseThrow(()-> new EntityNotFoundException("Specialty not found"));

        User user = User.builder()
                .email(createDoctorAndScheduleDto.getEmail())
                .firstName(createDoctorAndScheduleDto.getFirstName())
                .lastName(createDoctorAndScheduleDto.getLastName())
                .password(passwordEncoder.encode(createDoctorAndScheduleDto.getPassword()))
                .roles(Set.of(doctorRole))
                .build();

        Doctor doctor = Doctor.builder()
                .user(user)
                .specialty(specialty)
                .phoneNumber(createDoctorAndScheduleDto.getPhoneNumber())
                .registerDate(LocalDate.now())
                .build();

        doctorRepository.save(doctor);

        for (var scheduleDto : createDoctorAndScheduleDto.getSchedules()) {

            if (doctorScheduleRepository.existsByDoctorIdAndDayOfWeek(doctor.getId(), scheduleDto.getDayOfWeek())) {
                throw new InvalidDataException("Schedule already exists for day: " + scheduleDto.getDayOfWeek());
            }

            if (scheduleDto.getStartTime().isAfter(LocalTime.of(22, 0))) {
                throw new InvalidDataException("Start time must be before 22:00");
            }

            if (scheduleDto.getEndTime().isBefore(scheduleDto.getStartTime())) {
                throw new InvalidDataException("End time must be after start time");
            }

            DoctorSchedule doctorSchedule = DoctorSchedule.builder()
                    .doctor(doctor)
                    .dayOfWeek(scheduleDto.getDayOfWeek())
                    .startTime(scheduleDto.getStartTime())
                    .endTime(scheduleDto.getEndTime())
                    .build();

            doctorScheduleRepository.save(doctorSchedule);
        }

        return doctorMapper.toResponseDto(doctor);
    }

    @Override
    @Transactional
    public DoctorResponseDto updateDoctorAndSchedule(Long id, UpdateDoctorAndScheduleDto updateDoctorAndScheduleDto) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Doctor not found with id " + id));

        if (updateDoctorAndScheduleDto.getFirstName() != null){
            doctor.getUser().setFirstName(updateDoctorAndScheduleDto.getFirstName());
        }
        if (updateDoctorAndScheduleDto.getLastName() != null){
            doctor.getUser().setLastName(updateDoctorAndScheduleDto.getLastName());
        }
        if (updateDoctorAndScheduleDto.getNewPassword() != null){
            doctor.getUser().setPassword(passwordEncoder.encode(updateDoctorAndScheduleDto.getNewPassword()));
        }
        if (updateDoctorAndScheduleDto.getSpecialtyId() != null){
            Specialty specialty = specialtyRepository.findById(updateDoctorAndScheduleDto.getSpecialtyId()).orElseThrow(()-> new EntityNotFoundException("Specialty not found with id " + updateDoctorAndScheduleDto.getSpecialtyId()));
            doctor.setSpecialty(specialty);
        }
        if (updateDoctorAndScheduleDto.getPhoneNumber() != null){
            doctor.setPhoneNumber(updateDoctorAndScheduleDto.getPhoneNumber());
        }

        doctorRepository.save(doctor);

        List<DoctorSchedule> currentSchedules = doctorScheduleRepository.findByDoctorId(doctor.getId());

        // Convertir lista nueva a mapa por día
        Map<DayOfWeek, UpdateDoctorAndScheduleDto.DoctorScheduleDto> newSchedulesMap = updateDoctorAndScheduleDto.getSchedules().stream()
                .collect(Collectors.toMap(UpdateDoctorAndScheduleDto.DoctorScheduleDto::getDayOfWeek, s -> s));

        // Actualizar o eliminar horarios existentes
        for (DoctorSchedule existing : currentSchedules) {
            UpdateDoctorAndScheduleDto.DoctorScheduleDto newDto = newSchedulesMap.remove(existing.getDayOfWeek()); // También lo elimina del mapa

            if (newDto != null) {
                // Verifica si hay cambios
                if (!existing.getStartTime().equals(newDto.getStartTime()) ||
                        !existing.getEndTime().equals(newDto.getEndTime())) {

                    existing.setStartTime(newDto.getStartTime());
                    existing.setEndTime(newDto.getEndTime());
                    doctorScheduleRepository.save(existing);
                }
            } else {
                // El horario ya no existe en los nuevos => eliminar
                doctorScheduleRepository.delete(existing);
            }
        }

        // Insertar los que no estaban antes
        for (UpdateDoctorAndScheduleDto.DoctorScheduleDto remaining : newSchedulesMap.values()) {
            if (remaining.getStartTime().isAfter(LocalTime.of(22, 0))) {
                throw new InvalidDataException("Start time must be before 22:00");
            }

            if (remaining.getEndTime().isBefore(remaining.getStartTime())) {
                throw new InvalidDataException("End time must be after start time");
            }

            DoctorSchedule newSchedule = DoctorSchedule.builder()
                    .doctor(doctor)
                    .dayOfWeek(remaining.getDayOfWeek())
                    .startTime(remaining.getStartTime())
                    .endTime(remaining.getEndTime())
                    .build();

            doctorScheduleRepository.save(newSchedule);
        }
        return doctorMapper.toResponseDto(doctor);
    }

    @Override
    @Transactional
    public void deactivateDoctor(Long id) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Doctor not found with id " + id));

        List<Appointment> appointments = doctor.getAppointments().stream()
                .filter(appointment -> appointment.getDate().isAfter(LocalDate.now()))
                .toList();

        if (!appointments.isEmpty()){
            throw new InvalidDataException("Doctor " + doctor.getUser().getFirstName() + " cannot be deactivated because he has scheduled appointments");
        }

        doctor.setActive(false);
        doctor.getUser().setActive(false);
        tokenService.revokeAllUserTokens(doctor.getUser());

        doctorRepository.save(doctor);
    }
}
