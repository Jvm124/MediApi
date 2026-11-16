package com.voll.api.domain.usuario;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.medico.Especialidad;
import com.voll.api.domain.medico.Medico;
import com.voll.api.domain.paciente.Paciente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba los cambios de estado de una cuenta (Usuario), sin Spring ni base de datos.
 * La cuenta decide quién puede iniciar sesión. La ficha (Medico o Paciente) decide quién aparece en las listas y a
 * quién se le puede reservar. Desde BUG-05, la ficha sigue a su cuenta: solo está activa si la cuenta está ACTIVA
 * (docs/adr/0006-la-ficha-sigue-a-la-cuenta.md).
 */
class UsuarioTest {

    // ----- La ficha sigue a la cuenta (BUG-05) -----

    @Test
    @DisplayName("Suspender la cuenta de un médico también desactiva su ficha")
    void suspenderAUnMedico() {
        var medico = medico(true);
        var cuenta = cuentaDe(medico, EstadoUsuario.ACTIVO);

        cuenta.suspender();

        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.SUSPENDIDO);
        assertThat(medico.getActivo()).isFalse();
    }

    @Test
    @DisplayName("Dar de baja la cuenta de un paciente también desactiva su ficha")
    void darDeBajaAUnPaciente() {
        var paciente = paciente(true);
        var cuenta = cuentaDe(paciente, EstadoUsuario.ACTIVO);

        cuenta.darDeBaja();

        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.BAJA);
        assertThat(paciente.getActivo()).isFalse();
    }

    @Test
    @DisplayName("Una cuenta suspendida también se puede dar de baja, y su ficha sigue desactivada")
    void darDeBajaAUnaCuentaSuspendida() {
        var medico = medico(false);
        var cuenta = cuentaDe(medico, EstadoUsuario.SUSPENDIDO);

        cuenta.darDeBaja();

        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.BAJA);
        assertThat(medico.getActivo()).isFalse();
    }

    @Test
    @DisplayName("«Reincorporar» a un médico dado de baja también activa su ficha, y puede volver a entrar")
    void reincorporarAUnMedicoDadoDeBaja() {
        var medico = medico(false);
        var cuenta = cuentaDe(medico, EstadoUsuario.BAJA);

        cuenta.reactivar();

        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(medico.getActivo()).isTrue();
        assertThat(cuenta.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Reactivar a un paciente suspendido también activa su ficha")
    void reactivarAUnPacienteSuspendido() {
        var paciente = paciente(false);
        var cuenta = cuentaDe(paciente, EstadoUsuario.SUSPENDIDO);

        cuenta.reactivar();

        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(paciente.getActivo()).isTrue();
    }

    @Test
    @DisplayName("Una cuenta sin ficha, como la de un administrador, cambia de estado sin problemas")
    void unaCuentaSinFicha() {
        var cuenta = administrador(EstadoUsuario.ACTIVO);

        cuenta.suspender();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.SUSPENDIDO);
        cuenta.reactivar();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        cuenta.darDeBaja();
        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.BAJA);
    }

    // ----- Cambios que no se permiten (TEST-01) -----
    // La ficha empieza al revés de como la dejaría la acción. Así se comprueba que, si la acción no se permite,
    // no se toca nada.

    @ParameterizedTest(name = "desde {0}")
    @EnumSource(value = EstadoUsuario.class, names = {"SUSPENDIDO", "BAJA"})
    @DisplayName("Solo se puede suspender una cuenta activa")
    void soloSeSuspendeUnaCuentaActiva(EstadoUsuario estado) {
        var medico = medico(true);
        var cuenta = cuentaDe(medico, estado);

        assertThatThrownBy(cuenta::suspender)
                .isInstanceOf(ValidacionException.class)
                .hasMessage("Solo se puede suspender a un usuario activo");
        assertThat(cuenta.getEstado()).isEqualTo(estado);
        assertThat(medico.getActivo()).isTrue();
    }

    @Test
    @DisplayName("Una cuenta activa no se puede reactivar")
    void unaCuentaActivaNoSeReactiva() {
        var medico = medico(false);
        var cuenta = cuentaDe(medico, EstadoUsuario.ACTIVO);

        assertThatThrownBy(cuenta::reactivar)
                .isInstanceOf(ValidacionException.class)
                .hasMessage("Solo se puede reactivar a un usuario suspendido o dado de baja");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(medico.getActivo()).isFalse();
    }

    @Test
    @DisplayName("Una cuenta dada de baja no se puede dar de baja otra vez")
    void unaCuentaDeBajaNoSeDaDeBajaOtraVez() {
        var paciente = paciente(true);
        var cuenta = cuentaDe(paciente, EstadoUsuario.BAJA);

        assertThatThrownBy(cuenta::darDeBaja)
                .isInstanceOf(ValidacionException.class)
                .hasMessage("Solo se puede dar de baja a un usuario activo o suspendido");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoUsuario.BAJA);
        assertThat(paciente.getActivo()).isTrue();
    }

    // ----- Quién puede iniciar sesión (isEnabled) -----

    @Test
    @DisplayName("Entra quien tiene la cuenta activa y la ficha activa")
    void entraConCuentaYFichaActivas() {
        assertThat(cuentaDe(medico(true), EstadoUsuario.ACTIVO).isEnabled()).isTrue();
    }

    @ParameterizedTest(name = "con la cuenta {0}")
    @EnumSource(value = EstadoUsuario.class, names = {"SUSPENDIDO", "BAJA"})
    @DisplayName("No entra quien tiene la cuenta suspendida o de baja")
    void noEntraConLaCuentaSuspendidaODeBaja(EstadoUsuario estado) {
        assertThat(cuentaDe(paciente(true), estado).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("No entra quien tiene la ficha desactivada, aunque su cuenta diga ACTIVO")
    void noEntraConLaFichaDesactivada() {
        // Desde BUG-05 no debería pasar, salvo que alguien cambie los datos a mano: isEnabled() mira las dos cosas.
        assertThat(cuentaDe(medico(false), EstadoUsuario.ACTIVO).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Una cuenta activa sin ficha, como la de un administrador, puede entrar")
    void entraUnaCuentaSinFicha() {
        assertThat(administrador(EstadoUsuario.ACTIVO).isEnabled()).isTrue();
    }

    private static Medico medico(boolean activo) {
        return new Medico(1L, activo, "Dra. Ana Torres", null, null, null, Especialidad.CARDIOLOGIA, null);
    }

    private static Paciente paciente(boolean activo) {
        return new Paciente(2L, "Luis Pérez", activo, null, null, null, null);
    }

    // Las cuentas se arman con el constructor de todos los campos (lo genera Lombok), para empezar en el estado
    // que pide cada prueba.
    private static Usuario cuentaDe(Medico medico, EstadoUsuario estado) {
        return new Usuario(10L, "medico@ejemplo.com", "sin-uso", Rol.MEDICO, null, medico, estado);
    }

    private static Usuario cuentaDe(Paciente paciente, EstadoUsuario estado) {
        return new Usuario(11L, "paciente@ejemplo.com", "sin-uso", Rol.PACIENTE, paciente, null, estado);
    }

    private static Usuario administrador(EstadoUsuario estado) {
        return new Usuario(12L, "admin@ejemplo.com", "sin-uso", Rol.ADMINISTRADOR, null, null, estado);
    }
}
