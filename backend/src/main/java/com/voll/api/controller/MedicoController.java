package com.voll.api.controller;

import com.voll.api.domain.medico.*;
import com.voll.api.domain.usuario.Usuario;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/medicos")
@SecurityRequirement(name = "bearer-key")
public class MedicoController {

    @Autowired
    private MedicoService medicoService;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity registrar(@RequestBody @Valid DatosRegistroMedico datos, UriComponentsBuilder uriComponentsBuilder) {
        var detalle = medicoService.registrar(datos);
        var uri = uriComponentsBuilder.path("/medicos/{id}").buildAndExpand(detalle.id()).toUri();
        return ResponseEntity.created(uri).body(detalle);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<DatosListaMedico>> listar(Pageable paginacion) {
        var lista = medicoService.listar(paginacion);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<DatosDetalleMedico> miPerfil(@AuthenticationPrincipal Usuario usuarioLogueado) {
        return ResponseEntity.ok(medicoService.miPerfil(usuarioLogueado));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<DatosDetalleMedico> actualizarPerfil(
            @RequestBody @Valid DatosActualizarPerfilMedico datos,
            @AuthenticationPrincipal Usuario usuarioLogueado) {
        return ResponseEntity.ok(medicoService.actualizarPerfil(datos, usuarioLogueado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity actualizar(@PathVariable Long id, @RequestBody @Valid DatosActualizarMedico datos) {
        var medico = medicoService.actualizar(id, datos);
        return ResponseEntity.ok(medico);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity eliminarMedico(@PathVariable Long id) {
        medicoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity detallar(@PathVariable Long id) {
        var medico = medicoService.buscarPorId(id);
        return ResponseEntity.ok(medico);
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<DatosMedicoSelect>> listarParaSelect() {
        return ResponseEntity.ok(medicoService.listarDisponibles());
    }
}
