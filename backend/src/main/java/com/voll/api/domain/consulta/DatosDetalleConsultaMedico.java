package com.voll.api.domain.consulta;

import java.time.LocalDateTime;

public record DatosDetalleConsultaMedico(
        Long id,
        Long idPaciente,
        String pacienteNombre,
        Long idMedico,
        LocalDateTime fecha,
        EstadoConsulta estado
) {
    public DatosDetalleConsultaMedico(Consulta consulta) {
        this(
                consulta.getId(),
                consulta.getIdPaciente().getId(),
                consulta.getIdPaciente().getNombre(),
                consulta.getIdMedico().getId(),
                consulta.getFecha(),
                consulta.getEstado()
        );
    }
}
