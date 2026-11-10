package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.ConsultaRepository;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import com.voll.api.domain.consulta.EstadoConsulta;
import com.voll.api.domain.medico.Especialidad;
import com.voll.api.domain.medico.Medico;
import com.voll.api.domain.medico.MedicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class ValidadorPacienteSinOtraConsultaDeLaEspecialidadEnElDia implements ValidadorDeConsultas {

    @Autowired
    private ConsultaRepository consultaRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    public void validar(DatosReservaConsulta datos) {
        var especialidad = resolverEspecialidad(datos);
        if (especialidad == null) {
            return;
        }

        var inicioDia = datos.fecha().toLocalDate().atStartOfDay();
        var finDia = datos.fecha().toLocalDate().atTime(LocalTime.MAX);

        var yaTiene = consultaRepository.pacienteTieneConsultaDeEspecialidadEnElDia(
                datos.idPaciente(), especialidad, EstadoConsulta.PROGRAMADA, inicioDia, finDia);

        if (yaTiene) {
            throw new ValidacionException(
                    "El paciente ya tiene una consulta de esa especialidad reservada para ese día");
        }
    }

    private Especialidad resolverEspecialidad(DatosReservaConsulta datos) {
        if (datos.idMedico() != null) {
            return medicoRepository.findById(datos.idMedico())
                    .map(Medico::getEspecialidad)
                    .orElse(null);
        }
        return datos.especialidad();
    }
}
