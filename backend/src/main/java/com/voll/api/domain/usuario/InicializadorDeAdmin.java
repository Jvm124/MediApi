package com.voll.api.domain.usuario;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * Crea el administrador inicial al arrancar, con las variables de entorno ADMIN_EMAIL y ADMIN_PASSWORD.
 * Solo actúa si todavía no existe ningún ADMINISTRADOR, así que en los arranques siguientes no hace nada.
 * Ver docs/adr/0002-admin-inicial-por-variables-de-entorno.md.
 */
@Slf4j
@Component
public class InicializadorDeAdmin implements ApplicationRunner {

    static final int LONGITUD_MINIMA = 8;
    static final int BYTES_MAXIMOS_BCRYPT = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String correo;
    private final String contrasenia;

    public InicializadorDeAdmin(UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${api.admin.correo}") String correo,
                                @Value("${api.admin.contrasenia}") String contrasenia) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo == null ? null : correo.strip();
        this.contrasenia = contrasenia;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByRol(Rol.ADMINISTRADOR)) {
            return;
        }
        if (estaVacio(correo) || estaVacio(contrasenia)) {
            log.warn("No hay ningún usuario ADMINISTRADOR. Define ADMIN_EMAIL y ADMIN_PASSWORD para crear el admin inicial.");
            return;
        }
        validarContrasenia(contrasenia);
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL (" + correo + ") ya pertenece a otro usuario. Usa otro correo para el admin inicial.");
        }

        usuarioRepository.save(new Usuario(correo, passwordEncoder.encode(contrasenia), Rol.ADMINISTRADOR));
        log.info("Se creó el administrador inicial: {}", correo);
    }

    private static boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static void validarContrasenia(String contrasenia) {
        if (contrasenia.length() < LONGITUD_MINIMA) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
        }
        // BCrypt solo usa los primeros 72 bytes, y Spring Security rechaza contraseñas más largas.
        if (contrasenia.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS_BCRYPT) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD no puede superar los " + BYTES_MAXIMOS_BCRYPT + " bytes (límite de BCrypt).");
        }
    }
}
