package com.voll.api.controller;

import com.voll.api.domain.paciente.*;
import com.voll.api.domain.usuario.Usuario;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/pacientes")
@SecurityRequirement(name = "bearer-key")
public class PacienteController {

    @Autowired
    private PacienteService pacienteService;

    // Auto-registro público (crea paciente + cuenta). Debe estar permitido sin token
    // en tu SecurityFilterChain.
    @PostMapping
    public ResponseEntity<DatosDetallePaciente> registrarPaciente(@RequestBody @Valid DatosRegistroPaciente datos, UriComponentsBuilder uriBuilder) {
        var detalle = pacienteService.registrar(datos);
        var uri = uriBuilder.path("/pacientes/{id}").buildAndExpand(detalle.id()).toUri();
        return ResponseEntity.created(uri).body(detalle);
    }

    // Alta por mostrador (staff): crea el paciente SIN cuenta web.
    @PostMapping("/registro-staff")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<DatosDetallePaciente> registrarPacientePorStaff(@RequestBody @Valid DatosRegistroPacientePorStaff datos, UriComponentsBuilder uriBuilder) {
        var detalle = pacienteService.registrarPorStaff(datos);
        var uri = uriBuilder.path("/pacientes/{id}").buildAndExpand(detalle.id()).toUri();
        return ResponseEntity.created(uri).body(detalle);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<Page<DatosListaPaciente>> listarPacientes(@PageableDefault(size = 10, sort = {"nombre"}) Pageable paginacion) {
        return ResponseEntity.ok(pacienteService.listar(paginacion));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<DatosDetallePaciente> detallarPaciente(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.detallar(id));
    }

    @PutMapping
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<DatosDetallePaciente> actualizarPaciente(@RequestBody @Valid DatosActualizarPaciente datos, @AuthenticationPrincipal Usuario usuarioLogueado) {
        return ResponseEntity.ok(pacienteService.actualizar(datos, usuarioLogueado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PACIENTE')")
    public ResponseEntity<Void> eliminarPaciente(@PathVariable Long id, @AuthenticationPrincipal Usuario usuarioLogueado) {
        pacienteService.eliminar(id, usuarioLogueado);
        return ResponseEntity.noContent().build();
    }
}
