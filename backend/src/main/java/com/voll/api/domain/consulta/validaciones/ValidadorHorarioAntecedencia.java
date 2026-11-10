package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.Consulta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ValidadorHorarioAntecedencia implements ValidadorCancelamientoDeConsulta {

    // La hora de la clínica (ConfiguracionDelReloj); en las pruebas, una hora fija.
    private final Clock reloj;

    @Override
    public void validar(Consulta consulta) {
        var diferenciaEnHoras = Duration.between(LocalDateTime.now(reloj), consulta.getFecha()).toHours();

        if (diferenciaEnHoras < 24) {
            throw new ValidacionException("La consulta solo se puede cancelar con al menos 24 horas de anticipación.");
        }
    }
}
