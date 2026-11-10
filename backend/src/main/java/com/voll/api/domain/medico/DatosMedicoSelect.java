package com.voll.api.domain.medico;

public record DatosMedicoSelect(
        Long id,
        String nombre,
        Especialidad especialidad
) {
    public DatosMedicoSelect(Medico medico) {
        this(medico.getId(), medico.getNombre(), medico.getEspecialidad());
    }
}