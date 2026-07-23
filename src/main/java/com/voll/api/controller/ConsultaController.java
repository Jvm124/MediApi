package com.voll.api.controller;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.*;
import com.voll.api.domain.usuario.Usuario;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consultas")
@SecurityRequirement(name = "bearer-key")
public class ConsultaController {

    @Autowired
    private ReservaDeConsultas reserva;
    @Autowired
    private ConsultaRepository consultaRepository;

    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA')")
    public ResponseEntity reservar(@RequestBody @Valid DatosReservaConsulta datos, Usuario usuarioLogueado) {

        var detalleConsulta = reserva.reservar(datos,usuarioLogueado);

        return ResponseEntity.ok(detalleConsulta);

    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'RECEPCIONISTA', 'ADMINISTRADOR')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, @RequestBody @Valid DatosCancelamientoConsulta datos) {
        reserva.cancelar(id, datos.motivo());
        return ResponseEntity.noContent().build();
    }


}
