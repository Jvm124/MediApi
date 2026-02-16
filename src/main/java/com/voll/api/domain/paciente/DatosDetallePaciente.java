package com.voll.api.domain.paciente;

import com.voll.api.domain.direccion.Direccion;

public record DatosDetallePaciente(
        Long id,
        String nombre,
        String email,
        String telefono,
        String documento,
        Direccion direccion
) {
    public DatosDetallePaciente(Paciente paciente){
        this(
                paciente.getId(),
                paciente.getNombre(),
                paciente.getEmail(),
                paciente.getTelefono(),
                paciente.getDocumento(),
                paciente.getDireccion()
        );
    }
}
