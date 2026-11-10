package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.ConsultaRepository;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import com.voll.api.domain.consulta.EstadoConsulta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidadorMedicoConOtraConsultaEnElMismoHorario implements ValidadorDeConsultas {
    private final ConsultaRepository consultaRepository;
    public void validar(DatosReservaConsulta datos){
        var medicoTieneOtraConsultaEnElMismoHorario = consultaRepository.existsByIdMedicoIdAndFechaAndEstado(
                datos.idMedico(), datos.fecha(), EstadoConsulta.PROGRAMADA);
        if (medicoTieneOtraConsultaEnElMismoHorario){
            throw new ValidacionException("Medico ya tiene otra consulta en esa misma fecha y hora");
        }
    }
}
