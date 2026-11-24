package com.hospital.backendHospital.auth.controller;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.token.AuthRequest;
import com.hospital.backendHospital.models.dto.token.TokenResponse;
import com.hospital.backendHospital.models.dto.user.CreateUserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Registro, login y refresh. El logout (POST /auth/logout) lo atiende la cadena de seguridad. */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody CreateUserDto request){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public TokenResponse authenticate(@Valid @RequestBody AuthRequest request){
        return authService.authenticate(request);
    }

    @PostMapping("/refresh-token")
    public TokenResponse refreshToken(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) final String authentication){
        return authService.refreshToken(authentication);
    }
}
