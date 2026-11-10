package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.Consulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.voll.api.infra.reloj.ConfiguracionDelReloj.ZONA_DE_LA_CLINICA;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba la regla de las 24 horas para cancelar con la hora fijada (SEC-07): el reloj de la prueba marca
 * siempre las 9:00 del 10 de noviembre de 2026 en Lima, así que da lo mismo cuándo se ejecute.
 */
class ValidadorHorarioAntecedenciaTest {

    private static final LocalDateTime AHORA_EN_LIMA = LocalDateTime.of(2026, 11, 10, 9, 0);

    private final ValidadorHorarioAntecedencia validador = new ValidadorHorarioAntecedencia(
            Clock.fixed(AHORA_EN_LIMA.atZone(ZONA_DE_LA_CLINICA).toInstant(), ZONA_DE_LA_CLINICA));

    @Test
    @DisplayName("No deja cancelar una consulta que empieza dentro de 23 horas y 59 minutos")
    void rechazaConMenosDe24Horas() {
        assertThatThrownBy(() -> validador.validar(consultaEl(AHORA_EN_LIMA.plusHours(24).minusMinutes(1))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("24 horas");
    }

    @Test
    @DisplayName("Deja cancelar una consulta que empieza dentro de 24 horas justas")
    void aceptaCon24Horas() {
        assertThatCode(() -> validador.validar(consultaEl(AHORA_EN_LIMA.plusHours(24))))
                .doesNotThrowAnyException();
    }

    private static Consulta consultaEl(LocalDateTime fecha) {
        return new Consulta(null, null, fecha);
    }
}
