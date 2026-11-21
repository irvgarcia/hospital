package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.room.CreateRoomDto;
import com.hospital.backendHospital.models.dto.room.RoomResponseDto;
import com.hospital.backendHospital.models.dto.room.UpdateRoomDto;

import java.util.List;

public interface IRoomService {

    List<RoomResponseDto> listRooms(Boolean available);

    RoomResponseDto getRoom(Long id);

    RoomResponseDto createRoom(CreateRoomDto createRoomDto);

    RoomResponseDto updateRoom(Long id, UpdateRoomDto updateRoomDto);
}
