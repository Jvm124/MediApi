package com.voll.api;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba la migración V8 (BUG-05, docs/adr/0006) contra un MySQL desechable de Testcontainers, sin Spring.
 * Flyway aplica las migraciones hasta la V7. Después la prueba guarda cuentas y fichas desincronizadas, como las
 * dejaba la aplicación antes de BUG-05, y Flyway aplica la V8. Cada prueba revisa cómo quedaron.
 */
@EnabledIf("sePuedeEjecutar")
class MigracionV8ConMySQLTest {

    private static MySQLContainer mysql;
    private static JdbcTemplate jdbc;
    private static Map<String, String> antes;
    private static Map<String, String> despues;

    // Sin Docker (por ejemplo, con Docker Desktop cerrado) estas pruebas se saltan en tu equipo.
    // En el CI (GitHub Actions define CI=true) se ejecutan siempre, para que no se salten sin avisar.
    static boolean sePuedeEjecutar() {
        return System.getenv("CI") != null || DockerClientFactory.instance().isDockerAvailable();
    }

    @BeforeAll
    static void aplicarLaV8SobreDatosDesincronizados() {
        mysql = new MySQLContainer("mysql:8.4");
        mysql.start();
        jdbc = new JdbcTemplate(
                new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));

        migrarHasta("7");

        // Médicos: el número es el id de la ficha, y cada uno tiene su cuenta.
        medicoConCuenta(1, true, "ACTIVO");        // cumple la regla: no cambia
        medicoConCuenta(2, true, "BAJA");          // cuenta dada de baja en el panel de usuarios
        medicoConCuenta(3, true, "SUSPENDIDO");    // cuenta suspendida
        medicoConCuenta(4, false, "ACTIVO");       // dado de baja en «Médicos activos» y después «Reincorporar»
        medicoConCuenta(5, false, "BAJA");         // cumple la regla: no cambia
        medicoConCuenta(6, null, "ACTIVO");        // ficha sin valor: solo pasa si alguien toca la base a mano
        // Pacientes
        pacienteConCuenta(1, false, "ACTIVO");     // dado de baja: su cuenta seguía ACTIVO
        pacienteConCuenta(2, true, "BAJA");        // cuenta dada de baja en el panel de usuarios
        pacienteConCuenta(3, false, "SUSPENDIDO"); // cumple la regla: no cambia
        pacienteSinCuenta(4, true);                // alta por mostrador, sin cuenta: no cambia
        pacienteSinCuenta(5, false);               // de mostrador y dado de baja: no cambia
        // Cuentas sin ficha: no cambian
        cuentaSinFicha("admin@ejemplo.com", "ADMINISTRADOR", "ACTIVO");
        cuentaSinFicha("recepcion@ejemplo.com", "RECEPCIONISTA", "SUSPENDIDO");

        antes = foto();
        // Hasta la V8, no hasta la última: así una migración que llegue después no cambia lo que se prueba aquí.
        migrarHasta("8");
        despues = foto();
    }

    @AfterAll
    static void apagarMySQL() {
        if (mysql != null) {
            mysql.stop();
        }
    }

    @Test
    @DisplayName("Desactiva las fichas cuya cuenta no está ACTIVA, y pasa a BAJA las cuentas con la ficha desactivada")
    void corrigeLoDesincronizado() {
        assertThat(despues.keySet()).isEqualTo(antes.keySet());
        assertThat(cambios()).isEqualTo(Map.of(
                "ficha del médico 2", "desactivada",   // su cuenta estaba de baja
                "ficha del médico 3", "desactivada",   // su cuenta estaba suspendida
                "ficha del paciente 2", "desactivada", // su cuenta estaba de baja
                "cuenta del médico 4", "BAJA",         // su ficha estaba desactivada
                "cuenta del médico 6", "BAJA",         // su ficha no tenía valor
                "cuenta del paciente 1", "BAJA"));     // su ficha estaba desactivada
    }

    @Test
    @DisplayName("Solo apaga: ninguna cuenta pasa a ACTIVO y ninguna ficha se activa")
    void soloApaga() {
        assertThat(cambios().values()).allMatch(estado -> estado.equals("BAJA") || estado.equals("desactivada"));
    }

    @Test
    @DisplayName("Después de la V8, toda ficha con cuenta está activa solo si su cuenta está ACTIVA")
    void todasCumplenLaRegla() {
        var queNoCumplen = jdbc.queryForList("""
                SELECT u.correo
                FROM usuarios u
                LEFT JOIN medicos m ON m.id = u.medico_id
                LEFT JOIN pacientes p ON p.id = u.paciente_id
                WHERE (u.medico_id IS NOT NULL OR u.paciente_id IS NOT NULL)
                  AND (u.estado = 'ACTIVO') <> (COALESCE(m.activo, p.activo, 0) = 1)
                """, String.class);

        assertThat(queNoCumplen).isEmpty();
    }

    private static void migrarHasta(String version) {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .target(version)
                .load()
                .migrate();
    }

    // Una foto del estado de cada ficha y de cada cuenta, con nombres fáciles de leer si una prueba falla.
    private static Map<String, String> foto() {
        var foto = new TreeMap<String, String>();
        jdbc.queryForList("SELECT id, activo FROM medicos").forEach(fila ->
                foto.put("ficha del médico " + fila.get("id"), comoTexto(fila.get("activo"))));
        jdbc.queryForList("SELECT id, activo FROM pacientes").forEach(fila ->
                foto.put("ficha del paciente " + fila.get("id"), comoTexto(fila.get("activo"))));
        jdbc.queryForList("SELECT correo, medico_id, paciente_id, estado FROM usuarios").forEach(fila ->
                foto.put(nombreDeLaCuenta(fila), (String) fila.get("estado")));
        return foto;
    }

    // Lo que cambió con la V8: cada ficha o cuenta que ya no está como antes, con su estado nuevo.
    private static Map<String, String> cambios() {
        var cambios = new TreeMap<String, String>();
        despues.forEach((nombre, estado) -> {
            if (!estado.equals(antes.get(nombre))) {
                cambios.put(nombre, estado);
            }
        });
        return cambios;
    }

    private static String comoTexto(Object activo) {
        if (activo == null) {
            return "sin valor";
        }
        var estaActiva = activo instanceof Boolean valor ? valor : ((Number) activo).intValue() == 1;
        return estaActiva ? "activa" : "desactivada";
    }

    private static String nombreDeLaCuenta(Map<String, Object> fila) {
        if (fila.get("medico_id") != null) {
            return "cuenta del médico " + fila.get("medico_id");
        }
        if (fila.get("paciente_id") != null) {
            return "cuenta del paciente " + fila.get("paciente_id");
        }
        return "cuenta de " + fila.get("correo");
    }

    // Los datos se guardan con SQL, con las columnas que tienen las tablas en la V7. Ojo: en medicos y pacientes,
    // la columna «estado» es parte de la dirección. El estado de la cuenta está en usuarios.estado.
    private static void medicoConCuenta(long id, Boolean fichaActiva, String estadoDeLaCuenta) {
        jdbc.update("""
                INSERT INTO medicos (id, nombre, email, documento, telefono, especialidad,
                                     calle, barrio, ciudad, estado, codigo_postal, activo)
                VALUES (?, ?, ?, ?, '912345678', 'CARDIOLOGIA', 'Calle 1', 'Centro', 'Lima', 'Lima', '15001', ?)
                """, id, "Médico " + id, "medico" + id + "@ejemplo.com", "1000000" + id, fichaActiva);
        jdbc.update("""
                INSERT INTO usuarios (correo, rol, contrasenia, medico_id, estado)
                VALUES (?, 'MEDICO', 'sin-uso', ?, ?)
                """, "medico" + id + "@ejemplo.com", id, estadoDeLaCuenta);
    }

    private static void pacienteConCuenta(long id, boolean fichaActiva, String estadoDeLaCuenta) {
        guardarPaciente(id, fichaActiva);
        jdbc.update("""
                INSERT INTO usuarios (correo, rol, contrasenia, paciente_id, estado)
                VALUES (?, 'PACIENTE', 'sin-uso', ?, ?)
                """, "paciente" + id + "@ejemplo.com", id, estadoDeLaCuenta);
    }

    private static void pacienteSinCuenta(long id, boolean fichaActiva) {
        guardarPaciente(id, fichaActiva);
    }

    private static void guardarPaciente(long id, boolean fichaActiva) {
        jdbc.update("""
                INSERT INTO pacientes (id, nombre, email, documento,
                                       calle, barrio, ciudad, estado, codigo_postal, activo)
                VALUES (?, ?, ?, ?, 'Calle 1', 'Centro', 'Lima', 'Lima', '15001', ?)
                """, id, "Paciente " + id, "paciente" + id + "@ejemplo.com", "2000000" + id, fichaActiva);
    }

    private static void cuentaSinFicha(String correo, String rol, String estado) {
        jdbc.update("INSERT INTO usuarios (correo, rol, contrasenia, estado) VALUES (?, ?, 'sin-uso', ?)",
                correo, rol, estado);
    }
}
