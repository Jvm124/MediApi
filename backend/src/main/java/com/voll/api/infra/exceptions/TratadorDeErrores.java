package com.voll.api.infra.exceptions;

import jakarta.persistence.EntityNotFoundException;
import com.voll.api.domain.ValidacionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Todos los errores se responden con el formato ProblemDetail (RFC 9457), que Spring ya trae:
 * {"title": "Bad Request", "status": 400, "detail": "Ya existe un paciente con ese email", "instance": "/pacientes"}.
 * El mensaje para el usuario va siempre en "detail", que es lo que muestra el frontend
 * (frontend/src/utils/getErrorMessage.ts).
 */
@RestControllerAdvice
public class TratadorDeErrores {

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail tratarError404() {
        // El mensaje de la excepción es interno (nombre de la clase e id), así que no se envía.
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No se encontró el registro solicitado.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratarError400(MethodArgumentNotValidException e) {
        var errores = e.getFieldErrors().stream().map(DatosErrorValidacion::new).toList();
        var detalle = errores.stream()
                .map(error -> error.campo() + ": " + error.error())
                .collect(Collectors.joining("; "));
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detalle.isEmpty() ? "Hay datos inválidos." : detalle);
        // La lista campo por campo se mantiene, por si un formulario quiere marcar cada campo.
        problema.setProperty("errores", errores);
        return problema;
    }

    @ExceptionHandler(ValidacionException.class)
    public ProblemDetail tratarErrorDeValidacion(ValidacionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private record DatosErrorValidacion(String campo, String error){
        public DatosErrorValidacion(FieldError error) {
            this(error.getField(), error.getDefaultMessage());
        }
    }

}
