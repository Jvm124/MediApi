package com.voll.api.domain.consulta;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.consulta.validaciones.ValidadorCancelamientoDeConsulta;
import com.voll.api.domain.consulta.validaciones.ValidadorDeConsultas;
import com.voll.api.domain.medico.Medico;
import com.voll.api.domain.medico.MedicoRepository;
import com.voll.api.domain.paciente.Paciente;
import com.voll.api.domain.paciente.PacienteRepository;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservaDeConsultas {

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private ConsultaRepository consultaRepository;

    @Autowired
    private List<ValidadorDeConsultas> validadores;

    @Autowired
    private List<ValidadorCancelamientoDeConsulta> validadorCancelamiento;

    @Transactional
    public DatosDetalleConsulta reservar(DatosReservaConsulta datos, Usuario usuarioLogueado) {

        var paciente = resolverPaciente(datos, usuarioLogueado);

        var datosNormalizados = new DatosReservaConsulta(
                datos.idMedico(),
                paciente.getId(),
                datos.fecha(),
                datos.especialidad()
        );

        if (datosNormalizados.idMedico() != null
                && !medicoRepository.existsById(datosNormalizados.idMedico())) {
            throw new ValidacionException("No existe un médico con el id informado");
        }

        validadores.forEach(v -> v.validar(datosNormalizados));

        var medico = elegirMedico(datosNormalizados);
        if (medico == null) {
            throw new ValidacionException("No existe un médico disponible en ese horario");
        }

        var consulta = new Consulta(medico, paciente, datosNormalizados.fecha());
        consultaRepository.save(consulta);
        return new DatosDetalleConsulta(consulta);
    }

    public List<DatosDetalleConsulta> listarConsultasDelPaciente(Usuario usuarioLogueado) {
        var paciente = usuarioLogueado.getPaciente();
        if (paciente == null) {
            throw new ValidacionException("Tu cuenta no tiene un registro de paciente asociado");
        }
        return consultaRepository
                .buscarProgramadasDelPaciente(paciente.getId(), EstadoConsulta.PROGRAMADA)
                .stream()
                .map(DatosDetalleConsulta::new)
                .toList();
    }

    public List<DatosDetalleConsultaMedico> listarConsultasDelMedico(Usuario usuarioLogueado) {
        var medico = usuarioLogueado.getMedico();
        if (medico == null) {
            throw new ValidacionException("Tu cuenta no tiene un registro de médico asociado");
        }
        return consultaRepository
                .buscarProgramadasDelMedico(medico.getId(), EstadoConsulta.PROGRAMADA)
                .stream()
                .map(DatosDetalleConsultaMedico::new)
                .toList();
    }

    @Transactional
    public DatosDetalleConsulta atender(Long idConsulta, Usuario usuarioLogueado) {
        var consulta = consultaRepository.findById(idConsulta)
                .orElseThrow(() -> new ValidacionException("No existe una consulta con el id informado"));

        // Solo el médico asignado puede dar por atendida SU consulta.
        var medico = usuarioLogueado.getMedico();
        if (medico == null || !consulta.getIdMedico().getId().equals(medico.getId())) {
            throw new ValidacionException("Solo puedes atender consultas asignadas a ti");
        }

        consulta.atender();
        return new DatosDetalleConsulta(consulta);
    }

    @Transactional
    public void cancelar(Long idConsulta, MotivoCancelamiento motivo, Usuario usuarioLogueado) {
        var consulta = consultaRepository.findById(idConsulta)
                .orElseThrow(() -> new ValidacionException("No existe una consulta con el id informado"));

        verificarPermisoDeCancelacion(consulta, usuarioLogueado);

        validadorCancelamiento.forEach(v -> v.validar(consulta));
        consulta.cancelar(motivo);
    }

    private void verificarPermisoDeCancelacion(Consulta consulta, Usuario usuario) {
        switch (usuario.getRol()) {
            case ADMINISTRADOR, RECEPCIONISTA -> {
                // pueden cancelar cualquier consulta
            }
            case PACIENTE -> {
                var paciente = usuario.getPaciente();
                if (paciente == null
                        || !consulta.getIdPaciente().getId().equals(paciente.getId())) {
                    throw new ValidacionException("No puedes cancelar una consulta que no es tuya");
                }
            }
            case MEDICO -> {
                var medico = usuario.getMedico();
                if (medico == null
                        || !consulta.getIdMedico().getId().equals(medico.getId())) {
                    throw new ValidacionException("No puedes cancelar una consulta que no es tuya");
                }
            }
        }
    }

    private Paciente resolverPaciente(DatosReservaConsulta datos, Usuario usuarioLogueado) {
        if (usuarioLogueado.getRol() == Rol.PACIENTE) {
            var paciente = usuarioLogueado.getPaciente();
            if (paciente == null) {
                throw new ValidacionException("Tu cuenta no tiene un registro de paciente asociado");
            }
            return paciente;
        }

        if (datos.idPaciente() == null) {
            throw new ValidacionException("Es necesario informar el id del paciente");
        }
        return pacienteRepository.findById(datos.idPaciente())
                .orElseThrow(() -> new ValidacionException("No existe un paciente con el id informado"));
    }

    private Medico elegirMedico(DatosReservaConsulta datos) {
        if (datos.idMedico() != null) {
            return medicoRepository.getReferenceById(datos.idMedico());
        }
        if (datos.especialidad() == null) {
            throw new ValidacionException("Es necesario elegir una especialidad cuando no se elige un médico");
        }
        return medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(datos.especialidad(), datos.fecha());
    }
}
