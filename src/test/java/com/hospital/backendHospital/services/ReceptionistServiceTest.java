package com.hospital.backendHospital.services;

import com.hospital.backendHospital.auth.service.TokenService;

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
import com.hospital.backendHospital.services.impl.ReceptionistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReceptionistServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private ReceptionistMapper receptionistMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;

    @InjectMocks
    private ReceptionistService receptionistService;

    private CreateReceptionistDto createDto;
    private Role roleReceptionist;
    private User receptionist;

    @BeforeEach
    void setUp() {
        createDto = CreateReceptionistDto.builder()
                .email("email@gmail.com")
                .firstName("Margarita")
                .lastName("Gonzales")
                .password("12345678")
                .build();

        roleReceptionist = Role.builder()
                .roleEnum(RoleEnum.RECEPTIONIST)
                .build();

        receptionist = User.builder()
                .id(1L)
                .email(createDto.getEmail())
                .firstName(createDto.getFirstName())
                .lastName(createDto.getLastName())
                .password("encodedPassword")
                .roles(Set.of(roleReceptionist))
                .build();
    }

    @Test
    void testGetReceptionists() {
        List<User> receptionists = List.of(receptionist);
        List<ReceptionistResponseDto> receptionistDto = List.of(new ReceptionistResponseDto());

        when(userRepository.findByRoles_RoleEnum(RoleEnum.RECEPTIONIST)).thenReturn(receptionists);
        when(receptionistMapper.toListDto(receptionists)).thenReturn(receptionistDto);

        List<ReceptionistResponseDto> result = receptionistService.getReceptionists();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(receptionistDto, result);

        verify(userRepository).findByRoles_RoleEnum(RoleEnum.RECEPTIONIST);
        verify(receptionistMapper).toListDto(receptionists);
    }

    @Test
    void testGetReceptionists_ShouldThrowEntityNotFound() {
        List<User> emptyReceptionistList = Collections.emptyList();

        when(userRepository.findByRoles_RoleEnum(RoleEnum.RECEPTIONIST))
                .thenReturn(emptyReceptionistList);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> receptionistService.getReceptionists());

        assertEquals("There is not receptionists yet", exception.getMessage());
        verify(userRepository).findByRoles_RoleEnum(RoleEnum.RECEPTIONIST);
        verifyNoInteractions(receptionistMapper);
    }

    @Test
    void testCreateReceptionist() {
        ReceptionistResponseDto responseDto = ReceptionistResponseDto.builder()
                .id(1L)
                .email(createDto.getEmail())
                .firstName(createDto.getFirstName())
                .lastName(createDto.getLastName())
                .build();

        when(userRepository.existsByEmail(createDto.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleEnum(RoleEnum.RECEPTIONIST)).thenReturn(Optional.of(roleReceptionist));
        when(passwordEncoder.encode(createDto.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(receptionist);
        when(receptionistMapper.toDto(any(User.class))).thenReturn(responseDto);

        ReceptionistResponseDto result = receptionistService.createReceptionist(createDto);

        assertNotNull(result);
        assertEquals(responseDto, result);

        verify(userRepository).existsByEmail(createDto.getEmail());
        verify(roleRepository).findByRoleEnum(RoleEnum.RECEPTIONIST);
        verify(passwordEncoder).encode(createDto.getPassword());
        verify(userRepository).save(any(User.class));
        verify(receptionistMapper).toDto(any(User.class));
    }

    @Test
    void testCreateReceptionist_ShouldThrowEntityNotFound() {
        when(roleRepository.findByRoleEnum(RoleEnum.RECEPTIONIST)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, ()-> receptionistService.createReceptionist(createDto));

        assertEquals("Role not found", exception.getMessage());
        verify(roleRepository).findByRoleEnum(RoleEnum.RECEPTIONIST);
        verifyNoInteractions(receptionistMapper);
    }

    @Test
    void testCreateReceptionist_ShouldThrowInvalidDataException() {
        when(userRepository.existsByEmail(createDto.getEmail())).thenReturn(true);

        InvalidDataException exception = assertThrows(InvalidDataException.class, ()-> receptionistService.createReceptionist(createDto));

        assertEquals("Receptionist already exists", exception.getMessage());
        verify(userRepository).existsByEmail(createDto.getEmail());
    }

    @Test
    void testUpdateReceptionist() {
        UpdateReceptionistDto updateDto = UpdateReceptionistDto.builder()
                .email("newEmail@gmail.com")
                .firstName("newName")
                .lastName("newLastName")
                .password("newPassword")
                .build();

        ReceptionistResponseDto expectedResponse = ReceptionistResponseDto.builder()
                .id(1L)
                .email("newEmail@gmail.com")
                .firstName("newName")
                .lastName("newLastName")
                .build();

        when(userRepository.findById(receptionist.getId())).thenReturn(Optional.of(receptionist));
        when(passwordEncoder.encode(updateDto.getPassword())).thenReturn("encodedNewPass");
        when(receptionistMapper.toDto(receptionist)).thenReturn(expectedResponse);

        ReceptionistResponseDto result = receptionistService.updateReceptionist(receptionist.getId(), updateDto);

        assertEquals("newEmail@gmail.com", receptionist.getEmail());
        assertEquals("newName", result.getFirstName());
        assertEquals("newLastName", result.getLastName());
        assertEquals(expectedResponse.getEmail(), result.getEmail());

        verify(userRepository).save(receptionist);
    }

    @Test
    void testUpdateReceptionist_ShouldThrowEntityNotFound() {
        UpdateReceptionistDto updateDto = UpdateReceptionistDto.builder()
                .email("newEmail@gmail.com")
                .firstName("newName")
                .lastName("newLastName")
                .password("newPassword")
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, ()-> receptionistService.updateReceptionist(2L, updateDto));

        assertEquals("Receptionist not found with id " + 2, exception.getMessage());
        verify(userRepository).findById(2L);
    }

    @Test
    void testDeactivateReceptionist() {
        when(userRepository.findById(receptionist.getId())).thenReturn(Optional.of(receptionist));
        when(userRepository.save(receptionist)).thenReturn(receptionist);

        receptionistService.deactivateReceptionist(receptionist.getId());

        assertFalse(receptionist.isActive());
        verify(userRepository).save(receptionist);
        verify(tokenService).revokeAllUserTokens(receptionist);
    }

    @Test
    void testDeactivateReceptionist_ShouldThrowEntityNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, ()-> receptionistService.deactivateReceptionist(2L));

        assertEquals("Receptionist not found with id " + 2, exception.getMessage());
    }
}
