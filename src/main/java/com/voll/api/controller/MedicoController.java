package com.voll.api.controller;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.medico.*;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/medicos")
@SecurityRequirement(name = "bearer-key")
public class MedicoController {

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")   // solo el admin registra médicos
    public ResponseEntity registrar(@RequestBody @Valid DatosRegistroMedico datos,
                                    UriComponentsBuilder uriComponentsBuilder) {

        // 1. Evitar correos de acceso duplicados en la tabla usuarios
        if (usuarioRepository.existsByCorreo(datos.email())) {
            throw new ValidacionException("Ya existe un usuario con ese correo de acceso");
        }

        // 2. Crear y guardar el médico (datos profesionales)
        var medico = new Medico(datos);
        medicoRepository.save(medico);

        // 3. Crear el usuario de login enlazado al médico, con rol MEDICO
        var contraseniaEncriptada = passwordEncoder.encode(datos.contrasenia());
        var usuario = new Usuario(datos.email(), contraseniaEncriptada, Rol.MEDICO);
        usuario.asignarMedico(medico);
        usuarioRepository.save(usuario);

        var uri = uriComponentsBuilder.path("/medicos/{id}").buildAndExpand(medico.getId()).toUri();
        return ResponseEntity.created(uri).body(new DatosDetalleMedico(medico));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<DatosListaMedico>> listar(Pageable paginacion){
        var page = medicoRepository.findAllByActivoTrue(paginacion)
                .map(DatosListaMedico::new);
        return ResponseEntity.ok(page);
    }

    @Transactional
    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")   // por ahora, solo admin edita
    public ResponseEntity actualizar(@RequestBody @Valid DatosActualizarMedico datos){
        var medico = medicoRepository.getReferenceById(datos.id());
        medico.actualizarInformaciones(datos);
        return ResponseEntity.ok(new DatosDetalleMedico(medico));
    }

    @Transactional
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")   // solo admin elimina
    public ResponseEntity eliminar(@PathVariable Long id){
        var medico = medicoRepository.getReferenceById(id);
        medico.eliminacionLogica();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    public ResponseEntity detallar(@PathVariable Long id){
        var medico = medicoRepository.getReferenceById(id);
        return ResponseEntity.ok(new DatosListaMedico(medico));
    }
}