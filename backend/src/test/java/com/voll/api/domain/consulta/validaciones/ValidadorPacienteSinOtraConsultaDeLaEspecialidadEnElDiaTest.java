package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.ConsultaRepository;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import com.voll.api.domain.consulta.EstadoConsulta;
import com.voll.api.domain.medico.Especialidad;
import com.voll.api.domain.medico.Medico;
import com.voll.api.domain.medico.MedicoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Prueba que un paciente no tenga dos consultas de la misma especialidad el mismo día, con los repositorios
 * simulados. Los repositorios solo responden si se les pregunta por el día completo de la consulta.
 */
@ExtendWith(MockitoExtension.class)
class ValidadorPacienteSinOtraConsultaDeLaEspecialidadEnElDiaTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 16);
    private static final LocalDateTime LUNES_A_LAS_10 = LUNES.atTime(10, 0);
    // El día completo: desde las 00:00 hasta el último instante antes de la medianoche.
    private static final LocalDateTime INICIO_DEL_DIA = LUNES.atStartOfDay();
    private static final LocalDateTime FIN_DEL_DIA = LUNES.atTime(LocalTime.MAX);

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @InjectMocks
    private ValidadorPacienteSinOtraConsultaDeLaEspecialidadEnElDia validador;

    @Test
    @DisplayName("Con médico elegido, rechaza si el paciente ya tiene ese día una consulta de la especialidad del médico")
    void conMedicoElegidoUsaSuEspecialidad() {
        var cardiologa = new Medico(1L, true, "Dra. Ana Torres", null, null, null, Especialidad.CARDIOLOGIA, null);
        when(medicoRepository.findById(1L)).thenReturn(Optional.of(cardiologa));
        when(consultaRepository.pacienteTieneConsultaDeEspecialidadEnElDia(
                2L, Especialidad.CARDIOLOGIA, EstadoConsulta.PROGRAMADA, INICIO_DEL_DIA, FIN_DEL_DIA))
                .thenReturn(true);

        assertThatThrownBy(() -> validador.validar(new DatosReservaConsulta(1L, 2L, LUNES_A_LAS_10, null)))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("ya tiene una consulta de esa especialidad");
    }

    @Test
    @DisplayName("Sin médico elegido, usa la especialidad pedida y acepta si ese día no tiene otra")
    void sinMedicoUsaLaEspecialidadPedida() {
        when(consultaRepository.pacienteTieneConsultaDeEspecialidadEnElDia(
                2L, Especialidad.DERMATOLOGIA, EstadoConsulta.PROGRAMADA, INICIO_DEL_DIA, FIN_DEL_DIA))
                .thenReturn(false);

        assertThatCode(() -> validador.validar(
                new DatosReservaConsulta(null, 2L, LUNES_A_LAS_10, Especialidad.DERMATOLOGIA)))
                .doesNotThrowAnyException();
        verifyNoInteractions(medicoRepository);
    }

    @Test
    @DisplayName("Sin médico ni especialidad, no hay nada que revisar")
    void sinMedicoNiEspecialidadNoRevisaNada() {
        assertThatCode(() -> validador.validar(new DatosReservaConsulta(null, 2L, LUNES_A_LAS_10, null)))
                .doesNotThrowAnyException();
        verifyNoInteractions(consultaRepository, medicoRepository);
    }
}
