package com.voll.api.infra.security;

import java.time.Duration;

/**
 * Hubo demasiados intentos fallidos de login con ese correo o desde esa IP (LimiteDeIntentosDeLogin, SEC-04).
 * TratadorDeErrores responde 429 Too Many Requests, con este mensaje y la cabecera Retry-After.
 */
public class DemasiadosIntentosException extends RuntimeException {

    private final long segundosDeEspera;

    public DemasiadosIntentosException(Duration espera) {
        this(segundosRedondeadosHaciaArriba(espera));
    }

    private DemasiadosIntentosException(long segundosDeEspera) {
        super(mensaje(segundosDeEspera));
        this.segundosDeEspera = segundosDeEspera;
    }

    /**
     * Los segundos que faltan para poder volver a intentarlo. Van en la cabecera Retry-After.
     */
    public long getSegundosDeEspera() {
        return segundosDeEspera;
    }

    // Se redondea hacia arriba: si faltan 14 minutos y medio, decir «14 minutos» haría volver antes de tiempo.
    private static long segundosRedondeadosHaciaArriba(Duration espera) {
        return espera.toSeconds() + (espera.toNanosPart() > 0 ? 1 : 0);
    }

    private static String mensaje(long segundos) {
        var minutos = Math.max(1, (segundos + 59) / 60);
        return "Demasiados intentos fallidos. Vuelve a intentarlo en " + minutos
                + (minutos == 1 ? " minuto." : " minutos.");
    }
}
