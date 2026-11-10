package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import com.voll.api.domain.paciente.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidadorPacienteActivo implements ValidadorDeConsultas{
    private final PacienteRepository pacienteRepository;
    public void validar(DatosReservaConsulta datos){
        var pacienteEstadoActivo = pacienteRepository.findActivoById(datos.idPaciente());
        if (!pacienteEstadoActivo){
            throw new ValidacionException("Consulta no puede ser reservada con paciente excluido");
        }
    }
}
