package com.hospital.backendHospital.exceptions;

/** La operacion choca con el estado actual de otro registro (duplicado, recurso ocupado...). Responde 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
