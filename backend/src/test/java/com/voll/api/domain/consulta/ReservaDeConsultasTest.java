package com.voll.api.domain.consulta;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.validaciones.ValidadorCancelamientoDeConsulta;
import com.voll.api.domain.consulta.validaciones.ValidadorDeConsultas;
import com.voll.api.domain.medico.Especialidad;
import com.voll.api.domain.medico.Medico;
import com.voll.api.domain.medico.MedicoRepository;
import com.voll.api.domain.paciente.Paciente;
import com.voll.api.domain.paciente.PacienteRepository;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.voll.api.infra.reloj.ConfiguracionDelReloj.ZONA_DE_LA_CLINICA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Prueba las reglas de ReservaDeConsultas sin Spring ni base de datos: los repositorios y los validadores son
 * simulados con Mockito, y la hora de la clínica es un reloj parado (Clock.fixed). Así se prueba solo lo que decide
 * esta clase: a nombre de quién se reserva, qué médico se elige, quién puede cancelar una consulta, quién y desde
 * qué hora puede atenderla y qué pasa cuando un validador o la base dicen que no. Lo que depende de las consultas a
 * MySQL lo prueba ReservaDeConsultasConMySQLTest, y dos reservas que llegan a la vez, DobleReservaConMySQLTest.
 */
@ExtendWith(MockitoExtension.class)
class ReservaDeConsultasTest {

    private static final LocalDateTime LUNES_A_LAS_10 = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Mock
    private MedicoRepository medicoRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private ConsultaRepository consultaRepository;
    @Mock
    private ValidadorDeConsultas validador;
    @Mock
    private ValidadorCancelamientoDeConsulta validadorDeCancelacion;

    private ReservaDeConsultas reserva;

    @BeforeEach
    void crearReserva() {
        // Salvo que la prueba diga otra hora, en la clínica son las 10:30 del lunes: media hora después de la hora de
        // las consultas de prueba.
        reserva = reservaALas(LUNES_A_LAS_10.plusMinutes(30));
    }

    // Con la inyección por constructor (CAL-01), la prueba arma la clase con piezas simuladas y con un reloj parado
    // en esa hora de Lima (SEC-07).
    private ReservaDeConsultas reservaALas(LocalDateTime horaEnLima) {
        var reloj = Clock.fixed(horaEnLima.atZone(ZONA_DE_LA_CLINICA).toInstant(), ZONA_DE_LA_CLINICA);
        return new ReservaDeConsultas(medicoRepository, pacienteRepository, consultaRepository,
                List.of(validador), List.of(validadorDeCancelacion), reloj);
    }

    // ----- Reservar -----

    @Test
    @DisplayName("Un paciente siempre reserva a su nombre, aunque envíe el id de otro paciente")
    void unPacienteReservaASuNombre() {
        when(medicoRepository.existsById(1L)).thenReturn(true);
        when(medicoRepository.getReferenceById(1L)).thenReturn(medico(1L));

        var detalle = reserva.reservar(
                new DatosReservaConsulta(1L, 99L, LUNES_A_LAS_10, null), usuarioPaciente(paciente(2L)));

        assertThat(detalle.idPaciente()).isEqualTo(2L);
        assertThat(detalle.idMedico()).isEqualTo(1L);
        assertThat(detalle.estado()).isEqualTo(EstadoConsulta.PROGRAMADA);
        // Los validadores revisan los datos con su paciente (el 2), no con el 99.
        verify(validador).validar(new DatosReservaConsulta(1L, 2L, LUNES_A_LAS_10, null));
        verify(consultaRepository).saveAndFlush(any(Consulta.class));
        verifyNoInteractions(pacienteRepository);
    }

    @Test
    @DisplayName("Un usuario paciente sin ficha de paciente no puede reservar")
    void pacienteSinFichaNoPuedeReservar() {
        assertThatThrownBy(() -> reserva.reservar(
                new DatosReservaConsulta(1L, null, LUNES_A_LAS_10, null), usuarioPaciente(null)))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("no tiene un registro de paciente");
        verify(consultaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("No reserva con un médico que no existe, y ni siquiera llama a los validadores")
    void noReservaConUnMedicoInexistente() {
        when(pacienteRepository.findById(2L)).thenReturn(Optional.of(paciente(2L)));
        when(medicoRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> reserva.reservar(
                new DatosReservaConsulta(1L, 2L, LUNES_A_LAS_10, null), recepcionista()))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("No existe un médico con el id informado");
        verifyNoInteractions(validador);
        verify(consultaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Si un validador rechaza la reserva, no se guarda nada")
    void siUnValidadorLaRechazaNoSeGuarda() {
        when(medicoRepository.existsById(1L)).thenReturn(true);
        doThrow(new ValidacionException("Horario seleccionado fuera del horario de atendimiento de la clinica"))
                .when(validador).validar(any());

        assertThatThrownBy(() -> reserva.reservar(
                new DatosReservaConsulta(1L, null, LUNES_A_LAS_10, null), usuarioPaciente(paciente(2L))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("fuera del horario");
        verify(consultaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Sin médico, elige uno libre de la especialidad contando solo las consultas programadas")
    void sinMedicoEligeUnoLibreDeLaEspecialidad() {
        when(medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(
                Especialidad.CARDIOLOGIA, LUNES_A_LAS_10, EstadoConsulta.PROGRAMADA)).thenReturn(medico(5L));

        var detalle = reserva.reservar(new DatosReservaConsulta(null, null, LUNES_A_LAS_10, Especialidad.CARDIOLOGIA),
                usuarioPaciente(paciente(2L)));

        assertThat(detalle.idMedico()).isEqualTo(5L);
        verify(consultaRepository).saveAndFlush(any(Consulta.class));
    }

    @Test
    @DisplayName("Si no hay un médico libre de esa especialidad, avisa y no guarda nada")
    void sinMedicoLibreAvisa() {
        when(medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(
                Especialidad.CARDIOLOGIA, LUNES_A_LAS_10, EstadoConsulta.PROGRAMADA)).thenReturn(null);

        assertThatThrownBy(() -> reserva.reservar(
                new DatosReservaConsulta(null, null, LUNES_A_LAS_10, Especialidad.CARDIOLOGIA),
                usuarioPaciente(paciente(2L))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("No existe un médico disponible");
        verify(consultaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Sin médico ni especialidad, pide elegir una especialidad")
    void sinMedicoNiEspecialidadPideUnaEspecialidad() {
        assertThatThrownBy(() -> reserva.reservar(
                new DatosReservaConsulta(null, null, LUNES_A_LAS_10, null), usuarioPaciente(paciente(2L))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Es necesario elegir una especialidad");
        verify(consultaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Si la base la rechaza porque el horario se acaba de ocupar, avisa como una validación (BUG-06)")
    void siLaBaseLaRechazaPorElHorarioAvisa() {
        when(medicoRepository.existsById(1L)).thenReturn(true);
        when(medicoRepository.getReferenceById(1L)).thenReturn(medico(1L));
        // Así lo nombra MySQL, con la tabla delante.
        when(consultaRepository.saveAndFlush(any(Consulta.class)))
                .thenThrow(choqueConLaRegla("consultas.uk_consultas_medico_fecha"));

        assertThatThrownBy(() -> reserva.reservar(
                new DatosReservaConsulta(1L, null, LUNES_A_LAS_10, null), usuarioPaciente(paciente(2L))))
                .isInstanceOf(ValidacionException.class)
                .hasMessage("Ese horario se acaba de ocupar. Vuelve a intentarlo o elige otro.");
    }

    @Test
    @DisplayName("Si la base la rechaza por otra regla, el error sigue igual hasta TratadorDeErrores")
    void otroErrorDeLaBaseSigueSuCamino() {
        when(medicoRepository.existsById(1L)).thenReturn(true);
        when(medicoRepository.getReferenceById(1L)).thenReturn(medico(1L));
        var otroError = choqueConLaRegla("fk_consultas_paciente_id");
        when(consultaRepository.saveAndFlush(any(Consulta.class))).thenThrow(otroError);

        assertThatThrownBy(() -> reserva.reservar(
                new DatosReservaConsulta(1L, null, LUNES_A_LAS_10, null), usuarioPaciente(paciente(2L))))
                .isSameAs(otroError);
    }

    // ----- Ver las consultas de un paciente (recepción y administrador) -----

    @Test
    @DisplayName("Recepción ve las consultas programadas de un paciente, con su médico (BUG-04)")
    void recepcionVeLasConsultasDeUnPaciente() {
        when(pacienteRepository.existsById(2L)).thenReturn(true);
        when(consultaRepository.buscarProgramadasDelPaciente(2L, EstadoConsulta.PROGRAMADA))
                .thenReturn(List.of(consultaProgramada(medico(1L), paciente(2L))));

        var consultas = reserva.listarConsultasDeUnPaciente(2L);

        assertThat(consultas).containsExactly(new DatosDetalleConsulta(10L, 1L, "Dra. Ana Torres",
                Especialidad.CARDIOLOGIA, 2L, LUNES_A_LAS_10, EstadoConsulta.PROGRAMADA));
    }

    @Test
    @DisplayName("Con un paciente que no existe, avisa y no busca consultas")
    void conUnPacienteQueNoExisteAvisa() {
        when(pacienteRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> reserva.listarConsultasDeUnPaciente(99L))
                .isInstanceOf(ValidacionException.class)
                .hasMessage("No existe un paciente con el id informado");
        verifyNoInteractions(consultaRepository);
    }

    // ----- Cancelar -----

    @Test
    @DisplayName("Un paciente cancela su propia consulta")
    void unPacienteCancelaSuConsulta() {
        var consulta = consultaProgramada(medico(1L), paciente(2L));
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        reserva.cancelar(10L, MotivoCancelamiento.PACIENTE_DESISTIO, usuarioPaciente(paciente(2L)));

        assertThat(consulta.getEstado()).isEqualTo(EstadoConsulta.CANCELADA);
        assertThat(consulta.getMotivo()).isEqualTo(MotivoCancelamiento.PACIENTE_DESISTIO);
        verify(validadorDeCancelacion).validar(consulta);
    }

    @Test
    @DisplayName("Un paciente no puede cancelar la consulta de otro paciente")
    void unPacienteNoCancelaLaDeOtro() {
        var consulta = consultaProgramada(medico(1L), paciente(2L));
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> reserva.cancelar(
                10L, MotivoCancelamiento.PACIENTE_DESISTIO, usuarioPaciente(paciente(3L))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("no es tuya");
        assertThat(consulta.getEstado()).isEqualTo(EstadoConsulta.PROGRAMADA);
        verifyNoInteractions(validadorDeCancelacion);
    }

    @Test
    @DisplayName("Un médico no puede cancelar la consulta de otro médico")
    void unMedicoNoCancelaLaDeOtro() {
        var consulta = consultaProgramada(medico(1L), paciente(2L));
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> reserva.cancelar(
                10L, MotivoCancelamiento.MEDICO_CANCELO, usuarioMedico(medico(4L))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("no es tuya");
        assertThat(consulta.getEstado()).isEqualTo(EstadoConsulta.PROGRAMADA);
    }

    @Test
    @DisplayName("La recepción puede cancelar cualquier consulta")
    void laRecepcionCancelaCualquierConsulta() {
        var consulta = consultaProgramada(medico(1L), paciente(2L));
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        reserva.cancelar(10L, MotivoCancelamiento.OTROS, recepcionista());

        assertThat(consulta.getEstado()).isEqualTo(EstadoConsulta.CANCELADA);
    }

    @Test
    @DisplayName("Si el validador rechaza la cancelación, la consulta sigue programada")
    void siElValidadorRechazaLaCancelacionSigueProgramada() {
        var consulta = consultaProgramada(medico(1L), paciente(2L));
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        doThrow(new ValidacionException("La consulta solo se puede cancelar con al menos 24 horas de anticipación."))
                .when(validadorDeCancelacion).validar(consulta);

        assertThatThrownBy(() -> reserva.cancelar(
                10L, MotivoCancelamiento.PACIENTE_DESISTIO, usuarioPaciente(paciente(2L))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("24 horas");
        assertThat(consulta.getEstado()).isEqualTo(EstadoConsulta.PROGRAMADA);
    }

    // ----- Atender -----

    @Test
    @DisplayName("Antes de la hora de la consulta, el médico asignado no puede marcarla como atendida")
    void elMedicoNoAtiendeAntesDeLaHora() {
        var consulta = consultaProgramada(medico(1L), paciente(2L));
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        // Las 9:59: falta un minuto para la consulta.
        reserva = reservaALas(LUNES_A_LAS_10.minusMinutes(1));

        assertThatThrownBy(() -> reserva.atender(10L, usuarioMedico(medico(1L))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Solo se puede marcar como atendida desde la hora de la consulta");
        assertThat(consulta.getEstado()).isEqualTo(EstadoConsulta.PROGRAMADA);
    }

    @Test
    @DisplayName("A la hora justa de la consulta, el médico asignado ya puede marcarla como atendida")
    void elMedicoAtiendeALaHoraJusta() {
        var consulta = consultaProgramada(medico(1L), paciente(2L));
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        reserva = reservaALas(LUNES_A_LAS_10);

        reserva.atender(10L, usuarioMedico(medico(1L)));

        assertThat(consulta.getEstado()).isEqualTo(EstadoConsulta.ATENDIDA);
    }

    @Test
    @DisplayName("Pasada la hora de la consulta, el médico asignado la marca como atendida")
    void elMedicoAsignadoAtiende() {
        var consulta = consultaProgramada(medico(1L), paciente(2L));
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        var detalle = reserva.atender(10L, usuarioMedico(medico(1L)));

        assertThat(detalle.estado()).isEqualTo(EstadoConsulta.ATENDIDA);
        assertThat(consulta.getEstado()).isEqualTo(EstadoConsulta.ATENDIDA);
    }

    @Test
    @DisplayName("Otro médico no puede atender una consulta que no es suya")
    void otroMedicoNoAtiende() {
        var consulta = consultaProgramada(medico(1L), paciente(2L));
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> reserva.atender(10L, usuarioMedico(medico(4L))))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("Solo puedes atender consultas asignadas a ti");
        assertThat(consulta.getEstado()).isEqualTo(EstadoConsulta.PROGRAMADA);
    }

    // ----- Datos de prueba -----

    private static Medico medico(Long id) {
        return new Medico(id, true, "Dra. Ana Torres", null, null, null, Especialidad.CARDIOLOGIA, null);
    }

    private static Paciente paciente(Long id) {
        return new Paciente(id, "Luis Pérez", true, null, null, null, null);
    }

    private static Consulta consultaProgramada(Medico medico, Paciente paciente) {
        return new Consulta(10L, medico, paciente, LUNES_A_LAS_10, null, EstadoConsulta.PROGRAMADA);
    }

    private static Usuario usuarioPaciente(Paciente paciente) {
        var usuario = new Usuario("paciente@ejemplo.com", "sin-uso", Rol.PACIENTE);
        usuario.asignarPaciente(paciente);
        return usuario;
    }

    private static Usuario usuarioMedico(Medico medico) {
        var usuario = new Usuario("medico@ejemplo.com", "sin-uso", Rol.MEDICO);
        usuario.asignarMedico(medico);
        return usuario;
    }

    private static Usuario recepcionista() {
        return new Usuario("recepcion@ejemplo.com", "sin-uso", Rol.RECEPCIONISTA);
    }

    // Lo que llega cuando la base rechaza la consulta por la regla con ese nombre: el error de Spring envuelve el de
    // Hibernate, que trae el nombre de la regla, y ese envuelve el de MySQL.
    private static DataIntegrityViolationException choqueConLaRegla(String regla) {
        var deMySQL = new SQLIntegrityConstraintViolationException("La consulta rompe la regla " + regla);
        return new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException("could not execute statement", deMySQL, regla));
    }
}
