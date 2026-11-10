package com.voll.api.domain.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionService implements UserDetailsService{
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByCorreo(username);
        if (usuario == null) {
            // El contrato de Spring Security pide lanzar esta excepción, nunca devolver null.
            // Spring la convierte en "credenciales inválidas", igual que una contraseña incorrecta.
            throw new UsernameNotFoundException("No existe un usuario con ese correo");
        }
        return usuario;
    }
}
