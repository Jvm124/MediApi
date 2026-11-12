package com.voll.api.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos de login (SEC-04 en docs/revisiones/0001-revision-inicial.md, y docs/adr/0005).
 * Antes, un programa podía probar miles de contraseñas seguidas.
 * <p>
 * Cuenta los fallos de dos formas, y al llegar al límite bloquea 15 minutos:
 * <ul>
 *     <li><b>Por correo</b>: 5 fallos en 15 minutos. Frena a quien intenta adivinar la contraseña de una cuenta.
 *     Mientras dura el bloqueo, ese correo no entra ni con la contraseña correcta. Un login correcto pone su
 *     contador a cero.</li>
 *     <li><b>Por IP</b>: 50 fallos en 15 minutos. Frena a quien prueba una misma contraseña en muchas cuentas. El
 *     límite es más alto porque en una clínica varias personas pueden compartir la IP. Un login correcto no lo pone
 *     a cero: si no, quien tuviera una cuenta válida podría reiniciarlo entrando con ella entre intento e
 *     intento.</li>
 * </ul>
 * Los 15 minutos se cuentan desde el primer fallo, con el reloj de la aplicación (ConfiguracionDelReloj), así que
 * las pruebas pueden adelantar la hora. Los contadores viven en memoria: al reiniciar el backend se borran.
 */
@Component
@RequiredArgsConstructor
public class LimiteDeIntentosDeLogin {

    static final int FALLOS_POR_CORREO = 5;
    static final int FALLOS_POR_IP = 50;
    static final Duration VENTANA = Duration.ofMinutes(15);
    static final Duration BLOQUEO = Duration.ofMinutes(15);
    // Si un mapa pasa de estos contadores, se borran los vencidos, para que la memoria no crezca sin límite.
    static final int LIMPIAR_DESDE = 10_000;

    private final Clock reloj;
    private final Map<String, Contador> porCorreo = new ConcurrentHashMap<>();
    private final Map<String, Contador> porIp = new ConcurrentHashMap<>();

    /**
     * Lanza DemasiadosIntentosException si el correo o la IP están bloqueados. Se llama antes de mirar la
     * contraseña.
     */
    public void comprobar(String correo, String ip) {
        var ahora = reloj.instant();
        var hasta = elMasTardio(bloqueadoHasta(porCorreo.get(clave(correo)), ahora),
                bloqueadoHasta(porIp.get(ip), ahora));
        if (hasta != null) {
            throw new DemasiadosIntentosException(Duration.between(ahora, hasta));
        }
    }

    public void registrarFallo(String correo, String ip) {
        var ahora = reloj.instant();
        sumarFallo(porCorreo, clave(correo), FALLOS_POR_CORREO, ahora);
        sumarFallo(porIp, ip, FALLOS_POR_IP, ahora);
    }

    public void registrarAcierto(String correo) {
        porCorreo.remove(clave(correo));
    }

    // Solo para la prueba que comprueba que los contadores vencidos se borran.
    int contadoresEnMemoria() {
        return porCorreo.size() + porIp.size();
    }

    // «Ana@Ejemplo.com» y « ana@ejemplo.com » comparten contador. Las demás variantes que MySQL trata como el mismo
    // correo (tildes, letras de ancho completo...) las resuelve antes AutenticacionService.correoGuardado.
    private static String clave(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }

    private static void sumarFallo(Map<String, Contador> contadores, String clave, int limite, Instant ahora) {
        contadores.compute(clave, (mismaClave, contador) -> {
            if (contador == null || contador.vencido(ahora)) {
                contador = new Contador(0, ahora, null);
            }
            var fallos = contador.fallos() + 1;
            var bloqueadoHasta = contador.bloqueadoHasta() == null && fallos >= limite
                    ? ahora.plus(BLOQUEO)
                    : contador.bloqueadoHasta();
            return new Contador(fallos, contador.desde(), bloqueadoHasta);
        });
        if (contadores.size() > LIMPIAR_DESDE) {
            contadores.values().removeIf(contador -> contador.vencido(ahora));
        }
    }

    private static Instant bloqueadoHasta(Contador contador, Instant ahora) {
        return contador != null && contador.bloqueado(ahora) ? contador.bloqueadoHasta() : null;
    }

    private static Instant elMasTardio(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        return b == null || a.isAfter(b) ? a : b;
    }

    /**
     * Los fallos de un correo o de una IP desde «desde», el primero, y hasta cuándo están bloqueados (null si no
     * lo están).
     */
    private record Contador(int fallos, Instant desde, Instant bloqueadoHasta) {

        boolean bloqueado(Instant ahora) {
            return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
        }

        // Ya no bloquea y su primer fallo tiene 15 minutos o más: se empieza de cero, o se puede borrar.
        boolean vencido(Instant ahora) {
            return !bloqueado(ahora) && !ahora.isBefore(desde.plus(VENTANA));
        }
    }
}
