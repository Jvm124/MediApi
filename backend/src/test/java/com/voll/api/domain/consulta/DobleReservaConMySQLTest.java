package com.voll.api.domain.consulta;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.validaciones.ValidadorDeConsultas;
import com.voll.api.domain.direccion.DatosDireccion;
import com.voll.api.domain.medico.DatosRegistroMedico;
import com.voll.api.domain.medico.Especialidad;
import com.voll.api.domain.medico.Medico;
import com.voll.api.domain.medico.MedicoRepository;
import com.voll.api.domain.paciente.DatosRegistroPaciente;
import com.voll.api.domain.paciente.Paciente;
import com.voll.api.domain.paciente.PacienteRepository;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.voll.api.infra.reloj.ConfiguracionDelReloj.ZONA_DE_LA_CLINICA;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * BUG-06 de docs/revisiones/0001-revision-inicial.md: dos pacientes reservan al mismo médico a la misma hora en el
 * mismo instante. Cada reserva revisa «¿el médico ya tiene otra consulta a esa hora?» antes de que la otra se guarde,
 * así que las dos pasan la revisión y se guardaban las dos. Como en ReservaDeConsultasConMySQLTest, Testcontainers
 * arranca un MySQL aparte en Docker y tu base vollmed_api no se toca.
 *
 * Cada reserva va en su propio hilo y en su propia transacción, como dos peticiones que llegan a la vez. Por eso estas
 * pruebas no van dentro de una transacción que se deshace al final, y lo que guardan se borra en limpiar().
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIf("sePuedeEjecutar")
class DobleReservaConMySQLTest {

    @TestConfiguration(proxyBeanMethods = false)
    @Import(ReservaDeConsultas.class)
    @ComponentScan(basePackageClasses = ValidadorDeConsultas.class) // los validadores de la reserva y la cancelación
    static class Configuracion {

        @Bean
        @ServiceConnection
        MySQLContainer mysql() {
            return new MySQLContainer("mysql:8.4");
        }

        // La hora de la clínica, una semana antes de las consultas de la prueba (SEC-07).
        @Bean
        Clock reloj() {
            return Clock.fixed(LUNES_A_LAS_10.minusWeeks(1).atZone(ZONA_DE_LA_CLINICA).toInstant(), ZONA_DE_LA_CLINICA);
        }

        // Los @Bean se registran después de los validadores que encuentra @ComponentScan, así que la reserva llama a
        // este al final de todos. comprobarElOrden() lo vigila.
        @Bean
        EsperarALaOtraReserva esperarALaOtraReserva() {
            return new EsperarALaOtraReserva();
        }
    }

    // Un validador solo de esta prueba: hace esperar a cada reserva hasta que la otra también llegue aquí. Como va
    // después de los de verdad, las dos ya revisaron que el médico estaba libre y ninguna se guardó todavía. Es justo
    // el momento en que dos peticiones a la vez se pisan.
    static class EsperarALaOtraReserva implements ValidadorDeConsultas {

        private final CyclicBarrier barrera = new CyclicBarrier(2);

        @Override
        public void validar(DatosReservaConsulta datos) {
            try {
                barrera.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                throw new IllegalStateException("La otra reserva no llegó a tiempo", e);
            }
        }

        void reiniciar() {
            barrera.reset();
        }
    }

    // Sin Docker (por ejemplo, con Docker Desktop cerrado) estas pruebas se saltan en tu equipo.
    // En el CI (GitHub Actions define CI=true) se ejecutan siempre, para que no se salten sin avisar.
    static boolean sePuedeEjecutar() {
        return System.getenv("CI") != null || DockerClientFactory.instance().isDockerAvailable();
    }

    private static final LocalDateTime LUNES_A_LAS_10 = LocalDateTime.of(2030, 1, 7, 10, 0);
    private static final Usuario RECEPCIONISTA = new Usuario("recepcion@ejemplo.com", "sin-uso", Rol.RECEPCIONISTA);

    // Lo que pasó con cada reserva: la consulta guardada o el error.
    private record Resultado(DatosDetalleConsulta consulta, Throwable error) {
    }

    @Autowired
    private ReservaDeConsultas reserva;

    @Autowired
    private List<ValidadorDeConsultas> validadores;

    @Autowired
    private EsperarALaOtraReserva esperar;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsultaRepository consultaRepository;

    // Si la espera no fuera lo último, una reserva podría guardarse antes de que la otra revise el horario, y la prueba
    // ya no reproduciría el choque.
    @BeforeEach
    void comprobarElOrden() {
        assertThat(validadores.getLast()).isSameAs(esperar);
    }

    @AfterEach
    void limpiar() {
        esperar.reiniciar();
        consultaRepository.deleteAllInBatch();
        pacienteRepository.deleteAllInBatch();
        medicoRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Dos reservas a la vez con el mismo médico y a la misma hora: solo se guarda una (BUG-06)")
    void mismoMedicoALaMismaHora() throws InterruptedException {
        var medico = medicoRepository.save(nuevoMedico());
        var paciente1 = pacienteRepository.save(nuevoPaciente(1));
        var paciente2 = pacienteRepository.save(nuevoPaciente(2));

        var resultados = reservarALaVez(
                () -> reservar(medico, paciente1, LUNES_A_LAS_10),
                () -> reservar(medico, paciente2, LUNES_A_LAS_10));

        assertThat(resultados).filteredOn(r -> r.error() == null).hasSize(1);
        assertThat(resultados).filteredOn(r -> r.error() != null).singleElement()
                .satisfies(r -> assertThat(r.error())
                        .isInstanceOf(ValidacionException.class)
                        .hasMessage("Ese horario se acaba de ocupar. Vuelve a intentarlo o elige otro."));
        assertThat(consultaRepository.buscarProgramadasDelMedico(medico.getId(), EstadoConsulta.PROGRAMADA))
                .hasSize(1);
    }

    @Test
    @DisplayName("Dos reservas a la vez con el mismo médico y a horas distintas: se guardan las dos")
    void mismoMedicoAHorasDistintas() throws InterruptedException {
        var medico = medicoRepository.save(nuevoMedico());
        var paciente1 = pacienteRepository.save(nuevoPaciente(1));
        var paciente2 = pacienteRepository.save(nuevoPaciente(2));

        var resultados = reservarALaVez(
                () -> reservar(medico, paciente1, LUNES_A_LAS_10),
                () -> reservar(medico, paciente2, LUNES_A_LAS_10.plusHours(1)));

        assertThat(resultados).allSatisfy(r -> assertThat(r.error()).isNull());
        assertThat(consultaRepository.buscarProgramadasDelMedico(medico.getId(), EstadoConsulta.PROGRAMADA))
                .hasSize(2);
    }

    // Lanza las dos reservas en dos hilos, como dos peticiones que llegan a la vez, y espera a que las dos terminen.
    private List<Resultado> reservarALaVez(Callable<DatosDetalleConsulta> primera,
                                          Callable<DatosDetalleConsulta> segunda) throws InterruptedException {
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var enCurso = List.of(hilos.submit(primera), hilos.submit(segunda));
            var resultados = new ArrayList<Resultado>();
            for (var unaReserva : enCurso) {
                try {
                    resultados.add(new Resultado(unaReserva.get(30, TimeUnit.SECONDS), null));
                } catch (ExecutionException e) {
                    resultados.add(new Resultado(null, e.getCause()));
                } catch (TimeoutException e) {
                    throw new IllegalStateException("Una reserva no terminó a tiempo", e);
                }
            }
            return resultados;
        }
    }

    private DatosDetalleConsulta reservar(Medico medico, Paciente paciente, LocalDateTime fecha) {
        return reserva.reservar(new DatosReservaConsulta(medico.getId(), paciente.getId(), fecha, null), RECEPCIONISTA);
    }

    private static Medico nuevoMedico() {
        return new Medico(new DatosRegistroMedico("Ana Torres", "ana@ejemplo.com", "912345678", "10000001",
                Especialidad.CARDIOLOGIA, direccion(), "Clave-de-prueba-1"));
    }

    private static Paciente nuevoPaciente(int numero) {
        return new Paciente(new DatosRegistroPaciente("Paciente " + numero, "paciente" + numero + "@ejemplo.com",
                "98765432" + numero, "2000000" + numero, direccion(), "Clave-de-prueba-1"));
    }

    private static DatosDireccion direccion() {
        return new DatosDireccion("Calle 1", null, null, "Centro", "Lima", "15001", "Lima");
    }
}
