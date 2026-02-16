package com.voll.api.domain.paciente;

public record DatosListaPaciente (
        Long id,
        String nombre,
        String email,
        String telefono
){
    public DatosListaPaciente (Paciente paciente) {
            this(paciente.getId(),paciente.getNombre(),paciente.getEmail(),paciente.getTelefono());
    }


}
