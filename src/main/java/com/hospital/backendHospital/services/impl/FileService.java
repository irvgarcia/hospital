package com.hospital.backendHospital.services.impl;

import com.hospital.backendHospital.models.dto.fileEntity.FileResponseDto;
import com.hospital.backendHospital.models.entity.FileEntity;
import com.hospital.backendHospital.repositories.FileRepository;
import com.hospital.backendHospital.services.IFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService implements IFileService {

    private final FileRepository fileRepository;

    @Override
    public FileEntity store(MultipartFile file) throws IOException {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        FileEntity fileEntity = FileEntity.builder()
                .name(fileName)
                .type(file.getContentType())
                .data(file.getBytes())
                .build();
        return fileRepository.save(fileEntity);
    }

    @Override
    public Optional<FileEntity> getFile(UUID id) throws FileNotFoundException {
        Optional<FileEntity> file = fileRepository.findById(id);
        if (file.isPresent()){
            return file;
        }
        throw new FileNotFoundException();
    }

    @Override
    public List<FileResponseDto> getAllFiles() {
        List<FileResponseDto> files = fileRepository.findAll().stream()
                .map((dbFile) -> {
                    String fileDownloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("api/fileManager/files/")
                            .path(dbFile.getId().toString())
                            .toUriString();

                    return FileResponseDto.builder()
                            .name(dbFile.getName())
                            .url(fileDownloadUrl)
                            .type(dbFile.getType())
                            .size(dbFile.getData().length)
                            .build();
                }).collect(Collectors.toList());

        return files;
    }
}