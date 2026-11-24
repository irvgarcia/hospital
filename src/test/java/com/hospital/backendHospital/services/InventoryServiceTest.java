package com.hospital.backendHospital.services;

import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.SupplyMovementMapper;
import com.hospital.backendHospital.mappers.SupplyUsageMapper;
import com.hospital.backendHospital.models.dto.supplyMovement.CreateSupplyMovementDto;
import com.hospital.backendHospital.models.dto.supplyUsage.CreateSupplyUsageDto;
import com.hospital.backendHospital.models.entity.*;
import com.hospital.backendHospital.repositories.HospitalizationRepository;
import com.hospital.backendHospital.repositories.MedicalSupplyRepository;
import com.hospital.backendHospital.repositories.SupplyMovementRepository;
import com.hospital.backendHospital.repositories.SupplyUsageRepository;
import com.hospital.backendHospital.services.impl.SupplyMovementService;
import com.hospital.backendHospital.services.impl.SupplyUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** A7 y M11: consumos y movimientos aplican la misma regla de stock. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceTest {

    @Mock private SupplyUsageRepository supplyUsageRepository;
    @Mock private SupplyUsageMapper supplyUsageMapper;
    @Mock private HospitalizationRepository hospitalizationRepository;
    @Mock private MedicalSupplyRepository medicalSupplyRepository;
    @Mock private SupplyMovementRepository supplyMovementRepository;
    @Mock private SupplyMovementMapper supplyMovementMapper;

    private SupplyUsageService usageService;
    private SupplyMovementService movementService;
    private MedicalSupply supply;
    private Hospitalization hospitalization;
    private final User doctorUser = TestData.user(1, RoleEnum.DOCTOR);

    @BeforeEach
    void setUp() {
        usageService = new SupplyUsageService(supplyUsageRepository, supplyUsageMapper, hospitalizationRepository,
                medicalSupplyRepository, supplyMovementRepository);
        movementService = new SupplyMovementService(supplyMovementRepository, medicalSupplyRepository, supplyMovementMapper);
        supply = TestData.supply(4, 10, 5);
        hospitalization = Hospitalization.builder().id(7L).patient(TestData.patient(2)).doctor(TestData.doctor(1))
                .room(TestData.room(3, 1)).admissionDate(LocalDate.now()).reason("x").costPerDay(BigDecimal.TEN).build();
        when(medicalSupplyRepository.findById(4L)).thenReturn(Optional.of(supply));
        when(hospitalizationRepository.findById(7L)).thenReturn(Optional.of(hospitalization));
    }

    @Test
    void usageCanTakeTheStockDownToZeroAndRegistersAnExit() {
        usageService.registerUsage(doctorUser, new CreateSupplyUsageDto(7L, 4L, 10));
        assertThat(supply.getQuantity()).isZero();
        verify(supplyMovementRepository).save(argThat(m -> m.getType() == MovementType.OUT && m.getQuantityChanged() == 10));
    }

    @Test
    void usageCannotExceedStock() {
        assertThatThrownBy(() -> usageService.registerUsage(doctorUser, new CreateSupplyUsageDto(7L, 4L, 11)))
                .isInstanceOf(InvalidDataException.class).hasMessageContaining("disponible: 10");
    }

    @Test
    void usageRequiresActiveSupplyAndHospitalization() {
        supply.setActive(false);
        assertThatThrownBy(() -> usageService.registerUsage(doctorUser, new CreateSupplyUsageDto(7L, 4L, 1)))
                .hasMessageContaining("desactivado");
        supply.setActive(true);
        hospitalization.setActive(false);
        assertThatThrownBy(() -> usageService.registerUsage(doctorUser, new CreateSupplyUsageDto(7L, 4L, 1)))
                .hasMessageContaining("terminó");
    }

    @Test
    void usageListIsFilteredByHospitalization() {
        when(hospitalizationRepository.existsById(7L)).thenReturn(true);
        usageService.getUsageByHospitalization(7L);
        verify(supplyUsageRepository).findByHospitalizationIdOrderByUsedAtDesc(7L);
        verify(supplyUsageRepository, never()).findAll();
    }

    @Test
    void manualExitBelowMinimumIsAllowedButNotBelowZero() {
        movementService.registerMovement(doctorUser, new CreateSupplyMovementDto(4L, MovementType.OUT, 8));
        assertThat(supply.getQuantity()).isEqualTo(2);
        assertThatThrownBy(() -> movementService.registerMovement(doctorUser, new CreateSupplyMovementDto(4L, MovementType.OUT, 3)))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void entryIncreasesStock() {
        movementService.registerMovement(doctorUser, new CreateSupplyMovementDto(4L, MovementType.IN, 15));
        assertThat(supply.getQuantity()).isEqualTo(25);
    }
}
