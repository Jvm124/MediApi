package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.ConsultaRepository;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import com.voll.api.domain.consulta.EstadoConsulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Prueba que un médico no tenga dos consultas programadas a la misma hora, con el repositorio simulado.
 * <p>
 * El repositorio simulado solo responde si se le pregunta por las consultas PROGRAMADA. Si el validador
 * preguntara por otro estado, Mockito haría fallar la prueba. Que una consulta cancelada deje libre la hora en
 * MySQL (BUG-01) lo prueba ReservaDeConsultasConMySQLTest.
 */
@ExtendWith(MockitoExtension.class)
class ValidadorMedicoConOtraConsultaEnElMismoHorarioTest {

    private static final LocalDateTime LUNES_A_LAS_10 = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Mock
    private ConsultaRepository consultaRepository;

    @InjectMocks
    private ValidadorMedicoConOtraConsultaEnElMismoHorario validador;

    @Test
    @DisplayName("Rechaza si el médico ya tiene una consulta programada a esa hora")
    void rechazaSiElMedicoEstaOcupado() {
        when(consultaRepository.existsByIdMedicoIdAndFechaAndEstado(1L, LUNES_A_LAS_10, EstadoConsulta.PROGRAMADA))
                .thenReturn(true);

        assertThatThrownBy(() -> validador.validar(new DatosReservaConsulta(1L, 2L, LUNES_A_LAS_10, null)))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("otra consulta en esa misma fecha y hora");
    }

    @Test
    @DisplayName("Acepta si el médico no tiene otra consulta programada a esa hora")
    void aceptaSiElMedicoEstaLibre() {
        when(consultaRepository.existsByIdMedicoIdAndFechaAndEstado(1L, LUNES_A_LAS_10, EstadoConsulta.PROGRAMADA))
                .thenReturn(false);

        assertThatCode(() -> validador.validar(new DatosReservaConsulta(1L, 2L, LUNES_A_LAS_10, null)))
                .doesNotThrowAnyException();
    }
}
