package com.voll.api.domain.usuario;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutenticacionService implements UserDetailsService{
    private final UsuarioRepository usuarioRepository;

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

    /**
     * El correo con el que está guardado el usuario que encuentra la base de datos, o el recibido si no encuentra a
     * nadie. MySQL compara los correos sin distinguir mayúsculas, tildes ni letras de ancho completo: con
     * «Ána@Ejemplo.com» se entra en la cuenta de «ana@ejemplo.com». El límite de intentos del login (SEC-04) cuenta
     * los fallos con este correo, para que esas variantes no den intentos de más.
     */
    public String correoGuardado(String correo) {
        var usuario = usuarioRepository.findByCorreo(correo);
        return usuario != null ? usuario.getUsername() : correo;
    }
}
