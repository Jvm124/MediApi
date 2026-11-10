package com.voll.api.domain.medico;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicoService {

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public DatosDetalleMedico registrar(DatosRegistroMedico datos) {
        if (usuarioRepository.existsByCorreo(datos.email())) {
            throw new ValidacionException("Ya existe un usuario con ese correo de acceso");
        }
        if (medicoRepository.existsByDocumento(datos.documento())) {
            throw new ValidacionException("Ya existe un medico con ese documento");
        }

        var medico = new Medico(datos);
        medicoRepository.save(medico);

        var usuario = new Usuario(
                datos.email(),
                passwordEncoder.encode(datos.contrasenia()),
                Rol.MEDICO
        );
        usuario.asignarMedico(medico);
        usuarioRepository.save(usuario);

        return new DatosDetalleMedico(medico);
    }

    public Page<DatosListaMedico> listar(Pageable paginacion) {
        return medicoRepository.findAllByActivoTrue(paginacion).map(DatosListaMedico::new);
    }

    public DatosDetalleMedico buscarPorId(Long id) {
        return new DatosDetalleMedico(buscarActivo(id));
    }

    public DatosDetalleMedico miPerfil(Usuario usuarioLogueado) {
        return new DatosDetalleMedico(buscarActivo(idMedicoDelToken(usuarioLogueado)));
    }

    private Medico buscarActivo(Long id) {
        var medico = medicoRepository.findById(id)
                .orElseThrow(() -> new ValidacionException("No existe un medico con el id informado"));
        if (!medico.getActivo()) {
            throw new ValidacionException("El medico está dado de baja");
        }
        return medico;
    }

    @Transactional
    public DatosDetalleMedico actualizar(Long id, DatosActualizarMedico datos) {
        var medico = buscarActivo(id);
        medico.actualizarInformacionesMedico(datos);
        return new DatosDetalleMedico(medico);
    }

    @Transactional
    public DatosDetalleMedico actualizarPerfil(DatosActualizarPerfilMedico datos, Usuario usuarioLogueado) {
        var medico = buscarActivo(idMedicoDelToken(usuarioLogueado));
        medico.actualizarPerfil(datos);
        return new DatosDetalleMedico(medico);
    }

    private Long idMedicoDelToken(Usuario usuario) {
        if (usuario.getMedico() == null) {
            throw new ValidacionException("El usuario autenticado no está vinculado a un medico");
        }
        return usuario.getMedico().getId();
    }

    @Transactional
    public void eliminar(Long id) {
        var medico = buscarActivo(id);
        medico.eliminacionLogicaMedico();

        usuarioRepository.findByMedicoId(id).ifPresent(Usuario::darDeBaja);
        //misma funcionalidad:  usuarioRepository.findByMedicoId(id).ifPresent(usuario -> usuario.darDeBaja());
    }

    // Aqui listo los Medicos Disponibles, esto se usa cuando el paciente esta haciendo su reserva,
    // mostrandole todos los medicos disponibles
    public List<DatosMedicoSelect> listarDisponibles() {
        return medicoRepository.findByActivoTrue()
                .stream()
                .map(DatosMedicoSelect::new)
                .toList();
    }
}
