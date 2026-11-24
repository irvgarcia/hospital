package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.RoomMapper;
import com.hospital.backendHospital.models.dto.room.CreateRoomDto;
import com.hospital.backendHospital.models.dto.room.RoomResponseDto;
import com.hospital.backendHospital.models.dto.room.UpdateRoomDto;
import com.hospital.backendHospital.models.entity.Room;
import com.hospital.backendHospital.repositories.HospitalizationRepository;
import com.hospital.backendHospital.repositories.RoomRepository;
import com.hospital.backendHospital.services.IRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService implements IRoomService {

    private final RoomRepository roomRepository;
    private final HospitalizationRepository hospitalizationRepository;
    private final RoomMapper roomMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDto> listRooms(Boolean available) {
        List<Room> rooms = available == null
                ? roomRepository.findAllByOrderByNameAsc()
                : roomRepository.findByAvailableOrderByNameAsc(available);
        return roomMapper.toListDtos(rooms);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponseDto getRoom(Long id) {
        return roomMapper.toResponseDto(findRoom(id));
    }

    @Override
    @Transactional
    public RoomResponseDto createRoom(CreateRoomDto dto) {
        String name = dto.getName().trim();
        if (roomRepository.existsByName(name)) {
            throw new ConflictException("Ya existe una sala con el nombre " + name);
        }
        Room room = Room.builder()
                .name(name)
                .capacity(dto.getCapacity())
                .available(true)
                .build();
        return roomMapper.toResponseDto(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponseDto updateRoom(Long id, UpdateRoomDto dto) {
        Room room = findRoom(id);
        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (roomRepository.existsByNameAndIdNot(name, id)) {
                throw new ConflictException("Ya existe una sala con el nombre " + name);
            }
            room.setName(name);
        }
        if (dto.getCapacity() != null) {
            long occupied = hospitalizationRepository.countByRoomIdAndActiveTrue(id);
            if (dto.getCapacity() < occupied) {
                throw new InvalidDataException("La sala tiene " + occupied + " pacientes; la capacidad no puede ser menor");
            }
            room.setCapacity(dto.getCapacity());
            room.setAvailable(occupied < dto.getCapacity());
        }
        return roomMapper.toResponseDto(roomRepository.save(room));
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No existe la sala con id " + id));
    }
}
