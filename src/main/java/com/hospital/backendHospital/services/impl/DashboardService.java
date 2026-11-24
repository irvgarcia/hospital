package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.models.dto.dashboard.DashboardDto;
import com.hospital.backendHospital.models.entity.AppointmentStatus;
import com.hospital.backendHospital.repositories.*;
import com.hospital.backendHospital.services.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService implements IDashboardService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final HospitalizationRepository hospitalizationRepository;
    private final RoomRepository roomRepository;
    private final MedicalSupplyRepository medicalSupplyRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardDto getDashboard() {
        LocalDate today = LocalDate.now();
        long activeHospitalizations = hospitalizationRepository.countByActiveTrue();
        return DashboardDto.builder()
                .activePatients(patientRepository.countByActiveTrue())
                .activeDoctors(doctorRepository.countByActiveTrue())
                .appointmentsToday(appointmentRepository.countByDateAndStatusNot(today, AppointmentStatus.CANCELLED))
                .pendingAppointments(appointmentRepository.countByStatusAndDateGreaterThanEqual(AppointmentStatus.PENDING, today))
                .activeHospitalizations(activeHospitalizations)
                .totalBeds(roomRepository.sumCapacity())
                .occupiedBeds(activeHospitalizations)
                .lowStockSupplies(medicalSupplyRepository.countLowStock())
                .build();
    }
}
