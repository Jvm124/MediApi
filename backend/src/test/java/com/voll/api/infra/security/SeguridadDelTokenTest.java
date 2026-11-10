package com.voll.api.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la cadena de seguridad real (SecurityConfigurations, SecurityFilter y TokenService)
 * contra una ruta protegida de prueba. Solo se simula la base de datos (UsuarioRepository).
 * Cubre SEC-02 y SEC-03 de docs/revisiones/0001-revision-inicial.md, y la duración del token de SEC-07.
 */
@WebMvcTest(controllers = SeguridadDelTokenTest.ControladorDePrueba.class,
        properties = "api.security.token.secret=" + SeguridadDelTokenTest.SECRETO)
@Import({SecurityConfigurations.class, CorsConfig.class, TokenService.class, SeguridadDelTokenTest.ControladorDePrueba.class})
class SeguridadDelTokenTest {

    static final String SECRETO = "secreto-solo-para-pruebas";
    private static final String CORREO = "usuario@ejemplo.com";
    private static final String RUTA = "/ruta-protegida";

    @RestController
    static class ControladorDePrueba {
        @GetMapping(RUTA)
        String rutaProtegida() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Sin token responde 401")
    void sinTokenResponde401() throws Exception {
        mvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con un token alterado responde 401")
    void conTokenAlteradoResponde401() throws Exception {
        mvc.perform(get(RUTA).header("Authorization", "Bearer token-alterado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con un token vencido responde 401")
    void conTokenVencidoResponde401() throws Exception {
        var tokenVencido = JWT.create()
                .withIssuer("API Voll.med")
                .withSubject(CORREO)
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256(SECRETO));

        mvc.perform(get(RUTA).header("Authorization", "Bearer " + tokenVencido))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con el token de un usuario suspendido responde 401")
    void conUsuarioSuspendidoResponde401() throws Exception {
        var usuario = new Usuario(CORREO, "hash", Rol.ADMINISTRADOR);
        usuario.suspender();
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuario);

        mvc.perform(get(RUTA).header("Authorization", "Bearer " + tokenService.generateToken(usuario)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con el token de un usuario que ya no existe responde 401")
    void conUsuarioInexistenteResponde401() throws Exception {
        var usuarioBorrado = new Usuario(CORREO, "hash", Rol.ADMINISTRADOR);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(null);

        mvc.perform(get(RUTA).header("Authorization", "Bearer " + tokenService.generateToken(usuarioBorrado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con el token de un usuario activo deja pasar")
    void conUsuarioActivoDejaPasar() throws Exception {
        var usuario = new Usuario(CORREO, "hash", Rol.ADMINISTRADOR);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuario);

        mvc.perform(get(RUTA).header("Authorization", "Bearer " + tokenService.generateToken(usuario)))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    @DisplayName("El token vence 2 horas después de crearse, aunque el servidor tenga hora UTC")
    void elTokenVenceEnDosHorasAunqueElServidorTengaHoraUtc() {
        var zonaDelEquipo = TimeZone.getDefault();
        // Como el CI de GitHub o muchos servidores de la nube.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            var antes = Instant.now();
            var token = tokenService.generateToken(new Usuario(CORREO, "hash", Rol.ADMINISTRADOR));
            var despues = Instant.now();

            // El token guarda el vencimiento en segundos enteros; por eso se recorta "antes".
            assertThat(JWT.decode(token).getExpiresAtAsInstant())
                    .isBetween(antes.plus(Duration.ofHours(2)).truncatedTo(ChronoUnit.SECONDS),
                            despues.plus(Duration.ofHours(2)));
        } finally {
            TimeZone.setDefault(zonaDelEquipo);
        }
    }
}
