package com.hospital.backendHospital.controllers;

import com.hospital.backendHospital.auth.service.AuthService;
import com.hospital.backendHospital.models.dto.file.FileResponseDto;
import com.hospital.backendHospital.models.entity.FileEntity;
import com.hospital.backendHospital.services.IFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

    private final IFileService fileService;
    private final AuthService authService;

    /** Lista sin contenido. PATIENT: los suyos; DOCTOR: los de un paciente suyo (patientId obligatorio). */
    @GetMapping
    public List<FileResponseDto> listFiles(@RequestParam(required = false) Long patientId) {
        return fileService.listFiles(authService.getAuthenticatedUser(), patientId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponseDto> uploadFile(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) Long patientId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileService.store(authService.getAuthenticatedUser(), file, patientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable UUID id) {
        FileEntity file = fileService.getFile(authService.getAuthenticatedUser(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getName(), StandardCharsets.UTF_8).build().toString())
                .body(file.getData());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
        fileService.deleteFile(authService.getAuthenticatedUser(), id);
        return ResponseEntity.noContent().build();
    }
}
