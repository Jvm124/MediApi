package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.voll.api.infra.reloj.ConfiguracionDelReloj.ZONA_DE_LA_CLINICA;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba la regla de los 30 minutos de anticipación con la hora fijada (SEC-07): el reloj de la prueba marca
 * siempre las 9:00 del 10 de noviembre de 2026 en Lima, así que da lo mismo cuándo se ejecute.
 */
class ValidadorConsultaConAnticipacionTest {

    private static final LocalDateTime AHORA_EN_LIMA = LocalDateTime.of(2026, 11, 10, 9, 0);

    private final ValidadorConsultaConAnticipacion validador = new ValidadorConsultaConAnticipacion(
            Clock.fixed(AHORA_EN_LIMA.atZone(ZONA_DE_LA_CLINICA).toInstant(), ZONA_DE_LA_CLINICA));

    @Test
    @DisplayName("Rechaza una consulta para dentro de 29 minutos")
    void rechazaConMenosDe30Minutos() {
        assertThatThrownBy(() -> validador.validar(reservaPara(AHORA_EN_LIMA.plusMinutes(29))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("30 minutos");
    }

    @Test
    @DisplayName("Acepta una consulta para dentro de 30 minutos justos")
    void aceptaCon30Minutos() {
        assertThatCode(() -> validador.validar(reservaPara(AHORA_EN_LIMA.plusMinutes(30))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Acepta una consulta para dentro de 2 horas")
    void aceptaConDosHoras() {
        // Con LocalDateTime.now() en un servidor con hora UTC, "ahora" serían las 14:00 y esta consulta
        // de las 11:00 se rechazaba como si ya hubiera pasado.
        assertThatCode(() -> validador.validar(reservaPara(AHORA_EN_LIMA.plusHours(2))))
                .doesNotThrowAnyException();
    }

    private static DatosReservaConsulta reservaPara(LocalDateTime fecha) {
        return new DatosReservaConsulta(1L, 1L, fecha, null);
    }
}
