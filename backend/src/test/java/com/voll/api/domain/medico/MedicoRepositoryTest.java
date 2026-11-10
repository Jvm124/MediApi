//package com.voll.api.domain.medico;
//
//import jakarta.persistence.EntityManager;
//import com.voll.api.domain.consulta.Consulta;
//import com.voll.api.domain.direccion.DatosDireccion;
//import com.voll.api.domain.paciente.DatosRegistroPaciente;
//import com.voll.api.domain.paciente.Paciente;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
//import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.time.DayOfWeek;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.temporal.TemporalAdjusters;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@ActiveProfiles("test")
//class MedicoRepositoryTest {
//
//    @Autowired
//    private MedicoRepository medicoRepository;
//
//    @Autowired
//    private EntityManager em;
//
//    @Test
//    @DisplayName("Deberia devolver null cuando el medico buscado existe pero no esta disponible en esa fecha")
//    void elegirMedicoAleatorioDisponibleEnLaFechaEscenario1() {
//        //given o arrange
//        var lunesSiguienteALas10 = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(10,0);
//        var medico = registrarMedico("Medico1", "medico@gmail.com", "12345", Especialidad.CARDIOLOGIA,"123");
//        var paciente = registrarPaciente("Paciente1", "paciente@gmail.com", "123789");
//        registrarConsulta(medico, paciente, lunesSiguienteALas10);
//        //when o act
//        var medicoLibre = medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(Especialidad.CARDIOLOGIA, lunesSiguienteALas10);
//        //then o assert
//        assertThat(medicoLibre).isNull();
//    }
//
//    @Test
//    @DisplayName("Deberia devolver medico cuando el medico buscado esta disponible en esa fecha")
//    void elegirMedicoAleatorioDisponibleEnLaFechaEscenario2() {
//        //given o arrange
//        var lunesSiguienteALas10 = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(10,0);
//        var medico = registrarMedico("Medico1", "medico@gmail.com", "12345", Especialidad.CARDIOLOGIA,"123");
//        //when o act
//        var medicoLibre = medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(Especialidad.CARDIOLOGIA, lunesSiguienteALas10);
//        //then o assert
//        assertThat(medicoLibre).isEqualTo(medico);
//    }
//
//    private void registrarConsulta(Medico medico, Paciente paciente, LocalDateTime fecha) {
//        em.persist(new Consulta(medico, paciente, fecha));
//    }
//
//    private Medico registrarMedico(String nombre, String email, String documento, Especialidad especialidad,String contrasenia) {
//        var medico = new Medico(    datosMedico(nombre, email, documento, especialidad, contrasenia));
//        em.persist(medico);
//        return medico;
//    }
//
//    private Paciente registrarPaciente(String nombre, String email,String documento,String contrasenia ) {
//        var paciente = new Paciente(datosPaciente(nombre, email,documento, contrasenia));
//        em.persist(paciente);
//        return paciente;
//    }
//
//    private DatosRegistroMedico datosMedico(String nombre, String email, String telefono,String documento, Especialidad especialidad,String contrasenia) {
//        return new DatosRegistroMedico(
//                nombre,
//                email,
//                telefono,
//                documento,
//                especialidad,
//                datosDireccion(),// correoAcceso — reutilizo el email para el test
//                contrasenia   // contrasenia — cualquier valor sirve en el test
//        );
//    }
//
//    private DatosRegistroPaciente datosPaciente(String nombre, String email, String telefono, String documento, String contrasenia) {
//        return new DatosRegistroPaciente(
//                nombre,
//                email,
//                telefono,
//                documento,
//                datosDireccion(),
//                contrasenia
//        );
//    }
//
//    private DatosDireccion datosDireccion() {
//        return new DatosDireccion("calle x", "distrito y", "ciudad z", "123", "1","1531","Lima");
//    }
//}