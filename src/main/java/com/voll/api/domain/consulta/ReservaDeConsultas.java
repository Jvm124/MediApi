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

    public DatosDetalleConsulta reservar(DatosReservaConsulta datos, Usuario usuarioLogueado) {

        // 1. Determinar de quién es la consulta segun quien la reserva
        var paciente = resolverPaciente(datos, usuarioLogueado);

        if (datos.idMedico() != null && !medicoRepository.existsById(datos.idMedico())) {
            throw new ValidacionException("No existe un médico con el id informado");
        }

        // 2. Validaciones de negocio (horario, anticipación, etc.)
        validadores.forEach(v -> v.validar(datos));

        var medico = elegirMedico(datos);
        if (medico == null) {
            throw new ValidacionException("No existe un médico disponible en ese horario");
        }

        var consulta = new Consulta(medico, paciente, datos.fecha());
        consultaRepository.save(consulta);
        return new DatosDetalleConsulta(consulta);
    }
    private Paciente resolverPaciente(DatosReservaConsulta datos, Usuario usuarioLogueado) {
        // Si quien reserva es un PACIENTE, se ignora el idPaciente del body:
        // solo puede reservar para sí mismo, y su paciente sale del token.
        if (usuarioLogueado.getRol() == Rol.PACIENTE) {
            var paciente = usuarioLogueado.getPaciente();
            if (paciente == null) {
                throw new ValidacionException("Tu cuenta no tiene un registro de paciente asociado");
            }
            return paciente;
        }

        // Recepcionista/Admin sí pueden reservar a nombre de otro paciente
        if (datos.idPaciente() == null) {
            throw new ValidacionException("Es necesario informar el id del paciente");
        }
        return pacienteRepository.findById(datos.idPaciente())
                .orElseThrow(() -> new ValidacionException("No existe un paciente con el id informado"));
    }
    private Medico elegirMedico(DatosReservaConsulta datos) {
        if(datos.idMedico() != null){
            return medicoRepository.getReferenceById(datos.idMedico());
        }
        if(datos.especialidad() == null){
            throw new ValidacionException("Es necesario elegir una especialidad cuando no se elige un médico");
        }

        return medicoRepository.elegirMedicoAleatorioDisponibleEnLaFecha(datos.especialidad(), datos.fecha());
    }

    @Transactional
    public void cancelar(Long idConsulta, MotivoCancelamiento motivo) {
        var consulta = consultaRepository.findById(idConsulta)
                .orElseThrow(() -> new ValidacionException("No existe una consulta con el id informado"));
        validadorCancelamiento.forEach(v-> v.validar(consulta));
        consulta.cancelar(motivo);
    }


}
