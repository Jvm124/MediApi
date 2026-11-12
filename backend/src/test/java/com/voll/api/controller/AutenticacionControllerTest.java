package com.voll.api.controller;

import com.voll.api.domain.usuario.AutenticacionService;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import com.voll.api.infra.exceptions.TratadorDeErrores;
import com.voll.api.infra.reloj.RelojDePrueba;
import com.voll.api.infra.security.CorsConfig;
import com.voll.api.infra.security.LimiteDeIntentosDeLogin;
import com.voll.api.infra.security.SecurityConfigurations;
import com.voll.api.infra.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el login (POST /login) con la seguridad real: SecurityConfigurations, el AuthenticationManager
 * de Spring, AutenticacionService, TratadorDeErrores y el límite de intentos. Solo se simula la base de datos
 * (UsuarioRepository). Cubre los mensajes del login de BUG-03 y el límite de intentos de SEC-04 de
 * docs/revisiones/0001-revision-inicial.md.
 */
@WebMvcTest(controllers = AutenticacionController.class,
        properties = "api.security.token.secret=secreto-solo-para-pruebas")
@Import({SecurityConfigurations.class, CorsConfig.class, TokenService.class, AutenticacionService.class,
        TratadorDeErrores.class, LimiteDeIntentosDeLogin.class})
class AutenticacionControllerTest {

    private static final String CORREO = "usuario@ejemplo.com";
    private static final String CONTRASENIA = "Clave-de-prueba-1";
    private static final String DEMASIADOS_INTENTOS = "Demasiados intentos fallidos. Vuelve a intentarlo en 15 minutos.";

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuracion {

        // El límite de intentos usa este reloj, que solo avanza cuando la prueba lo pide.
        @Bean
        RelojDePrueba reloj() {
            return new RelojDePrueba(Instant.parse("2026-11-10T15:00:00Z"));
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RelojDePrueba reloj;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    // Spring reutiliza el mismo contexto, y con él los contadores del límite, en todas las pruebas de esta clase.
    // Adelantar una hora deja atrás los fallos de las pruebas anteriores.
    @BeforeEach
    void dejarAtrasLosFallosDeOtrasPruebas() {
        reloj.avanzar(Duration.ofHours(1));
    }

    private Usuario usuarioConContrasenia() {
        return new Usuario(CORREO, passwordEncoder.encode(CONTRASENIA), Rol.ADMINISTRADOR);
    }

    private ResultActions iniciarSesion(String contrasenia) throws Exception {
        return iniciarSesion(CORREO, contrasenia);
    }

    private ResultActions iniciarSesion(String correo, String contrasenia) throws Exception {
        return mvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"" + correo + "\", \"contrasenia\": \"" + contrasenia + "\"}"));
    }

    private void fallarCincoVeces(String correo) throws Exception {
        for (int i = 0; i < 5; i++) {
            iniciarSesion(correo, "otra-clave").andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("Con la contraseña correcta devuelve el token")
    void contraseniaCorrecta() throws Exception {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuarioConContrasenia());

        iniciarSesion(CONTRASENIA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("Con una contraseña incorrecta responde 401 con un mensaje claro")
    void contraseniaIncorrecta() throws Exception {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuarioConContrasenia());

        iniciarSesion("otra-clave")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
    }

    @Test
    @DisplayName("Con un correo que no existe responde lo mismo que con una contraseña incorrecta")
    void correoQueNoExiste() throws Exception {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(null);

        iniciarSesion(CONTRASENIA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
    }

    @Test
    @DisplayName("Con una cuenta suspendida responde 401 y lo dice")
    void cuentaSuspendida() throws Exception {
        var usuario = usuarioConContrasenia();
        usuario.suspender();
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuario);

        iniciarSesion(CONTRASENIA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Tu cuenta está suspendida o dada de baja."));
    }

    @Test
    @DisplayName("Con una cuenta suspendida el aviso sale aunque la contraseña sea incorrecta (decisión aceptada)")
    void cuentaSuspendidaConContraseniaIncorrecta() throws Exception {
        var usuario = usuarioConContrasenia();
        usuario.suspender();
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuario);

        // Spring revisa el estado de la cuenta antes que la contraseña. Se aceptó porque el registro
        // ya revela si un correo tiene cuenta. Ver BUG-03 en docs/revisiones/0001-revision-inicial.md.
        iniciarSesion("otra-clave")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Tu cuenta está suspendida o dada de baja."));
    }

    @Test
    @DisplayName("Si falla la base de datos al buscar al usuario, responde 500 y no «contraseña incorrecta»")
    void fallaLaBaseDeDatos() throws Exception {
        when(usuarioRepository.findByCorreo(CORREO)).thenThrow(new RuntimeException("Sin conexión con la base de datos"));

        iniciarSesion(CONTRASENIA)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Intenta de nuevo más tarde."));
    }

    @Test
    @DisplayName("Tras 5 contraseñas incorrectas responde 429, aunque después llegue la correcta (SEC-04)")
    void bloqueaTrasCincoFallos() throws Exception {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuarioConContrasenia());
        fallarCincoVeces(CORREO);

        iniciarSesion(CONTRASENIA)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "900"))
                .andExpect(jsonPath("$.detail").value(DEMASIADOS_INTENTOS));
    }

    @Test
    @DisplayName("El bloqueo es solo para ese correo: otro correo entra")
    void otroCorreoNoSeBloquea() throws Exception {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuarioConContrasenia());
        when(usuarioRepository.findByCorreo("otra@ejemplo.com"))
                .thenReturn(new Usuario("otra@ejemplo.com", passwordEncoder.encode(CONTRASENIA), Rol.PACIENTE));
        fallarCincoVeces(CORREO);

        iniciarSesion("otra@ejemplo.com", CONTRASENIA).andExpect(status().isOk());
    }

    @Test
    @DisplayName("A los 15 minutos se puede volver a entrar")
    void aLosQuinceMinutosSeVuelveAEntrar() throws Exception {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuarioConContrasenia());
        fallarCincoVeces(CORREO);
        iniciarSesion(CONTRASENIA).andExpect(status().isTooManyRequests());

        reloj.avanzar(Duration.ofMinutes(15));

        iniciarSesion(CONTRASENIA).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Las variantes del correo que MySQL trata como el mismo usuario comparten el contador")
    void lasVariantesDelCorreoCompartenElContador() throws Exception {
        // MySQL encuentra al mismo usuario con mayúsculas, tildes o letras de ancho completo (la colación de la
        // tabla usuarios). Aquí se simula: las tres variantes devuelven al usuario guardado como CORREO.
        // La segunda variante empieza con una ú con tilde (U+00FA) y la tercera con una u de ancho completo
        // (U+FF55), que se ve casi igual que una u normal pero es otro carácter.
        var usuario = usuarioConContrasenia();
        var variantes = List.of("USUARIO@ejemplo.com", "\u00FAsuario@ejemplo.com", "\uFF55suario@ejemplo.com");
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuario);
        for (var variante : variantes) {
            when(usuarioRepository.findByCorreo(variante)).thenReturn(usuario);
        }
        for (int i = 0; i < 5; i++) {
            iniciarSesion(variantes.get(i % variantes.size()), "otra-clave").andExpect(status().isUnauthorized());
        }

        iniciarSesion(CONTRASENIA)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(DEMASIADOS_INTENTOS));
    }

    @Test
    @DisplayName("Un correo que no existe también se bloquea: si no, el bloqueo revelaría qué correos tienen cuenta")
    void unCorreoQueNoExisteTambienSeBloquea() throws Exception {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(null);
        fallarCincoVeces(CORREO);

        iniciarSesion(CONTRASENIA)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(DEMASIADOS_INTENTOS));
    }

    @Test
    @DisplayName("Si la base de datos falla al buscar el correo, el intento no cuenta para el límite")
    void losErroresDelServidorNoCuentan() throws Exception {
        var sinConexion = new RuntimeException("Sin conexión con la base de datos");
        // Las 5 primeras búsquedas fallan; después, la base de datos vuelve.
        when(usuarioRepository.findByCorreo(CORREO))
                .thenThrow(sinConexion, sinConexion, sinConexion, sinConexion, sinConexion)
                .thenReturn(usuarioConContrasenia());
        for (int i = 0; i < 5; i++) {
            iniciarSesion(CONTRASENIA).andExpect(status().isInternalServerError());
        }

        iniciarSesion(CONTRASENIA).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Si la base de datos falla mientras se comprueba la contraseña, el intento tampoco cuenta")
    void losErroresDelServidorAlComprobarLaContraseniaNoCuentan() throws Exception {
        var usuario = usuarioConContrasenia();
        var sinConexion = new RuntimeException("Sin conexión con la base de datos");
        // Cada login busca al usuario dos veces: primero AutenticacionService.correoGuardado, para el límite, y
        // después Spring Security, para comprobar la contraseña. Aquí, en 5 logins seguidos, la primera búsqueda
        // funciona y la segunda falla. Spring Security convierte ese fallo en una AuthenticationServiceException.
        var busquedas = when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuario).thenThrow(sinConexion);
        for (int i = 1; i < 5; i++) {
            busquedas = busquedas.thenReturn(usuario).thenThrow(sinConexion);
        }
        busquedas.thenReturn(usuario);
        for (int i = 0; i < 5; i++) {
            iniciarSesion(CONTRASENIA).andExpect(status().isInternalServerError());
        }

        iniciarSesion(CONTRASENIA).andExpect(status().isOk());
    }
}
