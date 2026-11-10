package com.voll.api.domain.consulta;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.validaciones.ValidadorDeConsultas;
import com.voll.api.domain.direccion.DatosDireccion;
import com.voll.api.domain.medico.DatosRegistroMedico;
import com.voll.api.domain.medico.Especialidad;
import com.voll.api.domain.medico.Medico;
import com.voll.api.domain.paciente.DatosRegistroPaciente;
import com.voll.api.domain.paciente.Paciente;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba la reserva de consultas contra un MySQL real. Testcontainers lo arranca en Docker, Flyway aplica
 * las migraciones y al terminar se descarta, así que tu base vollmed_api no se toca. Cubre BUG-01 de
 * docs/revisiones/0001-revision-inicial.md: una hora cancelada se puede volver a reservar con el mismo médico.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("sePuedeEjecutar")
class ReservaDeConsultasConMySQLTest {

    @TestConfiguration(proxyBeanMethods = false)
    @Import(ReservaDeConsultas.class)
    @ComponentScan(basePackageClasses = ValidadorDeConsultas.class) // los validadores de la reserva y la cancelación
    static class Configuracion {

        @Bean
        @ServiceConnection
        MySQLContainer mysql() {
            // La base se llama vollmed_api porque V5 inserta en vollmed_api.usuarios (CAL-04).
            // Es otro servidor, dentro de Docker: tu MySQL local no se toca.
            return new MySQLContainer("mysql:8.4").withDatabaseName("vollmed_api");
        }
    }

    // Sin Docker (por ejemplo, con Docker Desktop cerrado) estas pruebas se saltan en tu equipo.
    // En el CI (GitHub Actions define CI=true) se ejecutan siempre, para que no se salten sin avisar.
    static boolean sePuedeEjecutar() {
        return System.getenv("CI") != null || DockerClientFactory.instance().isDockerAvailable();
    }

    private static final LocalDateTime LUNES_A_LAS_10 = LocalDateTime.of(2030, 1, 7, 10, 0);
    private static final Usuario RECEPCIONISTA = new Usuario("recepcion@ejemplo.com", "sin-uso", Rol.RECEPCIONISTA);

    @Autowired
    private ReservaDeConsultas reserva;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("Con el médico elegido: una consulta programada sigue ocupando la hora")
    void medicoElegidoConConsultaProgramada() {
        var medico = registrarMedico();
        reservarCon(medico, registrarPaciente(1));

        assertThatThrownBy(() -> reservarCon(medico, registrarPaciente(2)))
                .isInstanceOf(ValidacionException.class)
                .hasMessage("Medico ya tiene otra consulta en esa misma fecha y hora");
    }

    @Test
    @DisplayName("Con el médico elegido: una hora cancelada se puede volver a reservar (BUG-01)")
    void medicoElegidoConConsultaCancelada() {
        var medico = registrarMedico();
        cancelar(reservarCon(medico, registrarPaciente(1)));

        var nueva = reservarCon(medico, registrarPaciente(2));

        assertThat(nueva.idMedico()).isEqualTo(medico.getId());
    }

    @Test
    @DisplayName("Por especialidad: si el único médico tiene una consulta programada, no hay médico libre")
    void porEspecialidadConConsultaProgramada() {
        registrarMedico();
        reservarPorEspecialidad(registrarPaciente(1));

        assertThatThrownBy(() -> reservarPorEspecialidad(registrarPaciente(2)))
                .isInstanceOf(ValidacionException.class)
                .hasMessage("No existe un médico disponible en ese horario");
    }

    @Test
    @DisplayName("Por especialidad: el médico de una hora cancelada vuelve a estar libre (BUG-01)")
    void porEspecialidadConConsultaCancelada() {
        var medico = registrarMedico();
        cancelar(reservarPorEspecialidad(registrarPaciente(1)));

        var nueva = reservarPorEspecialidad(registrarPaciente(2));

        assertThat(nueva.idMedico()).isEqualTo(medico.getId());
    }

    private DatosDetalleConsulta reservarCon(Medico medico, Paciente paciente) {
        var datos = new DatosReservaConsulta(medico.getId(), paciente.getId(), LUNES_A_LAS_10, null);
        return reserva.reservar(datos, RECEPCIONISTA);
    }

    private DatosDetalleConsulta reservarPorEspecialidad(Paciente paciente) {
        var datos = new DatosReservaConsulta(null, paciente.getId(), LUNES_A_LAS_10, Especialidad.CARDIOLOGIA);
        return reserva.reservar(datos, RECEPCIONISTA);
    }

    private void cancelar(DatosDetalleConsulta consulta) {
        reserva.cancelar(consulta.id(), MotivoCancelamiento.PACIENTE_DESISTIO, RECEPCIONISTA);
    }

    private Medico registrarMedico() {
        var medico = new Medico(new DatosRegistroMedico("Ana Torres", "ana@ejemplo.com", "912345678", "10000001",
                Especialidad.CARDIOLOGIA, direccion(), "Clave-de-prueba-1"));
        em.persist(medico);
        return medico;
    }

    private Paciente registrarPaciente(int numero) {
        var paciente = new Paciente(new DatosRegistroPaciente("Paciente " + numero, "paciente" + numero + "@ejemplo.com",
                "98765432" + numero, "2000000" + numero, direccion(), "Clave-de-prueba-1"));
        em.persist(paciente);
        return paciente;
    }

    private static DatosDireccion direccion() {
        return new DatosDireccion("Calle 1", null, null, "Centro", "Lima", "15001", "Lima");
    }
}
