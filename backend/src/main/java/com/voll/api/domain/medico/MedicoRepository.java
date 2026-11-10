package com.voll.api.domain.medico;

import com.voll.api.domain.consulta.EstadoConsulta;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicoRepository extends JpaRepository<Medico, Long> {

    Page<Medico> findAllByActivoTrue(Pageable paginacion);

    // Un médico está libre si no tiene una consulta con ese estado a esa hora. Al reservar se pasa
    // PROGRAMADA, porque una consulta cancelada ya no ocupa el horario (BUG-01).
    @Query(" SELECT m from Medico m  where m.activo =true and m.especialidad = :especialidad and m.id not in(select c.idMedico.id from Consulta c where c.fecha = :fecha and c.estado = :estado) order by rand() limit 1 ")
    Medico elegirMedicoAleatorioDisponibleEnLaFecha(Especialidad especialidad, @NotNull @Future LocalDateTime fecha, EstadoConsulta estado);

    @Query("select m.activo from Medico m where m.id = :idMedico")
    boolean findActivoById(Long idMedico);

    boolean existsByDocumento(String documento);
    List<Medico> findByActivoTrue();
}
