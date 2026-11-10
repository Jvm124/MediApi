package com.voll.api.controller;

import com.voll.api.domain.usuario.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<DatosDetalleUsuario> registrar(@RequestBody @Valid DatosRegistroUsuario datos) {
        var usuario = usuarioService.registrarUsuarios(datos);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<DatosListaUsuario>> listarUsuarios(@PageableDefault(size = 10, sort = {"correo"}) Pageable paginacion) {
        return ResponseEntity.ok(usuarioService.listarUsuarios(paginacion));
    }
    @PatchMapping("/{id}/suspender")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> suspender(@PathVariable Long id, @AuthenticationPrincipal Usuario adminLogueado) {
        usuarioService.suspender(id, adminLogueado);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> reactivar(@PathVariable Long id, @AuthenticationPrincipal Usuario adminLogueado) {
        usuarioService.reactivar(id, adminLogueado);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{id}/baja")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id, @AuthenticationPrincipal Usuario adminLogueado) {
        usuarioService.darDeBaja(id, adminLogueado);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/me")
    public ResponseEntity<DatosUsuarioActual> datosUsuarioActual(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(DatosUsuarioActual.desde(usuario));
    }
}
