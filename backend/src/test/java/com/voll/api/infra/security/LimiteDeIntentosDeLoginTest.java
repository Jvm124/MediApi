package com.voll.api.infra.security;

import com.voll.api.infra.reloj.RelojDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias del límite de intentos de login (SEC-04), sin Spring. RelojDePrueba adelanta la hora, así que
 * los 15 minutos de bloqueo pasan sin esperar.
 */
class LimiteDeIntentosDeLoginTest {

    private static final String CORREO = "ana@ejemplo.com";
    private static final String IP = "10.0.0.1";
    private static final String OTRA_IP = "10.0.0.2";

    private final RelojDePrueba reloj = new RelojDePrueba(Instant.parse("2026-11-10T15:00:00Z"));
    private final LimiteDeIntentosDeLogin limite = new LimiteDeIntentosDeLogin(reloj);

    private void fallar(int veces, String correo, String ip) {
        for (int i = 0; i < veces; i++) {
            limite.registrarFallo(correo, ip);
        }
    }

    // Fallos desde una IP, cada uno con un correo distinto, para no llegar al límite de ningún correo.
    private void fallarConCorreosDistintos(int veces, String ip) {
        for (int i = 1; i <= veces; i++) {
            limite.registrarFallo("persona" + i + "@ejemplo.com", ip);
        }
    }

    private boolean bloqueado(String correo, String ip) {
        try {
            limite.comprobar(correo, ip);
            return false;
        } catch (DemasiadosIntentosException e) {
            return true;
        }
    }

    private void comprobarEspera(long segundos, String mensaje) {
        assertThatThrownBy(() -> limite.comprobar(CORREO, IP))
                .isInstanceOfSatisfying(DemasiadosIntentosException.class, e -> {
                    assertThat(e.getSegundosDeEspera()).isEqualTo(segundos);
                    assertThat(e).hasMessage(mensaje);
                });
    }

    @Test
    @DisplayName("Con 4 fallos todavía se puede intentar")
    void cuatroFallosNoBloquean() {
        fallar(4, CORREO, IP);

        assertThat(bloqueado(CORREO, IP)).isFalse();
    }

    @Test
    @DisplayName("El quinto fallo bloquea el correo, también desde otra IP")
    void elQuintoFalloBloqueaElCorreo() {
        fallar(5, CORREO, IP);

        assertThat(bloqueado(CORREO, IP)).isTrue();
        assertThat(bloqueado(CORREO, OTRA_IP)).as("el mismo correo desde otra IP").isTrue();
        assertThat(bloqueado("otro@ejemplo.com", IP)).as("otro correo desde la misma IP").isFalse();
    }

    @Test
    @DisplayName("El bloqueo termina a los 15 minutos y el contador vuelve a cero")
    void elBloqueoTerminaALosQuinceMinutos() {
        fallar(5, CORREO, IP);

        reloj.avanzar(Duration.ofMinutes(15).minusSeconds(1));
        assertThat(bloqueado(CORREO, IP)).as("un segundo antes").isTrue();

        reloj.avanzar(Duration.ofSeconds(1));
        assertThat(bloqueado(CORREO, IP)).as("a los 15 minutos").isFalse();

        // Si el contador siguiera en 5, cualquier fallo más volvería a bloquear.
        fallar(4, CORREO, IP);
        assertThat(bloqueado(CORREO, IP)).as("con 4 fallos nuevos").isFalse();
    }

    @Test
    @DisplayName("Un login correcto pone a cero el contador del correo")
    void unAciertoReiniciaElCorreo() {
        fallar(4, CORREO, IP);
        limite.registrarAcierto(CORREO);
        fallar(4, CORREO, IP);

        assertThat(bloqueado(CORREO, IP)).isFalse();
    }

    @Test
    @DisplayName("Los fallos cuentan durante 15 minutos desde el primero")
    void losFallosCuentanQuinceMinutos() {
        fallar(4, CORREO, IP);
        reloj.avanzar(Duration.ofMinutes(14));
        fallar(1, CORREO, IP);

        assertThat(bloqueado(CORREO, IP)).isTrue();
    }

    @Test
    @DisplayName("Los fallos de hace 15 minutos o más ya no cuentan")
    void losFallosViejosNoCuentan() {
        fallar(4, CORREO, IP);
        reloj.avanzar(Duration.ofMinutes(15));
        fallar(4, CORREO, IP);

        assertThat(bloqueado(CORREO, IP)).isFalse();
    }

    @Test
    @DisplayName("Mayúsculas y espacios alrededor cuentan como el mismo correo")
    void mayusculasYEspacios() {
        fallar(2, "Ana@Ejemplo.com", IP);
        fallar(2, " ana@ejemplo.com ", IP);
        fallar(1, "ANA@EJEMPLO.COM", IP);

        assertThat(bloqueado(CORREO, IP)).isTrue();
    }

    @Test
    @DisplayName("50 fallos desde una IP la bloquean, con cualquier correo")
    void laIpSeBloqueaConCincuentaFallos() {
        fallarConCorreosDistintos(49, IP);
        assertThat(bloqueado("nueva@ejemplo.com", IP)).as("con 49 fallos").isFalse();

        limite.registrarFallo("persona50@ejemplo.com", IP);

        assertThat(bloqueado("nueva@ejemplo.com", IP)).as("cualquier correo desde esa IP").isTrue();
        assertThat(bloqueado("nueva@ejemplo.com", OTRA_IP)).as("el mismo correo desde otra IP").isFalse();
    }

    @Test
    @DisplayName("Un login correcto no pone a cero el contador de la IP")
    void unAciertoNoReiniciaLaIp() {
        fallarConCorreosDistintos(49, IP);
        // Quien tenga una cuenta válida no puede reiniciar el contador de su IP entrando con ella.
        limite.registrarAcierto("valida@ejemplo.com");
        limite.registrarFallo("persona50@ejemplo.com", IP);

        assertThat(bloqueado("valida@ejemplo.com", IP)).isTrue();
    }

    @Test
    @DisplayName("El mensaje dice cuántos minutos faltan, redondeando hacia arriba")
    void elMensajeDiceLosMinutosQueFaltan() {
        fallar(5, CORREO, IP);
        comprobarEspera(900, "Demasiados intentos fallidos. Vuelve a intentarlo en 15 minutos.");

        reloj.avanzar(Duration.ofMinutes(5).plusSeconds(30));
        comprobarEspera(570, "Demasiados intentos fallidos. Vuelve a intentarlo en 10 minutos.");

        reloj.avanzar(Duration.ofMinutes(9));
        comprobarEspera(30, "Demasiados intentos fallidos. Vuelve a intentarlo en 1 minuto.");
    }

    @Test
    @DisplayName("Los contadores vencidos se borran de la memoria")
    void losContadoresVencidosSeBorran() {
        for (int i = 0; i < LimiteDeIntentosDeLogin.LIMPIAR_DESDE; i++) {
            limite.registrarFallo("persona" + i + "@ejemplo.com", "10.1." + (i / 256) + "." + (i % 256));
        }
        assertThat(limite.contadoresEnMemoria()).isEqualTo(2 * LimiteDeIntentosDeLogin.LIMPIAR_DESDE);

        reloj.avanzar(Duration.ofMinutes(15));
        limite.registrarFallo("ultima@ejemplo.com", "10.2.0.1");

        // Al pasar de 10.000, se borraron los vencidos: solo quedan el correo y la IP del último fallo.
        assertThat(limite.contadoresEnMemoria()).isEqualTo(2);
    }
}
