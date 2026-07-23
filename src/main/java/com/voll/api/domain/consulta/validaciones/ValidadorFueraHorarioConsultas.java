package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
@Component
public class ValidadorFueraHorarioConsultas implements ValidadorDeConsultas{

    public void validar(DatosReservaConsulta datos){
        var fechaConsulta = datos.fecha();
        var domingo = fechaConsulta.getDayOfWeek().equals(DayOfWeek.SUNDAY);
        var horarioAntesDeAperturaClinica = fechaConsulta.getHour()< 7;
        var horarioAntesDeCierreClinica = fechaConsulta.getHour()> 18;

        if(domingo || horarioAntesDeAperturaClinica || horarioAntesDeCierreClinica){
            throw  new ValidacionException("Horario seleccionado fuera del horario de atendimiento de la clinica");
        }
    }
}
