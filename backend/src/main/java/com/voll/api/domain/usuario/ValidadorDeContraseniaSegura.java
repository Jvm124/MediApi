package com.voll.api.domain.usuario;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Comprueba la regla de {@link ContraseniaSegura}. Hibernate Validator lo usa solo: basta con poner
 * la anotación en el campo del DTO y @Valid en el parámetro del controlador.
 */
public class ValidadorDeContraseniaSegura implements ConstraintValidator<ContraseniaSegura, String> {

    private static final int LONGITUD_MINIMA = 8;
    private static final int BYTES_MAXIMOS = 72;

    // \p{Lu} es cualquier letra mayúscula (también Á o Ñ) y \p{Ll}, cualquier minúscula.
    // \d es un dígito del 0 al 9. El frontend usa las mismas expresiones.
    private static final Pattern MAYUSCULA = Pattern.compile("\\p{Lu}");
    private static final Pattern MINUSCULA = Pattern.compile("\\p{Ll}");
    private static final Pattern NUMERO = Pattern.compile("\\d");

    @Override
    public boolean isValid(String contrasenia, ConstraintValidatorContext context) {
        return contrasenia != null
                && contrasenia.length() >= LONGITUD_MINIMA
                && contrasenia.getBytes(StandardCharsets.UTF_8).length <= BYTES_MAXIMOS
                && MAYUSCULA.matcher(contrasenia).find()
                && MINUSCULA.matcher(contrasenia).find()
                && NUMERO.matcher(contrasenia).find();
    }
}
