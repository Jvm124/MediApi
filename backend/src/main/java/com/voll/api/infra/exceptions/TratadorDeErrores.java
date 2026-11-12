package com.voll.api.infra.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import com.voll.api.domain.ValidacionException;
import com.voll.api.infra.security.DemasiadosIntentosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Todos los errores se responden con el formato ProblemDetail (RFC 9457), que Spring ya trae:
 * {"title": "Bad Request", "status": 400, "detail": "Ya existe un paciente con ese email", "instance": "/pacientes"}.
 * El mensaje para el usuario va siempre en "detail", que es lo que muestra el frontend
 * (frontend/src/utils/getErrorMessage.ts).
 *
 * Hereda de ResponseEntityExceptionHandler, que ya responde con ProblemDetail y su código correcto
 * los errores propios de Spring MVC (JSON mal formado, método no permitido, ruta inexistente...).
 * Sin eso, el manejador genérico del final los convertiría a todos en un 500.
 */
@Slf4j
@RestControllerAdvice
public class TratadorDeErrores extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail tratarError404() {
        // El mensaje de la excepción es interno (nombre de la clase e id), así que no se envía.
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No se encontró el registro solicitado.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errores = e.getFieldErrors().stream().map(DatosErrorValidacion::new).toList();
        var detalle = errores.stream()
                .map(error -> error.campo() + ": " + error.error())
                .collect(Collectors.joining("; "));
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detalle.isEmpty() ? "Hay datos inválidos." : detalle);
        // La lista campo por campo se mantiene, por si un formulario quiere marcar cada campo.
        problema.setProperty("errores", errores);
        return handleExceptionInternal(e, problema, headers, status, request);
    }

    @ExceptionHandler(ValidacionException.class)
    public ProblemDetail tratarErrorDeValidacion(ValidacionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // Login fallido. Usuario.isEnabled() es false si la cuenta está suspendida o dada de baja,
    // y entonces Spring lanza DisabledException. Un correo que no existe da el mismo mensaje que
    // una contraseña incorrecta, para no revelar qué correos tienen cuenta.
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail tratarErrorDeAutenticacion(AuthenticationException e, HttpServletRequest request) {
        if (e instanceof AuthenticationServiceException) {
            // No es culpa del usuario: falló algo del servidor al buscarlo (por ejemplo, la base de datos).
            return tratarErrorInesperado(e, request);
        }
        var mensaje = e instanceof DisabledException
                ? "Tu cuenta está suspendida o dada de baja."
                : "Correo o contraseña incorrectos.";
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, mensaje);
    }

    // Demasiados intentos fallidos de login (SEC-04). Retry-After dice cuántos segundos hay que esperar.
    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<ProblemDetail> tratarDemasiadosIntentos(DemasiadosIntentosException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getSegundosDeEspera()))
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage()));
    }

    // @PreAuthorize rechazó la petición porque el usuario no tiene el rol necesario.
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail tratarAccesoDenegado() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "No tienes permiso para hacer esto.");
    }

    // Un dato que debe ser único (correo, documento) ya estaba guardado. Pasa, por ejemplo, cuando
    // dos registros con el mismo correo llegan a la vez y los dos pasan las validaciones previas.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail tratarConflicto(DataIntegrityViolationException e) {
        log.warn("Conflicto al guardar: {}", e.getMostSpecificCause().getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Ya existe un registro con esos datos (por ejemplo, el mismo correo o documento).");
    }

    // Cualquier otro error. El detalle técnico va al log del servidor, nunca al usuario.
    @ExceptionHandler(Exception.class)
    public ProblemDetail tratarErrorInesperado(Exception e, HttpServletRequest request) {
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intenta de nuevo más tarde.");
    }

    private record DatosErrorValidacion(String campo, String error){
        public DatosErrorValidacion(FieldError error) {
            this(error.getField(), error.getDefaultMessage());
        }
    }

}
