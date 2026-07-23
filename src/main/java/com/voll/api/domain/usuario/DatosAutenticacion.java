package com.voll.api.domain.usuario;

public record DatosAutenticacion(
        Long id,
        String login,
        String contrasenia
) {
}
