package com.voll.api.domain.usuario;

import com.voll.api.domain.ValidacionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public DatosDetalleUsuario registrarUsuarios(DatosRegistroUsuario datos) {

        if (datos.rol() == Rol.MEDICO || datos.rol() == Rol.PACIENTE) {
            throw new ValidacionException("Acceso denegado");
        }
        if (usuarioRepository.existsByCorreo(datos.correo())) {
            throw new ValidacionException("Ya existe un usuario con ese correo");
        }
        var contraseniaEncriptada = passwordEncoder.encode(datos.contrasenia());
        var usuario = new Usuario(datos.correo(), contraseniaEncriptada, datos.rol());
        usuarioRepository.save(usuario);
        return new DatosDetalleUsuario(usuario);
    }
    @Transactional
    public void suspender(Long id, Usuario adminLogueado) {

        if (adminLogueado.getId().equals(id)) {
            throw new ValidacionException("No puedes suspender tu propia cuenta");
        }
        var usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ValidacionException("No existe un usuario con el id informado"));
        usuario.suspender();
    }
    @Transactional
    public void reactivar(Long id, Usuario adminLogueado) {

        if (adminLogueado.getId().equals(id)) {
            throw new ValidacionException("No puedes reactivar tu propia cuenta");
        }
        var usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ValidacionException("No existe un usuario con el id informado"));
        usuario.reactivar();
    }

    @Transactional
    public void darDeBaja(Long id, Usuario adminLogueado) {

        if (adminLogueado.getId().equals(id)) {
            throw new ValidacionException("No puedes dar de baja tu propia cuenta");
        }
        var usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ValidacionException("No existe un usuario con el id informado"));
        usuario.darDeBaja();
    }



    public Page<DatosListaUsuario> listarUsuarios(Pageable paginacion) {
        return usuarioRepository.findAll(paginacion).map(DatosListaUsuario::new);
    }
}
