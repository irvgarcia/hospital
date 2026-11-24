package com.hospital.backendHospital.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Formato unico de error (B10): ProblemDetail (RFC 7807) con status, title, detail, instance y,
 * en errores de validacion, "errors": [{field, message}]. Ninguna entrada invalida termina en 500 (A10),
 * y el 500 generico nunca devuelve el mensaje interno (queda en el log).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ------------------------------------------------------------------ 400

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Valor no válido" : error.getDefaultMessage()))
                .toList();
        ex.getBindingResult().getGlobalErrors().forEach(error -> log.debug("Error global de validacion: {}", error));
        String detail = errors.stream().map(e -> e.get("message")).distinct().collect(Collectors.joining(". "));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Datos inválidos", detail.isEmpty() ? "La petición tiene datos inválidos" : detail, request);
        problem.setProperty("errors", errors);
        return respond(problem);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        String detail = ex.getAllErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "Valor no válido" : error.getDefaultMessage())
                .distinct().collect(Collectors.joining(". "));
        return respond(problem(HttpStatus.BAD_REQUEST, "Datos inválidos", detail, request));
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ProblemDetail> handleInvalidData(InvalidDataException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.BAD_REQUEST, "Datos inválidos", ex.getMessage(), request));
    }

    /** JSON mal formado o con un valor que no corresponde al tipo (por ejemplo un enum o una fecha invalida). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String detail = "El cuerpo de la petición no es un JSON válido";
        if (ex.getCause() instanceof InvalidFormatException invalid) {
            detail = "Valor no válido para el campo '" + fieldPath(invalid) + "': " + invalid.getValue();
        } else if (ex.getCause() instanceof MismatchedInputException mismatched && !mismatched.getPath().isEmpty()) {
            detail = "Valor no válido para el campo '" + fieldPath(mismatched) + "'";
        } else if (ex.getMessage() != null && ex.getMessage().startsWith("Required request body is missing")) {
            detail = "Falta el cuerpo de la petición";
        }
        return respond(problem(HttpStatus.BAD_REQUEST, "Petición mal formada", detail, request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.BAD_REQUEST, "Petición mal formada",
                "Valor no válido para '" + ex.getName() + "': " + ex.getValue(), request));
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ResponseEntity<ProblemDetail> handleBind(org.springframework.validation.BindException ex, HttpServletRequest request) {
        String detail = ex.getFieldErrors().stream()
                .map(error -> "Valor no válido para '" + error.getField() + "'")
                .distinct().collect(Collectors.joining(". "));
        return respond(problem(HttpStatus.BAD_REQUEST, "Petición mal formada", detail, request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.BAD_REQUEST, "Petición mal formada",
                "Falta el parámetro '" + ex.getParameterName() + "'", request));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ProblemDetail> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.BAD_REQUEST, "Petición mal formada",
                "Falta la parte '" + ex.getRequestPartName() + "'", request));
    }

    // ------------------------------------------------------------------ 401 / 403

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ProblemDetail> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.FORBIDDEN, "Usuario desactivado", "La cuenta está desactivada", request));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        String detail = ex instanceof InvalidTokenException ? ex.getMessage() : "Email o contraseña incorrectos";
        return respond(problem(HttpStatus.UNAUTHORIZED, "No autenticado", detail, request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String detail = ex.getMessage() == null || ex.getMessage().equals("Access Denied")
                ? "Tu rol no tiene permiso para este recurso" : ex.getMessage();
        return respond(problem(HttpStatus.FORBIDDEN, "Acceso denegado", detail, request));
    }

    // ------------------------------------------------------------------ 404 / 405 / 415

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", "La ruta /" + ex.getResourcePath() + " no existe", request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
                "La ruta no admite el método " + ex.getMethod(), request));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado",
                "Content-Type no soportado: " + ex.getContentType(), request));
    }

    // ------------------------------------------------------------------ 409

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.CONFLICT, "Conflicto", ex.getMessage(), request));
    }

    /**
     * Otra transaccion modifico el mismo registro entre la lectura y el guardado (@Version, por ejemplo el stock
     * de un insumo). No se aplico ningun cambio: el cliente puede volver a consultar el recurso y reintentar.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLocking(OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.info("Conflicto de concurrencia: {}", ex.getMessage());
        return respond(problem(HttpStatus.CONFLICT, "Conflicto de concurrencia",
                "El recurso fue modificado por otra operación al mismo tiempo; vuelve a intentarlo", request));
    }

    /**
     * La base rechazo la operacion por una restriccion (indice unico, llave foranea, NOT NULL). El mensaje de MySQL
     * no se devuelve porque expone tablas y columnas; queda en el log.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violacion de integridad: {}", ex.getMostSpecificCause().getMessage());
        return respond(problem(HttpStatus.CONFLICT, "Conflicto con datos existentes",
                "La operación entra en conflicto con otro registro (por ejemplo, un duplicado)", request));
    }

    // ------------------------------------------------------------------ 413 / 500

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.PAYLOAD_TOO_LARGE, "Archivo demasiado grande",
                "El archivo excede el tamaño máximo permitido (5 MB)", request));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ProblemDetail> handleMultipart(MultipartException ex, HttpServletRequest request) {
        return respond(problem(HttpStatus.BAD_REQUEST, "Petición mal formada",
                "La petición debe ser multipart/form-data con el archivo en el campo 'file'", request));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ProblemDetail> handleFileStorage(FileStorageException ex, HttpServletRequest request) {
        log.error("Error de archivo", ex);
        return respond(problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar el archivo", ex.getMessage(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor",
                "Ocurrió un error inesperado; inténtalo más tarde", request));
    }

    // ------------------------------------------------------------------ utilidades

    private static ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        if (request != null) {
            problem.setInstance(URI.create(request.getRequestURI()));
        }
        return problem;
    }

    /** El Content-Type de error es JSON aunque el endpoint produzca otro tipo (por ejemplo PDF). */
    private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String fieldPath(MismatchedInputException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."));
    }
}
