package com.voll.api.controller;

import com.voll.api.domain.consulta.*;
import com.voll.api.domain.usuario.Usuario;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/consultas")
@SecurityRequirement(name = "bearer-key")
@RequiredArgsConstructor
public class ConsultaController {

    private final ReservaDeConsultas reserva;

    @PostMapping
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA')")
    public ResponseEntity<DatosDetalleConsulta> reservar(
            @RequestBody @Valid DatosReservaConsulta datos,
            @AuthenticationPrincipal Usuario usuarioLogueado) {
        var detalleConsulta = reserva.reservar(datos, usuarioLogueado);
        return ResponseEntity.ok(detalleConsulta);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<DatosDetalleConsulta>> listarMisConsultas(
            @AuthenticationPrincipal Usuario usuarioLogueado) {
        return ResponseEntity.ok(reserva.listarConsultasDelPaciente(usuarioLogueado));
    }

    // Las consultas programadas de un paciente, para verlas y cancelarlas desde recepción (BUG-04). El paciente ve
    // las suyas en /me.
    @GetMapping("/paciente/{idPaciente}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMINISTRADOR')")
    public ResponseEntity<List<DatosDetalleConsulta>> listarConsultasDeUnPaciente(@PathVariable Long idPaciente) {
        return ResponseEntity.ok(reserva.listarConsultasDeUnPaciente(idPaciente));
    }

    // Agenda del médico: sus consultas programadas (ve al PACIENTE, no a sí mismo).
    @GetMapping("/medico")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<List<DatosDetalleConsultaMedico>> listarConsultasDelMedico(
            @AuthenticationPrincipal Usuario usuarioLogueado) {
        return ResponseEntity.ok(reserva.listarConsultasDelMedico(usuarioLogueado));
    }

    // El médico da por atendida su consulta, desde que llega su hora: PROGRAMADA -> ATENDIDA.
    @PostMapping("/{id}/atender")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<DatosDetalleConsulta> atender(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogueado) {
        return ResponseEntity.ok(reserva.atender(id, usuarioLogueado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'RECEPCIONISTA', 'ADMINISTRADOR')")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            @RequestBody @Valid DatosCancelamientoConsulta datos,
            @AuthenticationPrincipal Usuario usuarioLogueado) {
        reserva.cancelar(id, datos.motivo(), usuarioLogueado);
        return ResponseEntity.noContent().build();
    }
}
