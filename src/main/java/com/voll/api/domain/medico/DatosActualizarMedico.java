package com.voll.api.domain.medico;

import com.voll.api.domain.direccion.DatosDireccion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

public record DatosActualizarMedico(
        String nombre,
        @Pattern(regexp = "9\\d{8}") String telefono,
        Especialidad especialidad,
        @Valid DatosDireccion direccion
) {
}
