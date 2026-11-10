package com.voll.api.infra.security;

import com.voll.api.domain.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class SecurityFilter extends OncePerRequestFilter {
    private static final String PREFIJO_BEARER = "Bearer ";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        var tokenJWT = recuperarToken(request);
        if (tokenJWT != null) {
            autenticar(tokenJWT);
        }

        filterChain.doFilter(request, response);
    }

    // Si el token no sirve o el usuario ya no puede entrar, la petición sigue sin autenticar:
    // Spring Security responde 401 en las rutas protegidas y el frontend cierra la sesión.
    private void autenticar(String tokenJWT) {
        try {
            var usuario = usuarioRepository.findByCorreo(tokenService.getSubject(tokenJWT));
            if (usuario != null && usuario.isEnabled()) {
                var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (TokenInvalidoException e) {
            log.debug("Se ignora un token inválido: {}", e.getMessage());
        }
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith(PREFIJO_BEARER)) {
            return authorizationHeader.substring(PREFIJO_BEARER.length());
        }
        return null;
    }
}
