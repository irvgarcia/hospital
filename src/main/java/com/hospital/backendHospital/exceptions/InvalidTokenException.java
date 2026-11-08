package com.hospital.backendHospital.exceptions;

import org.springframework.security.core.AuthenticationException;

/** Token ausente, mal formado, expirado, revocado o de tipo incorrecto. Responde 401. */
public class InvalidTokenException extends AuthenticationException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
