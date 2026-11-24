package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.models.dto.room.CreateRoomDto;
import com.hospital.backendHospital.models.dto.room.RoomResponseDto;
import com.hospital.backendHospital.models.dto.room.UpdateRoomDto;
import com.hospital.backendHospital.services.IRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final IRoomService roomService;

    /** available=true: solo salas con camas libres. */
    @GetMapping
    public List<RoomResponseDto> listRooms(@RequestParam(required = false) Boolean available) {
        return roomService.listRooms(available);
    }

    @GetMapping("/{id}")
    public RoomResponseDto getRoom(@PathVariable Long id) {
        return roomService.getRoom(id);
    }

    @PostMapping
    public ResponseEntity<RoomResponseDto> createRoom(@Valid @RequestBody CreateRoomDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(dto));
    }

    @PatchMapping("/{id}")
    public RoomResponseDto updateRoom(@PathVariable Long id, @Valid @RequestBody UpdateRoomDto dto) {
        return roomService.updateRoom(id, dto);
    }
}
