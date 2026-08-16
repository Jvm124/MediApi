package com.voll.api.domain.medico;

import com.voll.api.domain.direccion.DatosDireccion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

public record DatosActualizarPerfilMedico(
        @Pattern(regexp = "9\\d{8}") String telefono,
        @Valid DatosDireccion direccion
) {
}
