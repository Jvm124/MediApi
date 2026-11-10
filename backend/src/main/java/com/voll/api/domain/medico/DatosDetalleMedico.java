package com.voll.api.domain.medico;

import com.voll.api.domain.direccion.Direccion;

public record DatosDetalleMedico(
        Long id,
        String nombre,
        String email,
        String telefono,
        String documento,
        Especialidad especialidad,
        Direccion direccion
) {
    public DatosDetalleMedico(Medico medico){
        this(
                medico.getId(),
                medico.getNombre(),
                medico.getEmail(),
                medico.getTelefono(),
                medico.getDocumento(),
                medico.getEspecialidad(),
                medico.getDireccion()
        );
    }
}
