package com.voll.api.controller;

import com.voll.api.domain.paciente.DatosDetallePaciente;
import com.voll.api.domain.paciente.PacienteService;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import com.voll.api.infra.security.CorsConfig;
import com.voll.api.infra.security.SecurityConfigurations;
import com.voll.api.infra.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba que el alta por mostrador (POST /pacientes/registro-staff) exige el correo, como la tabla pacientes
 * (V2: email NOT NULL UNIQUE) y el registro web. Así se decidió en
 * docs/adr/0007-el-correo-del-paciente-es-obligatorio.md, y cierra la duda que dejó BUG-04 en
 * docs/revisiones/0001-revision-inicial.md. Usa la seguridad real (SecurityConfigurations, SecurityFilter,
 * TokenService y los @PreAuthorize del controlador); solo se simulan la base de datos (UsuarioRepository) y
 * PacienteService. También prueba que la lista de pacientes (GET /pacientes) entiende el orden que pide el
 * frontend desde BUG-07.
 */
@WebMvcTest(controllers = PacienteController.class,
        properties = "api.security.token.secret=secreto-solo-para-pruebas")
@Import({SecurityConfigurations.class, CorsConfig.class, TokenService.class})
class PacienteControllerTest {

    private static final String CORREO = "usuario@ejemplo.com";
    private static final String CORREO_PACIENTE = "paciente@ejemplo.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private PacienteService pacienteService;

    // Sin correo, o con uno vacío o en blanco, el alta se frena en la validación: 400 con el error en el campo
    // email, y PacienteService ni se entera. Con "   " también falla @Email, así que llegan dos errores de email.
    @ParameterizedTest(name = "email = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("Sin correo no hay alta por mostrador: responde 400 con el error en email")
    void sinCorreoNoHayAlta(String email) throws Exception {
        mvc.perform(post("/pacientes/registro-staff")
                        .header("Authorization", tokenDe(Rol.RECEPCIONISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(altaCon(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isNotEmpty())
                .andExpect(jsonPath("$.errores[*].campo", everyItem(is("email"))));

        verifyNoInteractions(pacienteService);
    }

    @Test
    @DisplayName("Con un correo válido, recepción da de alta al paciente: responde 201")
    void conCorreoHayAlta() throws Exception {
        when(pacienteService.registrarPorStaff(any())).thenReturn(new DatosDetallePaciente(
                5L, "Paciente De Prueba", CORREO_PACIENTE, "987654321", "12345678", null));

        mvc.perform(post("/pacientes/registro-staff")
                        .header("Authorization", tokenDe(Rol.RECEPCIONISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(altaCon(CORREO_PACIENTE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));

        verify(pacienteService).registrarPorStaff(argThat(datos -> CORREO_PACIENTE.equals(datos.email())));
    }

    // Como en la lista de médicos (BUG-07): recepción pide los pacientes con sort=nombre,id, por nombre y, si dos
    // se llaman igual, por id. Spring lo tiene que entender como dos órdenes, en lugar del nombre que pone
    // @PageableDefault.
    @Test
    @DisplayName("sort=nombre,id llega al servicio como dos órdenes: por nombre y luego por id")
    void listaPorNombreYLuegoPorId() throws Exception {
        when(pacienteService.listar(any())).thenReturn(Page.empty());

        mvc.perform(get("/pacientes")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "nombre,id")
                        .header("Authorization", tokenDe(Rol.RECEPCIONISTA)))
                .andExpect(status().isOk());

        verify(pacienteService).listar(PageRequest.of(1, 10, Sort.by("nombre", "id")));
    }

    // Un alta por mostrador con datos de ejemplo, que solo cambia en el correo (null va como null en el JSON).
    private static String altaCon(String email) {
        var correo = email == null ? "null" : "\"" + email + "\"";
        return """
                {
                  "nombre": "Paciente De Prueba",
                  "telefono": "987654321",
                  "documento": "12345678",
                  "email": %s,
                  "direccion": {
                    "calle": "Calle de Prueba",
                    "numero": "123",
                    "barrio": "Centro",
                    "ciudad": "Lima",
                    "codigo_postal": "15001",
                    "estado": "Lima"
                  }
                }
                """.formatted(correo);
    }

    // Un usuario con ese rol, que el filtro de seguridad encuentra por su correo, y la cabecera con su token.
    private String tokenDe(Rol rol) {
        var usuario = new Usuario(CORREO, "hash", rol);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuario);
        return "Bearer " + tokenService.generateToken(usuario);
    }
}
