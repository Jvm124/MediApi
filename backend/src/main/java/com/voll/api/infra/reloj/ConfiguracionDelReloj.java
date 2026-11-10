package com.voll.api.infra.reloj;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Un solo reloj para toda la aplicación, con la hora de la clínica (SEC-07 en
 * docs/revisiones/0001-revision-inicial.md).
 * <p>
 * Las fechas de las consultas son horas de Lima. LocalDateTime.now() usa la zona horaria del servidor, y en un
 * servidor con hora UTC (como el CI de GitHub o muchos de la nube) "ahora" iría 5 horas adelantado. Por eso las
 * reglas reciben este reloj y preguntan la hora con LocalDateTime.now(reloj). En las pruebas se usa
 * Clock.fixed(...) para fijar la hora.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionDelReloj {

    public static final ZoneId ZONA_DE_LA_CLINICA = ZoneId.of("America/Lima");

    @Bean
    public Clock reloj() {
        return Clock.system(ZONA_DE_LA_CLINICA);
    }

    /**
     * Las validaciones de los datos que llegan a la API, como @Future en DatosReservaConsulta, también usan este
     * reloj. Sin esto, Bean Validation compararía con la hora del servidor.
     */
    @Bean
    public ValidationConfigurationCustomizer validacionesConElReloj(Clock reloj) {
        return configuracion -> configuracion.clockProvider(() -> reloj);
    }
}
