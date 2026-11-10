package com.voll.api.domain.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InicializadorDeAdminTest {

    private static final String CORREO = "admin@ejemplo.com";
    private static final String CONTRASENIA = "Clave-de-prueba-123";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private void ejecutar(String correo, String contrasenia) {
        new InicializadorDeAdmin(usuarioRepository, passwordEncoder, correo, contrasenia)
                .run(new DefaultApplicationArguments());
    }

    @Test
    @DisplayName("Crea el admin con la contraseña encriptada si no existe ningún ADMINISTRADOR")
    void creaElAdminSiNoExisteNinguno() {
        when(usuarioRepository.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);
        when(usuarioRepository.existsByCorreo(CORREO)).thenReturn(false);
        when(passwordEncoder.encode(CONTRASENIA)).thenReturn("hash-de-prueba");

        ejecutar(CORREO, CONTRASENIA);

        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        var admin = captor.getValue();
        assertThat(admin.getCorreo()).isEqualTo(CORREO);
        assertThat(admin.getContrasenia()).isEqualTo("hash-de-prueba");
        assertThat(admin.getRol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(admin.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    @DisplayName("Quita los espacios de los extremos del correo")
    void quitaLosEspaciosDelCorreo() {
        when(usuarioRepository.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);
        when(usuarioRepository.existsByCorreo(CORREO)).thenReturn(false);
        when(passwordEncoder.encode(CONTRASENIA)).thenReturn("hash-de-prueba");

        ejecutar("  " + CORREO + " ", CONTRASENIA);

        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getCorreo()).isEqualTo(CORREO);
    }

    @Test
    @DisplayName("No hace nada si ya existe un ADMINISTRADOR")
    void noHaceNadaSiYaExisteUnAdmin() {
        when(usuarioRepository.existsByRol(Rol.ADMINISTRADOR)).thenReturn(true);

        ejecutar(CORREO, CONTRASENIA);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Arranca sin crear nada si faltan las variables de entorno")
    void noCreaNadaSiFaltanLasVariables() {
        when(usuarioRepository.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);

        ejecutar("", "");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Falla si la contraseña tiene menos de 8 caracteres")
    void fallaSiLaContraseniaEsCorta() {
        when(usuarioRepository.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);

        assertThatThrownBy(() -> ejecutar(CORREO, "Corta1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("al menos 8");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Falla si la contraseña supera los 72 bytes que admite BCrypt")
    void fallaSiLaContraseniaSuperaElLimiteDeBcrypt() {
        when(usuarioRepository.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);
        // La "ñ" ocupa 2 bytes en UTF-8: son 37 caracteres, pero 74 bytes.
        var contraseniaLarga = "ñ".repeat(37);

        assertThatThrownBy(() -> ejecutar(CORREO, contraseniaLarga))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("72 bytes");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Falla si el correo ya pertenece a otro usuario")
    void fallaSiElCorreoYaExiste() {
        when(usuarioRepository.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);
        when(usuarioRepository.existsByCorreo(CORREO)).thenReturn(true);

        assertThatThrownBy(() -> ejecutar(CORREO, CONTRASENIA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya pertenece a otro usuario");
        verify(usuarioRepository, never()).save(any());
    }
}
