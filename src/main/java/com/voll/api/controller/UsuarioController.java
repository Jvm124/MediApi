package com.voll.api.controller;

import com.voll.api.domain.ValidacionException;
import com.voll.api.domain.usuario.DatosRegistroUsuario;
import com.voll.api.domain.usuario.Rol;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.domain.usuario.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping
    @Transactional
    public ResponseEntity registrar(@RequestBody @Valid DatosRegistroUsuario datos) {
        // Validar que el correo no exista ya
        if (usuarioRepository.existsByCorreo(datos.correo())) {
            throw new ValidacionException("Ya existe un usuario con ese correo");
        }
        var contraseniaEncriptada = passwordEncoder.encode(datos.contrasenia());
        // El registro público SIEMPRE crea un PACIENTE
        var usuario = new Usuario(datos.correo(), contraseniaEncriptada, Rol.PACIENTE);
        usuarioRepository.save(usuario);
        return ResponseEntity.ok("Usuario creado: " + usuario.getCorreo());
    }
}