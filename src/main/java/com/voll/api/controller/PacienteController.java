package com.voll.api.controller;

import com.voll.api.domain.paciente.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/pacientes")
@SecurityRequirement(name = "bearer-key")
public class PacienteController {
    @Autowired
    private PacienteRepository pacienteRepository;

    @Transactional
    @PostMapping
    public ResponseEntity registrarPaciente(@RequestBody @Valid DatosRegistroPaciente datos, UriComponentsBuilder uriComponentsBuilder) {
        var paciente= new Paciente(datos);
        pacienteRepository.save(paciente);
        var uri = uriComponentsBuilder.path("/pacientes/{id}").buildAndExpand(paciente.getId()).toUri();
        return ResponseEntity.created(uri).body(new DatosDetallePaciente(paciente));
    }

    @GetMapping
    public ResponseEntity<Page<DatosListaPaciente>> listarPacientes(@PageableDefault(size=10, sort={"nombre"}) Pageable paginacion) {
        var page = pacienteRepository.findAllByActivoTrue(paginacion)
                .map(DatosListaPaciente::new);
        return ResponseEntity.ok(page);
    }
    @Transactional
    @PutMapping
    public ResponseEntity actualizarPaciente(@RequestBody @Valid DatosActualizarPaciente datos) {
        var paciente = pacienteRepository.getReferenceById(datos.id());
        paciente.actualizarInformacionesPaciente(datos);
        return ResponseEntity.ok(new DatosDetallePaciente(paciente));
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity eliminarPaciente(@PathVariable Long id) {
        var paciente = pacienteRepository.getReferenceById(id);
        paciente.eliminacionLogicaPaciente();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity listarPacientesPorId(@PathVariable Long id) {
        var paciente =pacienteRepository.getReferenceById(id);
        return ResponseEntity.ok(new DatosDetallePaciente(paciente));
    }
}
