package com.voll.api.domain.consulta;

import com.voll.api.domain.medico.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    boolean existsByIdMedicoIdAndFecha(Long idMedico, LocalDateTime fecha);

    @Query("""
            SELECT COUNT(c) > 0 FROM Consulta c
            WHERE c.idPaciente.id = :idPaciente
              AND c.idMedico.especialidad = :especialidad
              AND c.estado = :estado
              AND c.fecha BETWEEN :inicioDia AND :finDia
            """)
    boolean pacienteTieneConsultaDeEspecialidadEnElDia(
            @Param("idPaciente") Long idPaciente,
            @Param("especialidad") Especialidad especialidad,
            @Param("estado") EstadoConsulta estado,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);

    @Query("""
            SELECT c FROM Consulta c
            JOIN FETCH c.idMedico
            WHERE c.idPaciente.id = :idPaciente
              AND c.estado = :estado
            ORDER BY c.fecha ASC
            """)
    List<Consulta> buscarProgramadasDelPaciente(@Param("idPaciente") Long idPaciente,
                                                @Param("estado") EstadoConsulta estado);

    @Query("""
            SELECT c FROM Consulta c
            JOIN FETCH c.idPaciente
            WHERE c.idMedico.id = :idMedico
              AND c.estado = :estado
            ORDER BY c.fecha ASC
            """)
    List<Consulta> buscarProgramadasDelMedico(@Param("idMedico") Long idMedico,
                                              @Param("estado") EstadoConsulta estado);
}
