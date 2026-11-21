package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.receptionist.CreateReceptionistDto;
import com.hospital.backendHospital.models.dto.receptionist.ReceptionistResponseDto;
import com.hospital.backendHospital.models.dto.receptionist.UpdateReceptionistDto;

import java.util.List;

public interface IReceptionistService {

    List<ReceptionistResponseDto> getReceptionists();

    ReceptionistResponseDto getReceptionist(Long id);

    ReceptionistResponseDto createReceptionist(CreateReceptionistDto createReceptionistDto);

    ReceptionistResponseDto updateReceptionist(Long id, UpdateReceptionistDto updateReceptionistDto);

    void deactivateReceptionist(Long id);

    void activateReceptionist(Long id);
}
