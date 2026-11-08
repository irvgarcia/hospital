package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.mappers.ReceptionistMapper;
import com.hospital.backendHospital.models.dto.recepcionist.CreateReceptionistDto;
import com.hospital.backendHospital.models.dto.recepcionist.ReceptionistResponseDto;
import com.hospital.backendHospital.models.dto.recepcionist.UpdateReceptionistDto;
import com.hospital.backendHospital.models.entity.Role;
import com.hospital.backendHospital.models.entity.RoleEnum;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.repositories.RoleRepository;
import com.hospital.backendHospital.repositories.UserRepository;
import com.hospital.backendHospital.services.IReceptionistService;
import com.hospital.backendHospital.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReceptionistService implements IReceptionistService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ReceptionistMapper receptionistMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional(readOnly = true)
    public List<ReceptionistResponseDto> getReceptionists() {
        List<User> receptionists = userRepository.findByRoles_RoleEnum(RoleEnum.RECEPTIONIST);

        if (receptionists.isEmpty()) {
            throw new EntityNotFoundException("There is not receptionists yet");
        }

        return receptionistMapper.toListDto(receptionists);
    }

    @Override
    @Transactional
    public ReceptionistResponseDto createReceptionist(CreateReceptionistDto createReceptionistDto) {

        if (userRepository.existsByEmail(createReceptionistDto.getEmail())){
            throw new InvalidDataException("Receptionist already exists");
        }

        Role recepcionistRole = roleRepository.findByRoleEnum(RoleEnum.RECEPTIONIST).orElseThrow(()-> new EntityNotFoundException("Role not found"));

        User receptionist = User.builder()
                .email(createReceptionistDto.getEmail())
                .firstName(createReceptionistDto.getFirstName())
                .lastName(createReceptionistDto.getLastName())
                .password(passwordEncoder.encode(createReceptionistDto.getPassword()))
                .roles(Set.of(recepcionistRole))
                .build();

        userRepository.save(receptionist);

        return receptionistMapper.toDto(receptionist);
    }

    @Override
    @Transactional
    public ReceptionistResponseDto updateReceptionist(Long id, UpdateReceptionistDto updateReceptionistDto) {
        User receptionist = userRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Receptionist not found with id " + id));

        if (updateReceptionistDto.getEmail() != null){
            receptionist.setEmail(updateReceptionistDto.getEmail());
        }

        if (updateReceptionistDto.getFirstName() != null){
            receptionist.setFirstName(updateReceptionistDto.getFirstName());
        }

        if (updateReceptionistDto.getLastName() != null){
            receptionist.setLastName(updateReceptionistDto.getLastName());
        }

        if (updateReceptionistDto.getPassword() != null){
            receptionist.setPassword(passwordEncoder.encode(updateReceptionistDto.getPassword()));
        }

        userRepository.save(receptionist);

        return receptionistMapper.toDto(receptionist);
    }

    @Override
    @Transactional
    public void deactivateReceptionist(Long id) {
        User receptionist = userRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Receptionist not found with id " + id));

        receptionist.setActive(false);
        tokenService.revokeAllUserTokens(receptionist);

        userRepository.save(receptionist);
    }
}
