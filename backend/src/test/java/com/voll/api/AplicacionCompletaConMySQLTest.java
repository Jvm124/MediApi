package com.voll.api;

import com.voll.api.controller.AutenticacionController;
import com.voll.api.controller.ConsultaController;
import com.voll.api.controller.MedicoController;
import com.voll.api.controller.PacienteController;
import com.voll.api.controller.UsuarioController;
import com.voll.api.domain.usuario.UsuarioRepository;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Arranca la aplicación completa contra un MySQL desechable en Docker (Testcontainers) y comprueba que todas
 * sus piezas se conectan: controladores, servicios, validadores y seguridad. Protege la inyección por
 * constructor de CAL-01 (docs/revisiones/0001-revision-inicial.md): si a una clase le faltara una dependencia,
 * la aplicación no arrancaría y esta prueba fallaría. También comprueba que la aplicación usa la hora de Lima
 * (SEC-07), y que las migraciones funcionan en una base con otro nombre y no dejan ningún usuario (CAL-04).
 */
@SpringBootTest(properties = {
        "api.security.token.secret=secreto-solo-para-pruebas",
        // Sin admin inicial, para que la prueba no dependa de ADMIN_EMAIL ni de ADMIN_PASSWORD.
        "api.admin.correo=",
        "api.admin.contrasenia="
})
@EnabledIf("sePuedeEjecutar")
class AplicacionCompletaConMySQLTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuracion {

        @Bean
        @ServiceConnection
        MySQLContainer mysql() {
            // Es otro servidor, dentro de Docker: tu MySQL local no se toca. La base se llama «test», el nombre
            // por defecto de Testcontainers: desde CAL-04, las migraciones funcionan con cualquier nombre.
            return new MySQLContainer("mysql:8.4");
        }
    }

    // Como en ReservaDeConsultasConMySQLTest: sin Docker se salta en tu equipo, y en el CI se ejecuta siempre.
    static boolean sePuedeEjecutar() {
        return System.getenv("CI") != null || DockerClientFactory.instance().isDockerAvailable();
    }

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("La aplicación arranca con todos sus controladores y lo que necesitan")
    void arrancaConTodasLasPiezasConectadas() {
        assertThat(contexto.getBean(AutenticacionController.class)).isNotNull();
        assertThat(contexto.getBean(ConsultaController.class)).isNotNull();
        assertThat(contexto.getBean(MedicoController.class)).isNotNull();
        assertThat(contexto.getBean(PacienteController.class)).isNotNull();
        assertThat(contexto.getBean(UsuarioController.class)).isNotNull();
    }

    @Test
    @DisplayName("Las reglas y las validaciones de los datos usan la hora de Lima")
    void usaLaHoraDeLima() {
        var reloj = contexto.getBean(Clock.class);
        assertThat(reloj.getZone()).isEqualTo(ZoneId.of("America/Lima"));
        // El validador con el que Spring revisa los datos que llegan a la API (@Valid), @Future incluido, usa
        // ese mismo reloj. Sin ConfiguracionDelReloj usaría otro, con la zona horaria del equipo.
        assertThat(contexto.getBean(ValidatorFactory.class).getClockProvider().getClock()).isSameAs(reloj);
    }

    @Test
    @DisplayName("Una base nueva no trae ningún usuario: el admin inicial solo lo crea InicializadorDeAdmin")
    void unaBaseNuevaNoTraeUsuarios() {
        // Antes, V5 sembraba al admin con la contraseña publicada (SEC-01 y CAL-04). Esta prueba no configura el
        // admin inicial, así que cualquier usuario tendría que venir de las migraciones.
        assertThat(contexto.getBean(UsuarioRepository.class).count()).isZero();
    }
}
