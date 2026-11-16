package com.voll.api.domain.consulta;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.validaciones.ValidadorDeConsultas;
import com.voll.api.domain.direccion.DatosDireccion;
import com.voll.api.domain.medico.DatosRegistroMedico;
import com.voll.api.domain.medico.Especialidad;
import com.voll.api.domain.medico.Medico;
import com.voll.api.domain.medico.MedicoService;
import com.voll.api.domain.paciente.DatosRegistroPaciente;
import com.voll.api.domain.paciente.Paciente;
import com.voll.api.domain.usuario.EstadoUsuario;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import com.voll.api.domain.usuario.UsuarioService;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.voll.api.infra.reloj.ConfiguracionDelReloj.ZONA_DE_LA_CLINICA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba la reserva de consultas contra un MySQL real. Testcontainers lo arranca en Docker, Flyway aplica
 * las migraciones y al terminar se descarta, así que tu base vollmed_api no se toca. Cubre BUG-01 de
 * docs/revisiones/0001-revision-inicial.md: una hora cancelada se puede volver a reservar con el mismo médico.
 * Y BUG-05: a un médico con la cuenta de baja no se le puede reservar, y al reincorporarlo sí.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("sePuedeEjecutar")
class ReservaDeConsultasConMySQLTest {

    @TestConfiguration(proxyBeanMethods = false)
    @Import({ReservaDeConsultas.class, MedicoService.class, UsuarioService.class})
    @ComponentScan(basePackageClasses = ValidadorDeConsultas.class) // los validadores de la reserva y la cancelación
    static class Configuracion {

        @Bean
        @ServiceConnection
        MySQLContainer mysql() {
            // Es otro servidor, dentro de Docker: tu MySQL local no se toca. La base se llama «test», el nombre
            // por defecto de Testcontainers: desde CAL-04, las migraciones funcionan con cualquier nombre.
            return new MySQLContainer("mysql:8.4");
        }

        // Las reglas de la reserva piden la hora de la clínica (SEC-07). Aquí se fija una semana antes de las
        // consultas de la prueba, así da lo mismo cuándo se ejecute (antes, la prueba habría dejado de pasar
        // en enero de 2030).
        @Bean
        Clock reloj() {
            return Clock.fixed(LUNES_A_LAS_10.minusWeeks(1).atZone(ZONA_DE_LA_CLINICA).toInstant(), ZONA_DE_LA_CLINICA);
        }

        // MedicoService guarda la contraseña de la cuenta cifrada, como en la aplicación.
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    // Sin Docker (por ejemplo, con Docker Desktop cerrado) estas pruebas se saltan en tu equipo.
    // En el CI (GitHub Actions define CI=true) se ejecutan siempre, para que no se salten sin avisar.
    static boolean sePuedeEjecutar() {
        return System.getenv("CI") != null || DockerClientFactory.instance().isDockerAvailable();
    }

    private static final LocalDateTime LUNES_A_LAS_10 = LocalDateTime.of(2030, 1, 7, 10, 0);
    private static final Usuario RECEPCIONISTA = new Usuario("recepcion@ejemplo.com", "sin-uso", Rol.RECEPCIONISTA);
    // De este administrador solo se usa el id, para la regla de que nadie cambia el estado de su propia cuenta.
    private static final Usuario ADMINISTRADOR =
            new Usuario(999L, "admin@ejemplo.com", "sin-uso", Rol.ADMINISTRADOR, null, null, EstadoUsuario.ACTIVO);

    @Autowired
    private ReservaDeConsultas reserva;

    @Autowired
    private MedicoService medicoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

    @Test
    @DisplayName("No se puede reservar con un médico cuya cuenta se dio de baja en el panel de usuarios (BUG-05)")
    void medicoConLaCuentaDeBaja() {
        var idMedico = registrarMedicoConCuenta();
        var paciente = registrarPaciente(1);

        // «Dar de baja» en el panel de usuarios
        usuarioService.darDeBaja(cuentaDelMedico(idMedico).getId(), ADMINISTRADOR);
        guardarYOlvidar();

        assertThatThrownBy(() -> reservarCon(idMedico, paciente))
                .isInstanceOf(ValidacionException.class)
                .hasMessage("Consulta no puede ser reservada con medico inactivo");
    }

    @Test
    @DisplayName("Al reincorporar a un médico dado de baja, se le puede volver a reservar (BUG-05)")
    void medicoReincorporado() {
        var idMedico = registrarMedicoConCuenta();
        var paciente = registrarPaciente(1);

        // «Dar de baja» en «Médicos activos» y después «Reincorporar» en el panel de usuarios
        medicoService.eliminar(idMedico);
        usuarioService.reactivar(cuentaDelMedico(idMedico).getId(), ADMINISTRADOR);
        guardarYOlvidar();

        var consulta = reservarCon(idMedico, paciente);

        assertThat(consulta.idMedico()).isEqualTo(idMedico);
    }

    private DatosDetalleConsulta reservarCon(Medico medico, Paciente paciente) {
        return reservarCon(medico.getId(), paciente);
    }

    private DatosDetalleConsulta reservarCon(Long idMedico, Paciente paciente) {
        var datos = new DatosReservaConsulta(idMedico, paciente.getId(), LUNES_A_LAS_10, null);
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

    // Registra al médico como el panel del administrador: con su ficha y su cuenta. Devuelve el id de la ficha.
    private Long registrarMedicoConCuenta() {
        return medicoService.registrar(new DatosRegistroMedico("Ana Torres", "ana@ejemplo.com", "912345678",
                "10000001", Especialidad.CARDIOLOGIA, direccion(), "Clave-de-prueba-1")).id();
    }

    private Usuario cuentaDelMedico(Long idMedico) {
        return usuarioRepository.findByMedicoId(idMedico).orElseThrow();
    }

    // Escribe los cambios en MySQL y vacía la memoria de JPA: lo que venga después se vuelve a leer de la base.
    private void guardarYOlvidar() {
        em.flush();
        em.clear();
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
