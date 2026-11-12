package com.voll.api.controller;

import com.voll.api.domain.usuario.AutenticacionService;
import com.voll.api.domain.usuario.DatosAutenticacion;
import com.voll.api.domain.usuario.Usuario;
import com.voll.api.infra.security.DatosTokenJWT;
import com.voll.api.infra.security.LimiteDeIntentosDeLogin;
import com.voll.api.infra.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class AutenticacionController {
    private final TokenService tokenService;
    private final AuthenticationManager manager;
    private final AutenticacionService autenticacionService;
    private final LimiteDeIntentosDeLogin limiteDeIntentos;

    @PostMapping
    public ResponseEntity iniciarSesion(@RequestBody @Valid DatosAutenticacion datos, HttpServletRequest request){
        // Límite de intentos (SEC-04): los fallos se cuentan con el correo tal como está guardado y con la IP.
        var correo = autenticacionService.correoGuardado(datos.correo());
        var ip = request.getRemoteAddr();
        // Con demasiados fallos recientes responde 429 sin mirar la contraseña, aunque sea la correcta.
        limiteDeIntentos.comprobar(correo, ip);

        var authenticationtoken = new UsernamePasswordAuthenticationToken(datos.correo(), datos.contrasenia());
        Authentication autenticacion;
        try {
            autenticacion = manager.authenticate(authenticationtoken);
        } catch (AuthenticationServiceException e) {
            // Falló el servidor (por ejemplo, la base de datos), no el usuario: no cuenta como intento.
            throw e;
        } catch (AuthenticationException e) {
            // Correo o contraseña incorrectos, o cuenta suspendida.
            limiteDeIntentos.registrarFallo(correo, ip);
            throw e;
        }
        limiteDeIntentos.registrarAcierto(correo);

        var usuario = (Usuario) autenticacion.getPrincipal();
        var tokenJWT = tokenService.generateToken(usuario);

        return ResponseEntity.ok(new DatosTokenJWT(tokenJWT, usuario.getRol().name()));
    }
}
