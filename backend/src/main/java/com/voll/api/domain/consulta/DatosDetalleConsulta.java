package com.voll.api.domain.consulta;
import com.voll.api.domain.medico.Especialidad;

import java.time.LocalDateTime;

public record DatosDetalleConsulta(
        Long id,
        Long idMedico,
        String medicoNombre,
        Especialidad especialidad,
        Long idPaciente,
        LocalDateTime fecha,
        EstadoConsulta estado
) {
    public DatosDetalleConsulta(Consulta consulta) {
        this(
                consulta.getId(),
                consulta.getIdMedico().getId(),
                consulta.getIdMedico().getNombre(),
                consulta.getIdMedico().getEspecialidad(),
                consulta.getIdPaciente().getId(),
                consulta.getFecha(),
                consulta.getEstado()
        );
    }
}