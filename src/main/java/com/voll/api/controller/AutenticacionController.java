package com.voll.api.controller;

import com.voll.api.domain.usuario.DatosAutenticacion;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.infra.security.DatosTokenJWT;
import com.voll.api.infra.security.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class AutenticacionController {
    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthenticationManager manager;

    @PostMapping
    public ResponseEntity iniciarSesion(@RequestBody @Valid DatosAutenticacion datos){
        var authenticationtoken = new UsernamePasswordAuthenticationToken(datos.correo(), datos.contrasenia());
        var autenticacion = manager.authenticate(authenticationtoken);

        var usuario = (Usuario) autenticacion.getPrincipal();
        var tokenJWT = tokenService.generateToken(usuario);

        return ResponseEntity.ok(new DatosTokenJWT(tokenJWT, usuario.getRol().name()));
    }
}
