package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.DoctorScheduleMapper;
import com.hospital.backendHospital.models.dto.doctorSchedule.CreateDoctorScheduleDto;
import com.hospital.backendHospital.models.dto.doctorSchedule.DoctorScheduleResponseDto;
import com.hospital.backendHospital.models.dto.doctorSchedule.UpdateDoctorScheduleDto;
import com.hospital.backendHospital.models.entity.Appointment;
import com.hospital.backendHospital.models.entity.AppointmentStatus;
import com.hospital.backendHospital.models.entity.Doctor;
import com.hospital.backendHospital.models.entity.DoctorSchedule;
import com.hospital.backendHospital.models.filters.DoctorScheduleFilterRequest;
import com.hospital.backendHospital.repositories.AppointmentRepository;
import com.hospital.backendHospital.repositories.DoctorRepository;
import com.hospital.backendHospital.repositories.DoctorScheduleRepository;
import com.hospital.backendHospital.services.IDoctorScheduleService;
import com.hospital.backendHospital.services.validation.ScheduleValidator;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorScheduleService implements IDoctorScheduleService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorScheduleMapper doctorScheduleMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorScheduleResponseDto> filterDoctorSchedules(DoctorScheduleFilterRequest filter, Pageable pageable) {
        Specification<DoctorSchedule> spec = Specification.where(null);
        if (filter.getDoctorId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("doctor").get("id"), filter.getDoctorId()));
        }
        if (filter.getDayOfWeek() != null){
            spec = spec.and((root, query, cb) -> cb.equal(root.get("dayOfWeek"), filter.getDayOfWeek()));
        }
        if (filter.getStartTime() != null){
            spec = spec.and((root, query, cb) -> cb.equal(root.get("startTime"), filter.getStartTime()));
        }
        boolean byFirstName = filter.getFirstName() != null && !filter.getFirstName().isBlank();
        boolean byLastName = filter.getLastName() != null && !filter.getLastName().isBlank();
        if (byFirstName || byLastName) {
            // M5: un solo join a doctor -> user para ambos filtros
            spec = spec.and((root, query, cb) -> {
                Join<Object, Object> userJoin = root.join("doctor", JoinType.INNER).join("user", JoinType.INNER);
                var predicate = cb.conjunction();
                if (byFirstName) {
                    predicate = cb.and(predicate, cb.like(cb.lower(userJoin.get("firstName")), "%" + filter.getFirstName().trim().toLowerCase() + "%"));
                }
                if (byLastName) {
                    predicate = cb.and(predicate, cb.like(cb.lower(userJoin.get("lastName")), "%" + filter.getLastName().trim().toLowerCase() + "%"));
                }
                return predicate;
            });
        }
        return doctorScheduleRepository.findAll(spec, pageable).map(doctorScheduleMapper::toResponseDto);
    }

    @Override
    @Transactional
    public DoctorScheduleResponseDto createDoctorSchedule(CreateDoctorScheduleDto dto) {
        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new EntityNotFoundException("No existe el doctor con id " + dto.getDoctorId()));
        ScheduleValidator.validate(dto.getDayOfWeek(), dto.getStartTime(), dto.getEndTime());
        if (doctorScheduleRepository.existsByDoctorIdAndDayOfWeek(doctor.getId(), dto.getDayOfWeek())){
            throw new ConflictException("El doctor ya tiene horario el " + dto.getDayOfWeek());
        }
        DoctorSchedule doctorSchedule = DoctorSchedule.builder()
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .doctor(doctor)
                .build();
        doctorScheduleRepository.save(doctorSchedule);
        return doctorScheduleMapper.toResponseDto(doctorSchedule);
    }

    @Override
    @Transactional
    public DoctorScheduleResponseDto updateDoctorSchedule(Long id, UpdateDoctorScheduleDto dto) {
        DoctorSchedule schedule = findSchedule(id);
        ScheduleValidator.validate(schedule.getDayOfWeek(), dto.getStartTime(), dto.getEndTime());
        ensureNoPendingAppointmentsOutside(schedule, dto.getStartTime(), dto.getEndTime());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        doctorScheduleRepository.save(schedule);
        return doctorScheduleMapper.toResponseDto(schedule);
    }

    @Override
    @Transactional
    public void deleteDoctorSchedule(Long id) {
        DoctorSchedule schedule = findSchedule(id);
        ensureNoPendingAppointmentsOutside(schedule, null, null);
        schedule.getDoctor().getSchedules().remove(schedule);
        doctorScheduleRepository.delete(schedule);
    }

    /** Las citas pendientes de ese dia deben seguir dentro del turno (start/end nulos: el turno desaparece). */
    private void ensureNoPendingAppointmentsOutside(DoctorSchedule schedule, LocalTime start, LocalTime end) {
        List<Appointment> pending = appointmentRepository.findByDoctorIdAndStatusAndDateGreaterThanEqual(
                schedule.getDoctor().getId(), AppointmentStatus.PENDING, LocalDate.now());
        for (Appointment appointment : pending) {
            if (appointment.getDate().getDayOfWeek() != schedule.getDayOfWeek()) {
                continue;
            }
            boolean fits = start != null && !appointment.getStartTime().isBefore(start) && !appointment.getEndTime().isAfter(end);
            if (!fits) {
                throw new InvalidDataException("El cambio deja fuera la cita pendiente del " + appointment.getDate()
                        + " a las " + appointment.getStartTime() + "; cancélala o reprográmala primero");
            }
        }
    }

    private DoctorSchedule findSchedule(Long id) {
        return doctorScheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe el horario con id " + id));
    }
}
