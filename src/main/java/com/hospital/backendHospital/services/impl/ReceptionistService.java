package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.auth.service.TokenService;
import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.mappers.ReceptionistMapper;
import com.hospital.backendHospital.models.dto.receptionist.CreateReceptionistDto;
import com.hospital.backendHospital.models.dto.receptionist.ReceptionistResponseDto;
import com.hospital.backendHospital.models.dto.receptionist.UpdateReceptionistDto;
import com.hospital.backendHospital.models.entity.Role;
import com.hospital.backendHospital.models.entity.RoleEnum;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.repositories.RoleRepository;
import com.hospital.backendHospital.repositories.UserRepository;
import com.hospital.backendHospital.services.IReceptionistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
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
        // M3: sin recepcionistas la lista viene vacia (200), no es un error
        List<User> receptionists = userRepository.findByRoles_RoleEnum(RoleEnum.RECEPTIONIST).stream()
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .toList();
        return receptionistMapper.toListDto(receptionists);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceptionistResponseDto getReceptionist(Long id) {
        return receptionistMapper.toDto(findReceptionist(id));
    }

    @Override
    @Transactional
    public ReceptionistResponseDto createReceptionist(CreateReceptionistDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)){
            throw new ConflictException("Ya existe un usuario con el email " + email);
        }
        Role role = roleRepository.findByRoleEnum(RoleEnum.RECEPTIONIST)
                .orElseThrow(() -> new EntityNotFoundException("No existe el rol RECEPTIONIST"));
        User receptionist = User.builder()
                .email(email)
                .firstName(dto.getFirstName().trim())
                .lastName(dto.getLastName().trim())
                .password(passwordEncoder.encode(dto.getPassword()))
                .roles(new HashSet<>(Set.of(role)))
                .build();
        userRepository.save(receptionist);
        return receptionistMapper.toDto(receptionist);
    }

    @Override
    @Transactional
    public ReceptionistResponseDto updateReceptionist(Long id, UpdateReceptionistDto dto) {
        User receptionist = findReceptionist(id);
        if (dto.getEmail() != null) {
            String email = dto.getEmail().trim().toLowerCase();
            // A9: el email es unico; antes el choque terminaba en un 500 por la restriccion de la base
            if (userRepository.existsByEmailAndIdNot(email, id)) {
                throw new ConflictException("Ya existe un usuario con el email " + email);
            }
            if (!email.equals(receptionist.getEmail())) {
                receptionist.setEmail(email);
                tokenService.revokeAllUserTokens(receptionist);
            }
        }
        if (dto.getFirstName() != null) receptionist.setFirstName(dto.getFirstName().trim());
        if (dto.getLastName() != null) receptionist.setLastName(dto.getLastName().trim());
        if (dto.getPassword() != null) receptionist.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(receptionist);
        return receptionistMapper.toDto(receptionist);
    }

    @Override
    @Transactional
    public void deactivateReceptionist(Long id) {
        User receptionist = findReceptionist(id);
        receptionist.setActive(false);
        tokenService.revokeAllUserTokens(receptionist);
        userRepository.save(receptionist);
    }

    @Override
    @Transactional
    public void activateReceptionist(Long id) {
        User receptionist = findReceptionist(id);
        receptionist.setActive(true);
        userRepository.save(receptionist);
    }

    /** A9: solo usuarios con rol RECEPTIONIST; antes se podia editar o desactivar a cualquier usuario (incluido el admin). */
    private User findReceptionist(Long id) {
        return userRepository.findById(id)
                .filter(user -> user.hasRole(RoleEnum.RECEPTIONIST))
                .orElseThrow(() -> new EntityNotFoundException("No existe el recepcionista con id " + id));
    }
}
