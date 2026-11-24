package com.hospital.backendHospital.exceptions;

/** Error al leer o generar un archivo (responde 500 con un mensaje generico). */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
