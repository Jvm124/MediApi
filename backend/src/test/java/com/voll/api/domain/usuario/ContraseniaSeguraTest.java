package com.voll.api.domain.usuario;

import com.voll.api.domain.medico.DatosRegistroMedico;
import com.voll.api.domain.paciente.DatosRegistroPaciente;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba la regla de @ContraseniaSegura con el mismo Hibernate Validator que usa la app, sin Spring
 * ni base de datos. Cubre SEC-05 de docs/revisiones/0001-revision-inicial.md.
 */
class ContraseniaSeguraTest {

    private static final String MENSAJE = "debe tener entre 8 y 72 caracteres (las letras con tilde y la ñ cuentan como 2),"
            + " con al menos una mayúscula, una minúscula y un número";

    private static ValidatorFactory fabrica;
    private static Validator validador;

    record ConContrasenia(@ContraseniaSegura String contrasenia) {
    }

    @BeforeAll
    static void crearValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        fabrica.close();
    }

    private Set<ConstraintViolation<ConContrasenia>> validar(String contrasenia) {
        return validador.validate(new ConContrasenia(contrasenia));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Clave-de-prueba-1", "Abcdefg1", "Ñandú2026", "una frase Larga con 3 espacios"})
    @DisplayName("Acepta contraseñas de 8 o más caracteres con mayúscula, minúscula y número")
    void aceptaContraseniasSeguras(String contrasenia) {
        assertThat(validar(contrasenia)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1", "Abcdef1", "abcdefg1", "ABCDEFG1", "Abcdefgh", "        "})
    @DisplayName("Rechaza contraseñas vacías, cortas o sin mayúscula, minúscula o número, con un solo mensaje")
    void rechazaContraseniasDebiles(String contrasenia) {
        assertThat(validar(contrasenia))
                .singleElement()
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo(MENSAJE);
    }

    @Test
    @DisplayName("El máximo de 72 se mide en bytes: una ñ cuenta como 2")
    void elMaximoSeMideEnBytes() {
        assertThat(validar("Aa1" + "a".repeat(69))).isEmpty();   // 72 bytes
        assertThat(validar("Aa1" + "a".repeat(70))).hasSize(1);  // 73 bytes
        assertThat(validar("Aa1" + "ñ".repeat(35))).hasSize(1);  // 38 letras, pero 73 bytes
    }

    @Test
    @DisplayName("BCrypt no puede cifrar más de 72 bytes: por eso existe el máximo")
    void bcryptNoAdmiteMasDe72Bytes() {
        var codificador = new BCryptPasswordEncoder();
        assertThatThrownBy(() -> codificador.encode("Aa1" + "a".repeat(70)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Los tres registros (personal, paciente y médico) rechazan la contraseña «1»")
    void losTresRegistrosUsanLaRegla() {
        assertThat(validador.validateProperty(new DatosRegistroUsuario(null, "1", null), "contrasenia"))
                .hasSize(1);
        assertThat(validador.validateProperty(
                new DatosRegistroPaciente(null, null, null, null, null, "1"), "contrasenia"))
                .hasSize(1);
        assertThat(validador.validateProperty(
                new DatosRegistroMedico(null, null, null, null, null, null, "1"), "contrasenia"))
                .hasSize(1);
    }
}
