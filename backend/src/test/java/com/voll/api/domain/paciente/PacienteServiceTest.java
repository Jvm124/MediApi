package com.voll.api.domain.paciente;

import com.voll.api.domain.usuario.EstadoUsuario;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Prueba la baja de un paciente (PacienteService.eliminar), sin Spring ni base de datos: los repositorios son
 * simulados con Mockito. La puede pedir un administrador o el mismo paciente. Desde BUG-05, la baja apaga la ficha
 * y también da de baja la cuenta, si el paciente tiene una.
 */
@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    private static final Long ID_PACIENTE = 2L;

    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder; // PacienteService lo pide en su constructor; estas pruebas no lo usan

    @InjectMocks
    private PacienteService pacienteService;

    @Test
    @DisplayName("Si un administrador da de baja a un paciente, su cuenta también queda de baja (BUG-05)")
    void unAdministradorDaDeBajaAUnPaciente() {
        var paciente = paciente();
        var cuenta = cuentaDe(paciente, EstadoUsuario.ACTIVO);
        when(pacienteRepository.findById(ID_PACIENTE)).thenReturn(Optional.of(paciente));
        when(usuarioRepository.findByPacienteId(ID_PACIENTE)).thenReturn(Optional.of(cuenta));

        pacienteService.eliminar(ID_PACIENTE, administrador());

        assertThat(paciente.getActivo()).isFalse();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.BAJA);
    }

    @Test
    @DisplayName("Si un paciente se da de baja a sí mismo, su cuenta también queda de baja (BUG-05)")
    void unPacienteSeDaDeBaja() {
        var paciente = paciente();
        // La cuenta de la sesión la carga el filtro de seguridad, fuera de la transacción, y sus cambios no se
        // guardarían. Por eso son dos objetos: el servicio tiene que dar de baja la que le devuelve el repositorio.
        var cuentaDeLaSesion = cuentaDe(paciente, EstadoUsuario.ACTIVO);
        var cuenta = cuentaDe(paciente, EstadoUsuario.ACTIVO);
        when(pacienteRepository.findById(ID_PACIENTE)).thenReturn(Optional.of(paciente));
        when(usuarioRepository.findByPacienteId(ID_PACIENTE)).thenReturn(Optional.of(cuenta));

        pacienteService.eliminar(ID_PACIENTE, cuentaDeLaSesion);

        assertThat(paciente.getActivo()).isFalse();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.BAJA);
    }

    @Test
    @DisplayName("Un paciente de mostrador, sin cuenta, también se puede dar de baja")
    void unPacienteSinCuenta() {
        var paciente = paciente();
        when(pacienteRepository.findById(ID_PACIENTE)).thenReturn(Optional.of(paciente));
        // A findByPacienteId no se le prepara respuesta: Mockito devuelve Optional.empty(), o sea, no tiene cuenta.

        pacienteService.eliminar(ID_PACIENTE, administrador());

        assertThat(paciente.getActivo()).isFalse();
    }

    @Test
    @DisplayName("Si la cuenta del paciente ya estaba de baja, la baja no falla")
    void unPacienteConLaCuentaYaDeBaja() {
        var paciente = paciente();
        var cuenta = cuentaDe(paciente, EstadoUsuario.BAJA);
        when(pacienteRepository.findById(ID_PACIENTE)).thenReturn(Optional.of(paciente));
        when(usuarioRepository.findByPacienteId(ID_PACIENTE)).thenReturn(Optional.of(cuenta));

        pacienteService.eliminar(ID_PACIENTE, administrador());

        assertThat(paciente.getActivo()).isFalse();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.BAJA);
    }

    private static Paciente paciente() {
        return new Paciente(ID_PACIENTE, "Luis Pérez", true, null, null, null, null);
    }

    private static Usuario cuentaDe(Paciente paciente, EstadoUsuario estado) {
        return new Usuario(11L, "paciente@ejemplo.com", "sin-uso", Rol.PACIENTE, paciente, null, estado);
    }

    private static Usuario administrador() {
        return new Usuario("admin@ejemplo.com", "sin-uso", Rol.ADMINISTRADOR);
    }
}
