package com.hospital.backendHospital.models.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resumen para el panel de administracion y recepcion. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardDto {
    private long activePatients;
    private long activeDoctors;
    private long appointmentsToday;
    private long pendingAppointments;
    private long activeHospitalizations;
    private long totalBeds;
    private long occupiedBeds;
    private long lowStockSupplies;
}
