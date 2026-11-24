package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.exceptions.InvalidDataException;
import com.hospital.backendHospital.models.dto.account.AccountResponseDto;
import com.hospital.backendHospital.models.dto.account.ChangePasswordDto;
import com.hospital.backendHospital.models.entity.Doctor;
import com.hospital.backendHospital.models.entity.Patient;
import com.hospital.backendHospital.models.entity.User;
import com.hospital.backendHospital.repositories.DoctorRepository;
import com.hospital.backendHospital.repositories.PatientRepository;
import com.hospital.backendHospital.repositories.UserRepository;
import com.hospital.backendHospital.services.IAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService implements IAccountService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDto getAccount(User user) {
        Long patientId = patientRepository.findByUserId(user.getId()).map(Patient::getId).orElse(null);
        Long doctorId = doctorRepository.findByUser(user).map(Doctor::getId).orElse(null);
        List<String> roles = user.getRoles().stream().map(role -> role.getRoleEnum().name()).sorted().toList();
        boolean isPatient = roles.contains("PATIENT");
        return AccountResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roles)
                .patientId(patientId)
                .doctorId(doctorId)
                .profileCompleted(!isPatient || patientId != null)
                .build();
    }

    @Override
    @Transactional
    public void changePassword(User user, ChangePasswordDto dto) {
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidDataException("La contraseña actual no es correcta");
        }
        if (dto.getCurrentPassword().equals(dto.getNewPassword())) {
            throw new InvalidDataException("La contraseña nueva debe ser distinta de la actual");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }
}
