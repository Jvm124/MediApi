package com.voll.api.domain.paciente;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.usuario.EstadoUsuario;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // 1) Auto-registro web: crea Paciente + Usuario (cuenta de acceso).
    @Transactional
    public DatosDetallePaciente registrar(DatosRegistroPaciente datos) {
        if (usuarioRepository.existsByCorreo(datos.email())) {
            throw new ValidacionException("Ya existe un usuario con ese correo de acceso");
        }
        if (pacienteRepository.existsByEmail(datos.email())) {
            throw new ValidacionException("Ya existe un paciente con ese email");
        }
        if (pacienteRepository.existsByDocumento(datos.documento())) {
            throw new ValidacionException("Ya existe un paciente con ese documento");
        }

        var paciente = new Paciente(datos);
        pacienteRepository.save(paciente);

        var usuario = new Usuario(
                datos.email(),
                passwordEncoder.encode(datos.contrasenia()),
                Rol.PACIENTE
        );
        usuario.asignarPaciente(paciente);
        usuarioRepository.save(usuario);

        return new DatosDetallePaciente(paciente);
    }

    // 2) Alta por mostrador: crea SOLO el Paciente, sin cuenta web.
    @Transactional
    public DatosDetallePaciente registrarPorStaff(DatosRegistroPacientePorStaff datos) {
        if (pacienteRepository.existsByDocumento(datos.documento())) {
            throw new ValidacionException("Ya existe un paciente con ese documento");
        }
        if (pacienteRepository.existsByEmail(datos.email())) {
            throw new ValidacionException("Ya existe un paciente con ese email");
        }

        var paciente = new Paciente(datos);
        pacienteRepository.save(paciente);
        return new DatosDetallePaciente(paciente);
    }

    public Page<DatosListaPaciente> listar(Pageable paginacion) {
        return pacienteRepository.findAllByActivoTrue(paginacion)
                .map(DatosListaPaciente::new);
    }

    public DatosDetallePaciente detallar(Long id) {
        return new DatosDetallePaciente(buscarActivo(id));
    }

    @Transactional
    public DatosDetallePaciente actualizar(DatosActualizarPaciente datos, Usuario usuarioLogueado) {
        var paciente = buscarActivo(idPacienteDelToken(usuarioLogueado));
        paciente.actualizarInformacionesPaciente(datos);
        return new DatosDetallePaciente(paciente);
    }

    @Transactional
    public void eliminar(Long id, Usuario usuarioLogueado) {
        if (usuarioLogueado.getRol() == Rol.PACIENTE
                && !idPacienteDelToken(usuarioLogueado).equals(id)) {
            throw new ValidacionException("No puedes dar de baja a otro paciente");
        }
        var paciente = buscarActivo(id);
        paciente.desactivar();

        // Si el paciente tiene cuenta, se da de baja junto con la ficha (BUG-05); si ya estaba de baja, no se toca.
        // Se usa la cuenta que devuelve el repositorio: la de la sesión la cargó el filtro de seguridad, fuera de
        // esta transacción, y sus cambios no se guardarían.
        usuarioRepository.findByPacienteId(id)
                .filter(usuario -> usuario.getEstado() != EstadoUsuario.BAJA)
                .ifPresent(Usuario::darDeBaja);
    }

    private Paciente buscarActivo(Long id) {
        var paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ValidacionException("No existe un paciente con el id informado"));

        if (!paciente.getActivo()) {
            throw new ValidacionException("El paciente está suspendido o dado de baja");
        }
        return paciente;
    }

    private Long idPacienteDelToken(Usuario usuario) {
        if (usuario.getPaciente() == null) {
            throw new ValidacionException("El usuario autenticado no está vinculado a un paciente");
        }
        return usuario.getPaciente().getId();
    }
}
