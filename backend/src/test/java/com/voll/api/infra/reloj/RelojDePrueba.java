package com.voll.api.infra.reloj;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Un reloj para pruebas que solo avanza cuando la prueba lo pide, con avanzar(...). Sirve para probar reglas que
 * dependen del paso del tiempo, como el bloqueo de 15 minutos del login (SEC-04), sin esperar de verdad.
 * Clock.fixed(...) no sirve para eso, porque no se puede mover.
 */
public class RelojDePrueba extends Clock {

    private volatile Instant ahora;
    private final ZoneId zona;

    public RelojDePrueba(Instant inicio) {
        this(inicio, ConfiguracionDelReloj.ZONA_DE_LA_CLINICA);
    }

    private RelojDePrueba(Instant inicio, ZoneId zona) {
        this.ahora = inicio;
        this.zona = zona;
    }

    public void avanzar(Duration tiempo) {
        ahora = ahora.plus(tiempo);
    }

    @Override
    public Instant instant() {
        return ahora;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    // Devuelve una copia con otra zona, que ya no avanza junto con este reloj.
    @Override
    public Clock withZone(ZoneId otraZona) {
        return new RelojDePrueba(ahora, otraZona);
    }
}
