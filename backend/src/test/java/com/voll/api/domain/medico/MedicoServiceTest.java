package com.voll.api.domain.medico;

import com.voll.api.domain.ValidacionException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Prueba la baja de un médico desde «Médicos activos» (MedicoService.eliminar), sin Spring ni base de datos: los
 * repositorios son simulados con Mockito. Desde BUG-05, la baja apaga la ficha y también da de baja la cuenta.
 */
@ExtendWith(MockitoExtension.class)
class MedicoServiceTest {

    private static final Long ID_MEDICO = 1L;

    @Mock
    private MedicoRepository medicoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder; // MedicoService lo pide en su constructor; estas pruebas no lo usan

    @InjectMocks
    private MedicoService medicoService;

    @Test
    @DisplayName("Dar de baja a un médico también da de baja su cuenta")
    void darDeBajaAUnMedico() {
        var medico = medico(true);
        var cuenta = cuentaDe(medico, EstadoUsuario.ACTIVO);
        when(medicoRepository.findById(ID_MEDICO)).thenReturn(Optional.of(medico));
        when(usuarioRepository.findByMedicoId(ID_MEDICO)).thenReturn(Optional.of(cuenta));

        medicoService.eliminar(ID_MEDICO);

        assertThat(medico.getActivo()).isFalse();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.BAJA);
    }

    @Test
    @DisplayName("Si la cuenta del médico ya estaba de baja, la baja no falla (BUG-05)")
    void darDeBajaAUnMedicoConLaCuentaYaDeBaja() {
        // Antes de BUG-05 pasaba así: el administrador daba de baja la cuenta en el panel de usuarios y la ficha
        // seguía activa. Al darlo de baja después en «Médicos activos», salía un error y no se guardaba nada.
        var medico = medico(true);
        var cuenta = cuentaDe(medico, EstadoUsuario.BAJA);
        when(medicoRepository.findById(ID_MEDICO)).thenReturn(Optional.of(medico));
        when(usuarioRepository.findByMedicoId(ID_MEDICO)).thenReturn(Optional.of(cuenta));

        medicoService.eliminar(ID_MEDICO);

        assertThat(medico.getActivo()).isFalse();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.BAJA);
    }

    @Test
    @DisplayName("Un médico sin cuenta también se puede dar de baja")
    void darDeBajaAUnMedicoSinCuenta() {
        var medico = medico(true);
        when(medicoRepository.findById(ID_MEDICO)).thenReturn(Optional.of(medico));
        // A findByMedicoId no se le prepara respuesta: Mockito devuelve Optional.empty(), o sea, no tiene cuenta.

        medicoService.eliminar(ID_MEDICO);

        assertThat(medico.getActivo()).isFalse();
    }

    @Test
    @DisplayName("Un médico suspendido o dado de baja no se vuelve a dar de baja")
    void darDeBajaAUnMedicoInactivo() {
        when(medicoRepository.findById(ID_MEDICO)).thenReturn(Optional.of(medico(false)));

        assertThatThrownBy(() -> medicoService.eliminar(ID_MEDICO))
                .isInstanceOf(ValidacionException.class)
                .hasMessage("El médico está suspendido o dado de baja");
        verifyNoInteractions(usuarioRepository);
    }

    private static Medico medico(boolean activo) {
        return new Medico(ID_MEDICO, activo, "Dra. Ana Torres", null, null, null, Especialidad.CARDIOLOGIA, null);
    }

    private static Usuario cuentaDe(Medico medico, EstadoUsuario estado) {
        return new Usuario(10L, "medico@ejemplo.com", "sin-uso", Rol.MEDICO, null, medico, estado);
    }
}
