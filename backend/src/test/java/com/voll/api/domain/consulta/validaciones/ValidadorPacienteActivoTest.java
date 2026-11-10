package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import com.voll.api.domain.paciente.PacienteRepository;
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
 * Prueba que no se reserve para un paciente inactivo, con el repositorio simulado.
 */
@ExtendWith(MockitoExtension.class)
class ValidadorPacienteActivoTest {

    private static final LocalDateTime LUNES_A_LAS_10 = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private ValidadorPacienteActivo validador;

    @Test
    @DisplayName("Rechaza un paciente inactivo")
    void rechazaUnPacienteInactivo() {
        when(pacienteRepository.findActivoById(2L)).thenReturn(false);

        assertThatThrownBy(() -> validador.validar(new DatosReservaConsulta(1L, 2L, LUNES_A_LAS_10, null)))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("paciente excluido");
    }

    @Test
    @DisplayName("Acepta un paciente activo")
    void aceptaUnPacienteActivo() {
        when(pacienteRepository.findActivoById(2L)).thenReturn(true);

        assertThatCode(() -> validador.validar(new DatosReservaConsulta(1L, 2L, LUNES_A_LAS_10, null)))
                .doesNotThrowAnyException();
    }
}
