package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import com.voll.api.domain.medico.MedicoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Prueba que no se reserve con un médico inactivo. El repositorio es simulado con Mockito: la prueba decide qué
 * responde, sin base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ValidadorMedicoActivoTest {

    private static final LocalDateTime LUNES_A_LAS_10 = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Mock
    private MedicoRepository medicoRepository;

    // Mockito crea el validador con su constructor y le pasa el repositorio simulado.
    @InjectMocks
    private ValidadorMedicoActivo validador;

    @Test
    @DisplayName("Rechaza un médico inactivo")
    void rechazaUnMedicoInactivo() {
        when(medicoRepository.findActivoById(1L)).thenReturn(false);

        assertThatThrownBy(() -> validador.validar(new DatosReservaConsulta(1L, 2L, LUNES_A_LAS_10, null)))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("medico inactivo");
    }

    @Test
    @DisplayName("Acepta un médico activo")
    void aceptaUnMedicoActivo() {
        when(medicoRepository.findActivoById(1L)).thenReturn(true);

        assertThatCode(() -> validador.validar(new DatosReservaConsulta(1L, 2L, LUNES_A_LAS_10, null)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Si no se elige médico, no hay nada que revisar")
    void sinMedicoElegidoNoRevisaNada() {
        assertThatCode(() -> validador.validar(new DatosReservaConsulta(null, 2L, LUNES_A_LAS_10, null)))
                .doesNotThrowAnyException();
        verifyNoInteractions(medicoRepository);
    }
}
