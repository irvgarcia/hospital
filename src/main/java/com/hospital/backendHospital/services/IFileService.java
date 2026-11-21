package com.hospital.backendHospital.services;

import com.hospital.backendHospital.models.dto.file.FileResponseDto;
import com.hospital.backendHospital.models.entity.FileEntity;
import com.hospital.backendHospital.models.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IFileService {

    List<FileResponseDto> listFiles(User user, Long patientId);

    FileResponseDto store(User user, MultipartFile file, Long patientId);

    FileEntity getFile(User user, UUID id);

    void deleteFile(User user, UUID id);
}
