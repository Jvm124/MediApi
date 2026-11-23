package com.voll.api.controller;

import com.voll.api.domain.consulta.DatosDetalleConsulta;
import com.voll.api.domain.consulta.EstadoConsulta;
import com.voll.api.domain.consulta.MotivoCancelamiento;
import com.voll.api.domain.consulta.ReservaDeConsultas;
import com.voll.api.domain.medico.Especialidad;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba lo que usa el panel de recepción para ver y cancelar las consultas de un paciente (paso 3 de BUG-04 de
 * docs/revisiones/0001-revision-inicial.md): quién puede pedir GET /consultas/paciente/{id}, y que recepción
 * cancela con DELETE /consultas/{id} y el motivo en el cuerpo. Usa la seguridad real (SecurityConfigurations,
 * SecurityFilter, TokenService y los @PreAuthorize del controlador); solo se simulan la base de datos
 * (UsuarioRepository) y ReservaDeConsultas.
 */
@WebMvcTest(controllers = ConsultaController.class,
        properties = "api.security.token.secret=secreto-solo-para-pruebas")
@Import({SecurityConfigurations.class, CorsConfig.class, TokenService.class})
class ConsultaControllerTest {

    private static final String CORREO = "usuario@ejemplo.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private ReservaDeConsultas reserva;

    // Recepción las ve para cancelarlas cuando el paciente lo pide, y el administrador también puede verlas.
    // El paciente ve las suyas en /consultas/me, y el médico, su agenda en /consultas/medico.
    @ParameterizedTest(name = "{0} recibe {1}")
    @CsvSource({"RECEPCIONISTA, 200", "ADMINISTRADOR, 200", "PACIENTE, 403", "MEDICO, 403"})
    @DisplayName("Las consultas de un paciente son de recepción y del administrador, no del paciente ni del médico")
    void consultasDeUnPaciente(Rol rol, int estadoEsperado) throws Exception {
        when(reserva.listarConsultasDeUnPaciente(7L)).thenReturn(List.of());

        mvc.perform(get("/consultas/paciente/7").header("Authorization", tokenDe(rol)))
                .andExpect(status().is(estadoEsperado));
    }

    @Test
    @DisplayName("Recepción recibe las consultas que da ReservaDeConsultas, con su médico")
    void recepcionRecibeLasConsultas() throws Exception {
        when(reserva.listarConsultasDeUnPaciente(7L)).thenReturn(List.of(new DatosDetalleConsulta(
                10L, 3L, "Ana Torres", Especialidad.CARDIOLOGIA, 7L, LocalDateTime.of(2026, 12, 1, 10, 0),
                EstadoConsulta.PROGRAMADA)));

        mvc.perform(get("/consultas/paciente/7").header("Authorization", tokenDe(Rol.RECEPCIONISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].medicoNombre").value("Ana Torres"));
    }

    @Test
    @DisplayName("Recepción cancela una consulta con el motivo en el cuerpo del DELETE")
    void recepcionCancelaConMotivo() throws Exception {
        mvc.perform(delete("/consultas/10")
                        .header("Authorization", tokenDe(Rol.RECEPCIONISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"PACIENTE_DESISTIO\"}"))
                .andExpect(status().isNoContent());

        verify(reserva).cancelar(eq(10L), eq(MotivoCancelamiento.PACIENTE_DESISTIO),
                argThat(usuario -> usuario.getRol() == Rol.RECEPCIONISTA));
    }

    @Test
    @DisplayName("Sin motivo no se cancela: responde 400 y no llega a ReservaDeConsultas")
    void sinMotivoNoSeCancela() throws Exception {
        mvc.perform(delete("/consultas/10")
                        .header("Authorization", tokenDe(Rol.RECEPCIONISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reserva);
    }

    // Un usuario con ese rol, que el filtro de seguridad encuentra por su correo, y la cabecera con su token.
    private String tokenDe(Rol rol) {
        var usuario = new Usuario(CORREO, "hash", rol);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(usuario);
        return "Bearer " + tokenService.generateToken(usuario);
    }
}
