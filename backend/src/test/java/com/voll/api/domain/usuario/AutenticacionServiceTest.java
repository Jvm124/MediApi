package com.voll.api.domain.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AutenticacionService autenticacionService;

    @Test
    @DisplayName("Devuelve el usuario si el correo existe")
    void devuelveElUsuarioSiElCorreoExiste() {
        var usuario = new Usuario("usuario@ejemplo.com", "hash", Rol.PACIENTE);
        when(usuarioRepository.findByCorreo("usuario@ejemplo.com")).thenReturn(usuario);

        assertThat(autenticacionService.loadUserByUsername("usuario@ejemplo.com")).isSameAs(usuario);
    }

    @Test
    @DisplayName("Lanza UsernameNotFoundException si el correo no existe, en vez de devolver null")
    void lanzaExcepcionSiElCorreoNoExiste() {
        when(usuarioRepository.findByCorreo("nadie@ejemplo.com")).thenReturn(null);

        assertThatThrownBy(() -> autenticacionService.loadUserByUsername("nadie@ejemplo.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
