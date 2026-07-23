package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import com.voll.api.domain.medico.MedicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValidadorMedicoActivo implements ValidadorDeConsultas {
    @Autowired
    private MedicoRepository medicoRepository;
    public void validar(DatosReservaConsulta datos){
        //eleccion del medico opcional
        if(datos.idMedico()==null){
            return;
        }

        var medicoEstadoActivo = medicoRepository.findActivoById(datos.idMedico());
        if (!medicoEstadoActivo){
            throw new ValidacionException("Consulta no puede ser reservada con medico inactivo");
        }
    }
}
