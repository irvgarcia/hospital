package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.HospitalizationMapper;
import com.hospital.backendHospital.models.dto.hospitalization.CreateHospitalizationDto;
import com.hospital.backendHospital.models.dto.hospitalization.HospitalizationResponseDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.models.filters.HospitalizationFilterRequest;
import com.hospital.backendHospital.repositories.DoctorRepository;
import com.hospital.backendHospital.repositories.HospitalizationRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.repositories.RoomRepository;
import com.hospital.backendHospital.services.IHospitalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HospitalizationService implements IHospitalizationService {

    private final HospitalizationRepository hospitalizationRepository;
    private final HospitalizationMapper hospitalizationMapper;
    private final PatientRepository patientRepository;
    private final RoomRepository roomRepository;
    private final DoctorRepository doctorRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<HospitalizationResponseDto> filterHospitalizations(HospitalizationFilterRequest filter, Pageable pageable) {
        // Specification es inmutable: cada and() devuelve una nueva y hay que reasignarla
        Specification<Hospitalization> spec = Specification.where(null);
        if (filter.getDate() != null){
            spec = spec.and((root, query, cb) -> cb.equal(root.get("admissionDate"), filter.getDate()));
        }
        if (filter.getPatientId() != null){
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), filter.getPatientId()));
        }
        if (filter.getDoctorId() != null){
            spec = spec.and((root, query, cb) -> cb.equal(root.get("doctor").get("id"), filter.getDoctorId()));
        }
        if (filter.getRoomId() != null){
            spec = spec.and((root, query, cb) -> cb.equal(root.get("room").get("id"), filter.getRoomId()));
        }
        if (filter.getStatus() != null){
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), filter.getStatus()));
        }
        return hospitalizationRepository.findAll(spec, pageable).map(hospitalizationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalizationResponseDto> listMine(User user) {
        Patient patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("Completa tu perfil de paciente antes de continuar"));
        return hospitalizationRepository.findByPatientIdOrderByAdmissionDateDesc(patient.getId()).stream()
                .map(hospitalizationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalizationResponseDto getHospitalization(Long id) {
        return hospitalizationMapper.toDto(findHospitalization(id));
    }

    @Override
    @Transactional
    public HospitalizationResponseDto createHospitalization(User user, CreateHospitalizationDto dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new EntityNotFoundException("No existe el paciente con id " + dto.getPatientId()));
        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("No existe la sala con id " + dto.getRoomId()));
        Doctor doctor = resolveResponsibleDoctor(user, dto);

        if (!patient.isActive()) {
            throw new InvalidDataException("El paciente está inactivo");
        }
        if (!doctor.isActive()){
            throw new InvalidDataException("El doctor está inactivo");
        }
        if (hospitalizationRepository.existsByPatientAndActiveTrue(patient)){
            throw new InvalidDataException("El paciente ya está hospitalizado");
        }
        // A6: la sala admite tantos pacientes como su capacidad
        long occupied = hospitalizationRepository.countByRoomIdAndActiveTrue(room.getId());
        if (occupied >= room.getCapacity()){
            throw new InvalidDataException("La sala " + room.getName() + " no tiene camas libres");
        }

        Hospitalization hospitalization = Hospitalization.builder()
                .patient(patient)
                .doctor(doctor)
                .room(room)
                .admissionDate(LocalDate.now())
                .reason(dto.getReason().trim())
                .costPerDay(dto.getCostPerDay())
                .build();
        hospitalizationRepository.save(hospitalization);

        room.setAvailable(occupied + 1 < room.getCapacity());
        roomRepository.save(room);
        return hospitalizationMapper.toDto(hospitalization);
    }

    @Override
    @Transactional
    public HospitalizationResponseDto discharge(Long id) {
        Hospitalization hospitalization = findHospitalization(id);
        // A6: un alta doble sobreescribia la fecha de la primera
        if (!hospitalization.isActive()) {
            throw new InvalidDataException("El paciente ya fue dado de alta el " + hospitalization.getDischargeDate());
        }
        LocalDate today = LocalDate.now();
        hospitalization.setDischargeDate(today);
        hospitalization.setActive(false);
        hospitalization.setTotalCost(totalCost(hospitalization.getCostPerDay(), hospitalization.getAdmissionDate(), today));
        hospitalization.getRoom().setAvailable(true);
        hospitalizationRepository.save(hospitalization);
        return hospitalizationMapper.toDto(hospitalization);
    }

    /** Dias de estancia por costo diario; el dia de ingreso cuenta como uno aunque el alta sea el mismo dia. */
    static BigDecimal totalCost(BigDecimal costPerDay, LocalDate admission, LocalDate discharge) {
        long days = Math.max(1, ChronoUnit.DAYS.between(admission, discharge));
        return costPerDay.multiply(BigDecimal.valueOf(days));
    }

    /**
     * Doctor responsable: el indicado en doctorId; si no viene y quien hospitaliza es un DOCTOR,
     * queda el mismo. ADMIN y RECEPTIONIST deben indicarlo siempre.
     */
    private Doctor resolveResponsibleDoctor(User user, CreateHospitalizationDto dto) {
        if (dto.getDoctorId() != null) {
            return doctorRepository.findById(dto.getDoctorId())
                    .orElseThrow(() -> new EntityNotFoundException("No existe el doctor con id " + dto.getDoctorId()));
        }
        if (user.hasRole(RoleEnum.DOCTOR)) {
            return doctorRepository.findByUser(user)
                    .orElseThrow(() -> new EntityNotFoundException("No existe el perfil de doctor del usuario"));
        }
        throw new InvalidDataException("El doctor responsable es obligatorio");
    }

    private Hospitalization findHospitalization(Long id) {
        return hospitalizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe la hospitalización con id " + id));
    }
}
