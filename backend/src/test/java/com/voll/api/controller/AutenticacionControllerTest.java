package com.voll.api.controller;

import com.voll.api.domain.usuario.AutenticacionService;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import com.voll.api.infra.exceptions.TratadorDeErrores;
import com.voll.api.infra.security.CorsConfig;
import com.voll.api.infra.security.SecurityConfigurations;
import com.voll.api.infra.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el login (POST /login) con la seguridad real: SecurityConfigurations, el AuthenticationManager
 * de Spring, AutenticacionService y TratadorDeErrores. Solo se simula la base de datos (UsuarioRepository).
 * Cubre los mensajes del login de BUG-03 de docs/revisiones/0001-revision-inicial.md.
 */
@WebMvcTest(controllers = AutenticacionController.class,
        properties = "api.security.token.secret=secreto-solo-para-pruebas")
@Import({SecurityConfigurations.class, CorsConfig.class, TokenService.class, AutenticacionService.class,
        TratadorDeErrores.class})
class AutenticacionControllerTest {

    private static final String CORREO = "usuario@ejemplo.com";
    private static final String CONTRASENIA = "Clave-de-prueba-1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Usuario usuarioConContrasenia() {
        return new Usuario(CORREO, passwordEncoder.encode(CONTRASENIA), Rol.ADMINISTRADOR);
    }

    private ResultActions iniciarSesion(String contrasenia) throws Exception {
        return mvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"" + CORREO + "\", \"contrasenia\": \"" + contrasenia + "\"}"));
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
}
