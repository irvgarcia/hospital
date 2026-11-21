package com.hospital.backendHospital.auth.service;

import com.hospital.backendHospital.auth.repository.Token.TokenType;
import com.hospital.backendHospital.exceptions.ConflictException;
import com.hospital.backendHospital.exceptions.EntityNotFoundException;
import com.hospital.backendHospital.exceptions.InvalidTokenException;
import com.hospital.backendHospital.models.dto.token.AuthRequest;
import com.hospital.backendHospital.models.dto.token.TokenResponse;
import com.hospital.backendHospital.models.dto.user.CreateUserDto;
import com.hospital.backendHospital.models.entity.Role;
import com.hospital.backendHospital.models.entity.RoleEnum;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.repositories.RoleRepository;
import com.hospital.backendHospital.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;

    /** Registro publico: crea un usuario PATIENT. El perfil clinico se completa despues (POST /api/v1/patients). */
    @Transactional
    public TokenResponse register(final CreateUserDto request){
        String email = normalize(request.getEmail());
        if (userRepository.existsByEmail(email)){
            throw new ConflictException("Ya existe una cuenta con el email " + email);
        }
        Role patientRole = roleRepository.findByRoleEnum(RoleEnum.PATIENT)
                .orElseThrow(()-> new EntityNotFoundException("No existe el rol PATIENT"));

        final User user = User.builder()
                .email(email)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(new HashSet<>(Set.of(patientRole)))
                .build();

        return issueTokens(userRepository.save(user));
    }

    @Transactional
    public TokenResponse authenticate(final AuthRequest request){
        String email = normalize(request.getEmail());
        // Lanza BadCredentialsException (401) o DisabledException (403) si no procede
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        final User user = userRepository.findByEmail(email).orElseThrow(()-> new UsernameNotFoundException("Email not found"));
        return issueTokens(user);
    }

    /**
     * Recibe el refresh token en el header Authorization y entrega un par nuevo (rotacion):
     * el refresh usado queda revocado y no se puede reutilizar.
     */
    @Transactional
    public TokenResponse refreshToken(final String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new InvalidTokenException("Falta el refresh token");
        }
        final String refreshToken = authorizationHeader.substring(BEARER_PREFIX.length());

        final String username = jwtService.extractUsername(refreshToken, TokenType.REFRESH)
                .orElseThrow(() -> new InvalidTokenException("Refresh token no válido"));

        if (!tokenService.isActive(refreshToken, TokenType.REFRESH)) {
            throw new InvalidTokenException("El refresh token fue revocado");
        }

        final User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new InvalidTokenException("Refresh token no válido"));

        if (!user.isActive()) {
            throw new DisabledException("User is disabled");
        }

        return issueTokens(user);
    }

    /** Revoca los tokens vigentes del usuario y emite un par nuevo (una sola sesion activa por usuario). */
    private TokenResponse issueTokens(final User user) {
        final String accessToken = jwtService.generateToken(user);
        final String refreshToken = jwtService.generateRefreshToken(user);

        tokenService.revokeAllUserTokens(user);
        tokenService.save(user, accessToken, TokenType.ACCESS, jwtService.expiresAt(TokenType.ACCESS));
        tokenService.save(user, refreshToken, TokenType.REFRESH, jwtService.expiresAt(TokenType.REFRESH));

        return new TokenResponse(accessToken, refreshToken);
    }

    public User getAuthenticatedUser(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
