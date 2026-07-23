package com.voll.api.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

//@Configuration
public class CorsConfiguration {

    //@Bean
    public CorsFilter corsFilter() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:5173",   // Vite dev server
                "http://localhost:8080"    // Por si usas otro puerto
                // "https://tu-dominio-produccion.com" // Añadir cuando despliegues
        ));

        // ✅ Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // ✅ Headers permitidos
        config.setAllowedHeaders(List.of("*"));

        // ✅ Headers expuestos al cliente (necesario si lees el header Authorization en el front)
        config.setExposedHeaders(List.of("Authorization"));

        // ✅ Permite enviar cookies/credenciales (si en el futuro usas cookies HttpOnly)
        config.setAllowCredentials(true);

        // ✅ Tiempo de caché para preflight requests (1 hora)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}