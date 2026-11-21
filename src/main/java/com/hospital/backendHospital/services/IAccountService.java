package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.account.AccountResponseDto;
import com.hospital.backendHospital.models.dto.account.ChangePasswordDto;
import com.hospital.backendHospital.models.entity.User;

public interface IAccountService {

    AccountResponseDto getAccount(User user);

    void changePassword(User user, ChangePasswordDto changePasswordDto);
}
