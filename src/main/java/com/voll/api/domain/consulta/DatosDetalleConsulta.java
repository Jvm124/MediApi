package com.voll.api.domain.consulta;
import java.time.LocalDateTime;

public record DatosDetalleConsulta(
        Long id,
        Long idMedico,
        Long idPaciente,
        LocalDateTime fecha,
        EstadoConsulta estado
) {
    public DatosDetalleConsulta(Consulta consulta) {
        this(consulta.getId(), consulta.getIdMedico().getId(),
                consulta.getIdPaciente().getId(), consulta.getFecha(),
                consulta.getEstado());
    }
}
