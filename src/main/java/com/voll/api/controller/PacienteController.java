package com.voll.api.controller;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.paciente.*;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/pacientes")
@SecurityRequirement(name = "bearer-key")
public class PacienteController {
    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Transactional
    @PostMapping
    public ResponseEntity registrarPaciente(@RequestBody @Valid DatosRegistroPaciente datos, UriComponentsBuilder uriComponentsBuilder) {

        if (usuarioRepository.existsByCorreo(datos.email())) {
            throw new ValidacionException("Ya existe un usuario con ese correo de acceso");
        }
        var paciente= new Paciente(datos);
        pacienteRepository.save(paciente);

        var contraseniaEncriptada = passwordEncoder.encode(datos.contrasenia());
        var usuario = new Usuario(datos.email(), contraseniaEncriptada, Rol.PACIENTE);
        usuario.asignarPaciente(paciente);
        usuarioRepository.save(usuario);

        var uri = uriComponentsBuilder.path("/pacientes/{id}").buildAndExpand(paciente.getId()).toUri();
        return ResponseEntity.created(uri).body(new DatosDetallePaciente(paciente));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<Page<DatosListaPaciente>> listarPacientes(@PageableDefault(size=10, sort={"nombre"}) Pageable paginacion) {
        var page = pacienteRepository.findAllByActivoTrue(paginacion)
                .map(DatosListaPaciente::new);
        return ResponseEntity.ok(page);
    }
    @Transactional
    @PutMapping
    @PreAuthorize("hasAnyRole('PACIENTE')")
    public ResponseEntity actualizarPaciente(@RequestBody @Valid DatosActualizarPaciente datos) {
        var paciente = pacienteRepository.getReferenceById(datos.id());
        paciente.actualizarInformacionesPaciente(datos);
        return ResponseEntity.ok(new DatosDetallePaciente(paciente));
    }

    @Transactional
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','PACIENTE')")
    public ResponseEntity eliminarPaciente(@PathVariable Long id) {
        var paciente = pacienteRepository.getReferenceById(id);
        paciente.eliminacionLogicaPaciente();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    public ResponseEntity listarPacientesPorId(@PathVariable Long id) {
        var paciente =pacienteRepository.getReferenceById(id);
        return ResponseEntity.ok(new DatosDetallePaciente(paciente));
    }
}
