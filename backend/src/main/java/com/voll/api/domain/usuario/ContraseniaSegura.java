package com.voll.api.domain.usuario;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Regla única para las contraseñas nuevas (registro de pacientes, médicos y personal):
 * entre 8 y 72 caracteres, con al menos una mayúscula, una minúscula y un número.
 *
 * El máximo se mide en bytes (UTF-8) porque BCrypt, que cifra las contraseñas, no admite más de 72:
 * con más, Spring Security lanza una excepción al cifrar y el registro terminaba en un error 500.
 * Una letra con tilde o una ñ ocupa 2 bytes.
 *
 * También rechaza null y el texto vacío, así que reemplaza a @NotBlank.
 * El frontend aplica la misma regla en frontend/src/schemas/contraseniaSchema.ts.
 */
@Documented
@Constraint(validatedBy = ValidadorDeContraseniaSegura.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContraseniaSegura {

    String message() default "debe tener entre 8 y 72 caracteres (las letras con tilde y la ñ cuentan como 2),"
            + " con al menos una mayúscula, una minúscula y un número";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
