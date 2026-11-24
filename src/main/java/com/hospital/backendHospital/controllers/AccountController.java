package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.account.AccountResponseDto;
import com.hospital.backendHospital.models.dto.account.ChangePasswordDto;
import com.hospital.backendHospital.services.IAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Datos y contraseña del usuario autenticado (cualquier rol). */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account")
public class AccountController {

    private final IAccountService accountService;
    private final AuthService authService;

    @GetMapping
    public AccountResponseDto getAccount() {
        return accountService.getAccount(authService.getAuthenticatedUser());
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordDto dto) {
        accountService.changePassword(authService.getAuthenticatedUser(), dto);
        return ResponseEntity.noContent().build();
    }
}
