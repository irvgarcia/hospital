package com.hospital.backendHospital.services;

import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.MedicalSupplyMapper;
import com.hospital.backendHospital.mappers.RoomMapper;
import com.hospital.backendHospital.mappers.SpecialtyMapper;
import com.hospital.backendHospital.models.dto.medicalSupply.UpdateMedicalSupplyDto;
import com.hospital.backendHospital.models.dto.room.CreateRoomDto;
import com.hospital.backendHospital.models.dto.room.UpdateRoomDto;
import com.hospital.backendHospital.models.dto.specialty.CreateSpecialtyDto;
import com.hospital.backendHospital.models.dto.specialty.UpdateSpecialtyDto;
import com.hospital.backendHospital.models.entity.MedicalSupply;
import com.hospital.backendHospital.models.entity.Room;
import com.hospital.backendHospital.models.entity.Specialty;
import com.hospital.backendHospital.repositories.*;
import com.hospital.backendHospital.services.impl.MedicalSupplyService;
import com.hospital.backendHospital.services.impl.RoomService;
import com.hospital.backendHospital.services.impl.SpecialtyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** M9: CRUD de especialidades, salas e insumos. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogServiceTest {

    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private SpecialtyMapper specialtyMapper;
    @Mock private RoomRepository roomRepository;
    @Mock private HospitalizationRepository hospitalizationRepository;
    @Mock private RoomMapper roomMapper;
    @Mock private MedicalSupplyRepository medicalSupplyRepository;
    @Mock private MedicalSupplyMapper medicalSupplyMapper;

    private SpecialtyService specialtyService;
    private RoomService roomService;
    private MedicalSupplyService supplyService;

    @BeforeEach
    void setUp() {
        specialtyService = new SpecialtyService(specialtyRepository, doctorRepository, specialtyMapper);
        roomService = new RoomService(roomRepository, hospitalizationRepository, roomMapper);
        supplyService = new MedicalSupplyService(medicalSupplyRepository, medicalSupplyMapper);
        when(roomRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void duplicateSpecialtyIsAConflict() {
        when(specialtyRepository.existsByName("Cardiologia")).thenReturn(true);
        assertThatThrownBy(() -> specialtyService.createSpecialty(new CreateSpecialtyDto(" Cardiologia ", "x", BigDecimal.ONE)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void specialtyWithActiveDoctorsCannotBeDeactivated() {
        Specialty specialty = TestData.specialty();
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(specialty));
        when(doctorRepository.countBySpecialtyIdAndActiveTrue(1L)).thenReturn(2L);
        assertThatThrownBy(() -> specialtyService.updateSpecialty(1L, UpdateSpecialtyDto.builder().active(false).build()))
                .isInstanceOf(InvalidDataException.class);

        when(doctorRepository.countBySpecialtyIdAndActiveTrue(1L)).thenReturn(0L);
        specialtyService.updateSpecialty(1L, UpdateSpecialtyDto.builder().active(false).cost(BigDecimal.TEN).build());
        assertThat(specialty.isActive()).isFalse();
        assertThat(specialty.getCost()).isEqualByComparingTo("10");
    }

    @Test
    void roomCapacityCannotGoBelowOccupiedBeds() {
        Room room = TestData.room(3, 4);
        when(roomRepository.findById(3L)).thenReturn(Optional.of(room));
        when(hospitalizationRepository.countByRoomIdAndActiveTrue(3L)).thenReturn(3L);

        assertThatThrownBy(() -> roomService.updateRoom(3L, new UpdateRoomDto(null, 2))).isInstanceOf(InvalidDataException.class);
        roomService.updateRoom(3L, new UpdateRoomDto(null, 3));
        assertThat(room.getCapacity()).isEqualTo(3);
        assertThat(room.isAvailable()).isFalse();
    }

    @Test
    void duplicateRoomNameIsAConflict() {
        when(roomRepository.existsByName("Sala A")).thenReturn(true);
        assertThatThrownBy(() -> roomService.createRoom(new CreateRoomDto("Sala A", 2))).isInstanceOf(ConflictException.class);
    }

    @Test
    void supplyUpdateKeepsQuantity() {
        MedicalSupply supply = TestData.supply(4, 10, 5);
        when(medicalSupplyRepository.findById(4L)).thenReturn(Optional.of(supply));
        supplyService.updateSupply(4L, UpdateMedicalSupplyDto.builder().minimumStock(20).active(false).build());
        assertThat(supply.getQuantity()).isEqualTo(10);
        assertThat(supply.getMinimumStock()).isEqualTo(20);
        assertThat(supply.isActive()).isFalse();
    }
}
