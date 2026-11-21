package com.hospital.backendHospital.services;

import com.hospital.backendHospital.auth.service.TokenService;
import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.mappers.ReceptionistMapper;
import com.hospital.backendHospital.models.dto.receptionist.CreateReceptionistDto;
import com.hospital.backendHospital.models.dto.receptionist.UpdateReceptionistDto;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** A9: solo recepcionistas, email unico, y M3: lista vacia. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReceptionistServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private ReceptionistMapper receptionistMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;

    @InjectMocks private ReceptionistService service;

    private User receptionist;

    @BeforeEach
    void setUp() {
        receptionist = TestData.user(3, RoleEnum.RECEPTIONIST);
        when(userRepository.findById(3L)).thenReturn(Optional.of(receptionist));
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestData.user(1, RoleEnum.ADMIN)));
        when(roleRepository.findByRoleEnum(RoleEnum.RECEPTIONIST)).thenReturn(Optional.of(Role.builder().roleEnum(RoleEnum.RECEPTIONIST).build()));
    }

    @Test
    void otherRolesCannotBeEditedOrDeactivated() {
        assertThatThrownBy(() -> service.updateReceptionist(1L, new UpdateReceptionistDto())).isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> service.deactivateReceptionist(1L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void emailMustBeUnique() {
        when(userRepository.existsByEmailAndIdNot("usado@test.com", 3L)).thenReturn(true);
        assertThatThrownBy(() -> service.updateReceptionist(3L, UpdateReceptionistDto.builder().email("usado@test.com").build()))
                .isInstanceOf(ConflictException.class);

        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(true);
        assertThatThrownBy(() -> service.createReceptionist(CreateReceptionistDto.builder().email("nuevo@test.com")
                .firstName("Ana").lastName("Gil").password("12345678").build())).isInstanceOf(ConflictException.class);
    }

    @Test
    void deactivationRevokesTokens() {
        service.deactivateReceptionist(3L);
        assertThat(receptionist.isActive()).isFalse();
        verify(tokenService).revokeAllUserTokens(receptionist);
        service.activateReceptionist(3L);
        assertThat(receptionist.isActive()).isTrue();
    }

    @Test
    void emptyListIsNotAnError() {
        when(userRepository.findByRoles_RoleEnum(RoleEnum.RECEPTIONIST)).thenReturn(List.of());
        when(receptionistMapper.toListDto(any())).thenReturn(List.of());
        assertThat(service.getReceptionists()).isEmpty();
    }
}
