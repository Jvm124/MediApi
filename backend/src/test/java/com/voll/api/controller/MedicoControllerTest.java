package com.voll.api.controller;

import com.voll.api.domain.medico.MedicoService;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import com.voll.api.infra.security.CorsConfig;
import com.voll.api.infra.security.SecurityConfigurations;
import com.voll.api.infra.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba quién puede pedir la lista de médicos para reservar (GET /medicos/disponibles), con la seguridad
 * real: SecurityConfigurations, SecurityFilter, TokenService y el @PreAuthorize del controlador. Solo se
 * simulan la base de datos (UsuarioRepository) y MedicoService. Cubre el paso 2 de BUG-04 de
 * docs/revisiones/0001-revision-inicial.md. También prueba que la lista del administrador (GET /medicos)
 * entiende el orden que pide el frontend desde BUG-07.
 */
@WebMvcTest(controllers = MedicoController.class,
        properties = "api.security.token.secret=secreto-solo-para-pruebas")
@Import({SecurityConfigurations.class, CorsConfig.class, TokenService.class})
class MedicoControllerTest {

    private static final String CORREO = "usuario@ejemplo.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private MedicoService medicoService;

    // RECEPCIONISTA reserva para un paciente, así que necesita la lista igual que el paciente (BUG-04).
    // El médico y el administrador no reservan consultas.
    @ParameterizedTest(name = "{0} recibe {1}")
    @CsvSource({"PACIENTE, 200", "RECEPCIONISTA, 200", "MEDICO, 403", "ADMINISTRADOR, 403"})
    @DisplayName("La lista de médicos para reservar es del paciente y de recepción, no del médico ni del admin")
    void listaDeMedicosParaReservar(Rol rol, int estadoEsperado) throws Exception {
        var usuario = new Usuario(CORREO, "hash", rol);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuario);
        when(medicoService.listarDisponibles()).thenReturn(List.of());

        mvc.perform(get("/medicos/disponibles")
                        .header("Authorization", "Bearer " + tokenService.generateToken(usuario)))
                .andExpect(status().is(estadoEsperado));
    }

    // El frontend pide la lista con sort=nombre,id: por nombre y, si dos médicos se llaman igual, por id, para que
    // el orden de los empates no cambie de una página a otra (BUG-07). Spring lo tiene que entender como dos
    // órdenes, y no como una sola propiedad llamada «nombre,id».
    @Test
    @DisplayName("sort=nombre,id llega al servicio como dos órdenes: por nombre y luego por id")
    void listaPorNombreYLuegoPorId() throws Exception {
        var admin = new Usuario(CORREO, "hash", Rol.ADMINISTRADOR);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(admin);
        when(medicoService.listar(any())).thenReturn(Page.empty());

        mvc.perform(get("/medicos")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "nombre,id")
                        .header("Authorization", "Bearer " + tokenService.generateToken(admin)))
                .andExpect(status().isOk());

        verify(medicoService).listar(PageRequest.of(1, 10, Sort.by("nombre", "id")));
    }
}
