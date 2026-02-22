package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.consulta.DatosReservaConsulta;
import org.springframework.stereotype.Component;

@Component
public interface ValidadorDeConsultas {
    void validar(DatosReservaConsulta datos);
}
