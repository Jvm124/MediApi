package com.voll.api;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba la migración V9 (BUG-06) contra un MySQL desechable de Testcontainers, sin Spring, con consultas que ya
 * estaban en la base. Cada prueba vacía la base, aplica las migraciones hasta la V8, guarda consultas con SQL del
 * mismo médico a la misma hora y después aplica la V9.
 */
@EnabledIf("sePuedeEjecutar")
class MigracionV9ConMySQLTest {

    private static final LocalDateTime LUNES_A_LAS_10 = LocalDateTime.of(2030, 1, 7, 10, 0);

    private static MySQLContainer mysql;
    private static JdbcTemplate jdbc;

    // Sin Docker (por ejemplo, con Docker Desktop cerrado) estas pruebas se saltan en tu equipo.
    // En el CI (GitHub Actions define CI=true) se ejecutan siempre, para que no se salten sin avisar.
    static boolean sePuedeEjecutar() {
        return System.getenv("CI") != null || DockerClientFactory.instance().isDockerAvailable();
    }

    @BeforeAll
    static void arrancarMySQL() {
        mysql = new MySQLContainer("mysql:8.4");
        mysql.start();
        jdbc = new JdbcTemplate(
                new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
    }

    @AfterAll
    static void apagarMySQL() {
        if (mysql != null) {
            mysql.stop();
        }
    }

    // La base vacía, con las migraciones hasta la V8, un médico (el 1) y tres pacientes (1, 2 y 3).
    @BeforeEach
    void baseHastaLaV8() {
        flywayHasta("8").clean();
        flywayHasta("8").migrate();
        jdbc.update("""
                INSERT INTO medicos (id, nombre, email, documento, telefono, especialidad,
                                     calle, barrio, ciudad, estado, codigo_postal, activo)
                VALUES (1, 'Médico 1', 'medico1@ejemplo.com', '10000001', '912345678', 'CARDIOLOGIA',
                        'Calle 1', 'Centro', 'Lima', 'Lima', '15001', true)
                """);
        for (long id = 1; id <= 3; id++) {
            jdbc.update("""
                    INSERT INTO pacientes (id, nombre, email, documento,
                                           calle, barrio, ciudad, estado, codigo_postal, activo)
                    VALUES (?, ?, ?, ?, 'Calle 1', 'Centro', 'Lima', 'Lima', '15001', true)
                    """, id, "Paciente " + id, "paciente" + id + "@ejemplo.com", "2000000" + id);
        }
    }

    @Test
    @DisplayName("Si las otras consultas de esa hora están canceladas o atendidas, la V9 se aplica")
    void seAplicaConCanceladasYAtendidas() {
        consultaDelMedico1ALas10(1, "PROGRAMADA");
        consultaDelMedico1ALas10(2, "CANCELADA");
        consultaDelMedico1ALas10(3, "ATENDIDA");

        flywayHasta("9").migrate();

        assertThat(jdbc.queryForList("SELECT estado, medico_programado FROM consultas ORDER BY paciente_id"))
                .extracting(fila -> fila.get("estado") + " " + fila.get("medico_programado"))
                .containsExactly("PROGRAMADA 1", "CANCELADA null", "ATENDIDA null");
        // Otra consulta programada del mismo médico a la misma hora ya no entra.
        assertThatThrownBy(() -> consultaDelMedico1ALas10(2, "PROGRAMADA"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("Con dos consultas programadas del mismo médico a la misma hora, la V9 falla y la tabla no cambia")
    void fallaConDosProgramadas() {
        consultaDelMedico1ALas10(1, "PROGRAMADA");
        consultaDelMedico1ALas10(2, "PROGRAMADA");

        assertThatThrownBy(() -> flywayHasta("9").migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("uk_consultas_medico_fecha");

        assertThat(tieneLaColumnaDeLaV9()).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM consultas", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("Si la V9 falló, se aplica después de cancelar una de las dos y borrar su fila fallida")
    void seAplicaDespuesDeCorregirLosDatos() {
        consultaDelMedico1ALas10(1, "PROGRAMADA");
        consultaDelMedico1ALas10(2, "PROGRAMADA");
        assertThatThrownBy(() -> flywayHasta("9").migrate()).isInstanceOf(FlywayException.class);

        // Los pasos de BUG-06 en docs/revisiones/0001-revision-inicial.md.
        jdbc.update("UPDATE consultas SET estado = 'CANCELADA' WHERE paciente_id = 2");
        // Cancelar no basta: Flyway recuerda que la V9 falló y no deja seguir.
        assertThatThrownBy(() -> flywayHasta("9").migrate()).hasMessageContaining("failed migration");
        jdbc.update("DELETE FROM flyway_schema_history WHERE version = '9' AND success = 0");
        flywayHasta("9").migrate();

        assertThat(tieneLaColumnaDeLaV9()).isTrue();
    }

    // Hasta esa versión, no hasta la última: así una migración que llegue después no cambia lo que se prueba aquí.
    // clean() vacía la base; Flyway lo trae apagado y aquí se enciende, porque esta base es desechable.
    private static Flyway flywayHasta(String version) {
        return Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .target(version)
                .cleanDisabled(false)
                .load();
    }

    private static void consultaDelMedico1ALas10(long idPaciente, String estado) {
        jdbc.update("INSERT INTO consultas (medico_id, paciente_id, fecha, estado) VALUES (1, ?, ?, ?)",
                idPaciente, LUNES_A_LAS_10, estado);
    }

    private static boolean tieneLaColumnaDeLaV9() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'consultas' AND column_name = 'medico_programado'
                """, Integer.class) == 1;
    }
}
