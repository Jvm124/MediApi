package com.voll.api.domain.consulta;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DatosDetalleConsulta(
        Long id,
        Long idMedico,
        Long idPaciente,
        LocalDateTime fecha

) {
    public DatosDetalleConsulta(Consulta consulta) {
        this(consulta.getId(), consulta.getIdMedico().getId(), consulta.getIdPaciente().getId(), consulta.getFecha());
    }
}
