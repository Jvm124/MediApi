package com.voll.api.infra.exceptions;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.usuario.UsuarioRepository;
import com.voll.api.infra.security.TokenService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba, con la misma configuración de Spring MVC y Jackson que usa la app, que los errores
 * salen con el formato ProblemDetail (RFC 9457), con el código correcto y con el mensaje para el
 * usuario en "detail", que es lo que muestra el frontend. Cubre BUG-02 y BUG-03 de
 * docs/revisiones/0001-revision-inicial.md (los mensajes del login están en AutenticacionControllerTest).
 * La seguridad se desactiva (addFilters = false) y no se usa la base de datos.
 */
@WebMvcTest(controllers = TratadorDeErroresTest.ControladorDePrueba.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TratadorDeErrores.class, TratadorDeErroresTest.ControladorDePrueba.class})
class TratadorDeErroresTest {

    @RestController
    static class ControladorDePrueba {
        @GetMapping("/regla-rota")
        void reglaRota() {
            throw new ValidacionException("Ya existe un paciente con ese email");
        }

        @GetMapping("/no-existe")
        void noExiste() {
            throw new EntityNotFoundException("Unable to find com.voll.api.domain.medico.Medico with id 99");
        }

        @PostMapping("/datos")
        void datos(@RequestBody @Valid DatosDePrueba datos) {
        }

        @GetMapping("/sin-permiso")
        void sinPermiso() {
            throw new AccessDeniedException("Access Denied");
        }

        @GetMapping("/duplicado")
        void duplicado() {
            throw new DataIntegrityViolationException("Duplicate entry 'ana@ejemplo.com' for key 'usuarios.correo'");
        }

        @GetMapping("/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalle interno que no debe salir");
        }
    }

    record DatosDePrueba(@NotBlank String nombre) {
    }

    @Autowired
    private MockMvc mvc;

    // SecurityFilter los necesita para crearse, aunque aquí no se ejecute.
    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Una regla de negocio rota responde 400 con su mensaje en detail")
    void reglaDeNegocioRota() throws Exception {
        mvc.perform(get("/regla-rota"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Ya existe un paciente con ese email"));
    }

    @Test
    @DisplayName("Un registro que no existe responde 404 sin enviar el mensaje interno")
    void registroQueNoExiste() throws Exception {
        mvc.perform(get("/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No se encontró el registro solicitado."));
    }

    @Test
    @DisplayName("Un campo inválido responde 400 con el campo en detail y en la lista errores")
    void campoInvalido() throws Exception {
        mvc.perform(post("/datos").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("nombre: ")))
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"));
    }

    @Test
    @DisplayName("Sin el rol necesario responde 403 con un mensaje claro")
    void sinPermiso() throws Exception {
        mvc.perform(get("/sin-permiso"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("No tienes permiso para hacer esto."));
    }

    @Test
    @DisplayName("Un dato repetido que llega a la base de datos responde 409, no 500")
    void datoRepetido() throws Exception {
        mvc.perform(get("/duplicado"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Ya existe un registro con esos datos (por ejemplo, el mismo correo o documento)."))
                .andExpect(content().string(not(containsString("Duplicate entry"))));
    }

    @Test
    @DisplayName("Un error inesperado responde 500 con un mensaje seguro, sin el detalle interno")
    void errorInesperado() throws Exception {
        mvc.perform(get("/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Intenta de nuevo más tarde."))
                .andExpect(content().string(not(containsString("detalle interno"))));
    }

    @Test
    @DisplayName("Un JSON mal formado sigue respondiendo 400, no el 500 genérico")
    void jsonMalFormado() throws Exception {
        mvc.perform(post("/datos").contentType(MediaType.APPLICATION_JSON).content("{nombre"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
