package com.voll.api.domain.consulta.validaciones;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.DatosReservaConsulta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ValidadorConsultaConAnticipacion implements ValidadorDeConsultas {

    // La hora de la clínica (ConfiguracionDelReloj); en las pruebas, una hora fija.
    private final Clock reloj;

    public void validar(DatosReservaConsulta datos){
        var fechaConsulta = datos.fecha();
        var ahora = LocalDateTime.now(reloj);
        var diferenciaEnMinutos = Duration.between(ahora,fechaConsulta).toMinutes();
        if (diferenciaEnMinutos < 30) {
            throw new ValidacionException("Horario seleccionado con menos de 30 minutos de anticipacion");
        }
    }
}
