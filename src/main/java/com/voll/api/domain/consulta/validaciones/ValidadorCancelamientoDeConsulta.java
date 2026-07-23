package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.consulta.Consulta;
import org.springframework.stereotype.Component;

@Component
public interface ValidadorCancelamientoDeConsulta {
    void validar(Consulta consulta);
}
