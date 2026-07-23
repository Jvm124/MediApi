package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.consulta.DatosReservaConsulta;
import org.springframework.stereotype.Component;


public interface ValidadorDeConsultas {
    void validar(DatosReservaConsulta datos);
}
