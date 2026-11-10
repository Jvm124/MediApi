package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba el horario de la clínica: de lunes a sábado, con consultas que empiezan desde las 7:00 y antes de las
 * 18:00. No hace falta simular nada, porque el validador solo mira la fecha.
 */
class ValidadorFueraHorarioConsultasTest {

    private static final LocalDate DOMINGO = LocalDate.of(2026, 11, 8);
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);
    private static final LocalDate SABADO = LocalDate.of(2026, 11, 14);

    private final ValidadorFueraHorarioConsultas validador = new ValidadorFueraHorarioConsultas();

    @Test
    @DisplayName("Rechaza una consulta en domingo")
    void rechazaElDomingo() {
        assertThatThrownBy(() -> validador.validar(reservaPara(DOMINGO.atTime(10, 0))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("fuera del horario");
    }

    @Test
    @DisplayName("Rechaza una consulta a las 6:59")
    void rechazaAntesDeLasSiete() {
        assertThatThrownBy(() -> validador.validar(reservaPara(LUNES.atTime(6, 59))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("fuera del horario");
    }

    @Test
    @DisplayName("Acepta una consulta a las 7:00 en punto")
    void aceptaALasSiete() {
        assertThatCode(() -> validador.validar(reservaPara(LUNES.atTime(7, 0))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Acepta una consulta el sábado a las 17:00")
    void aceptaElSabadoALasCinco() {
        assertThatCode(() -> validador.validar(reservaPara(SABADO.atTime(17, 0))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rechaza una consulta a las 18:00")
    void rechazaALasSeis() {
        assertThatThrownBy(() -> validador.validar(reservaPara(LUNES.atTime(18, 0))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("fuera del horario");
    }

    private static DatosReservaConsulta reservaPara(LocalDateTime fecha) {
        return new DatosReservaConsulta(1L, 2L, fecha, null);
    }
}
