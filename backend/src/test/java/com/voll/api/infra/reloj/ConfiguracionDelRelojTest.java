package com.voll.api.infra.reloj;

import com.voll.api.domain.consulta.DatosReservaConsulta;
import jakarta.validation.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba el reloj de la aplicación (SEC-07): marca la hora de Lima, y las validaciones de los datos que llegan
 * a la API, como @Future, comparan con él.
 */
class ConfiguracionDelRelojTest {

    private final ConfiguracionDelReloj configuracion = new ConfiguracionDelReloj();

    @Test
    @DisplayName("El reloj de la aplicación marca la hora de Lima")
    void elRelojMarcaLaHoraDeLima() {
        assertThat(configuracion.reloj().getZone()).isEqualTo(ZoneId.of("America/Lima"));
    }

    @Test
    @DisplayName("@Future compara con el reloj de la aplicación, en la hora de Lima")
    void futureComparaConElRelojDeLaAplicacion() {
        // Las 14:00 en UTC, que son las 9:00 en Lima.
        var reloj = Clock.fixed(Instant.parse("2020-01-15T14:00:00Z"), ZoneId.of("America/Lima"));
        var configuracionDeValidacion = Validation.byDefaultProvider().configure();
        configuracion.validacionesConElReloj(reloj).customize(configuracionDeValidacion);

        try (var fabrica = configuracionDeValidacion.buildValidatorFactory()) {
            var validador = fabrica.getValidator();
            // Las 10:00 en Lima son futuro para ese reloj, aunque en UTC ya sean las 14:00
            // y aunque en el calendario real 2020 ya pasó.
            assertThat(validador.validateProperty(reservaPara(LocalDateTime.of(2020, 1, 15, 10, 0)), "fecha"))
                    .isEmpty();
            // Las 8:00 en Lima ya pasaron.
            assertThat(validador.validateProperty(reservaPara(LocalDateTime.of(2020, 1, 15, 8, 0)), "fecha"))
                    .hasSize(1);
        }
    }

    private static DatosReservaConsulta reservaPara(LocalDateTime fecha) {
        return new DatosReservaConsulta(1L, 1L, fecha, null);
    }
}
