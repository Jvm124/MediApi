package com.voll.api.domain.consulta;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.medico.Medico;
import com.voll.api.domain.paciente.Paciente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "consultas")
@Entity(name = "Consulta")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Consulta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="medico_id")
    private Medico idMedico;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="paciente_id")
    private Paciente idPaciente;
    private LocalDateTime fecha;
    @Enumerated(EnumType.STRING)
    private MotivoCancelamiento motivo;


    @Enumerated(EnumType.STRING)
    private EstadoConsulta estado;

    public Consulta(Medico medico, Paciente paciente, LocalDateTime fecha) {
        this.idMedico = medico;
        this.idPaciente = paciente;
        this.fecha = fecha;
        this.estado = EstadoConsulta.PROGRAMADA;
        this.motivo = null;
    }


    public void cancelar (MotivoCancelamiento motivo) {
        if(this.estado == EstadoConsulta.CANCELADA) {
            throw new ValidacionException("La consulta ya fue cancelada");
        }
        this.estado = EstadoConsulta.CANCELADA;
        this.motivo = motivo;
    }
}
