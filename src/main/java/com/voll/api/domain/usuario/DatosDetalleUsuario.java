package com.voll.api.domain.usuario;

public record DatosDetalleUsuario(
        Long id,
        String correo,
        Rol rol
) {
    public DatosDetalleUsuario(Usuario usuario) {
        this(usuario.getId(), usuario.getCorreo(), usuario.getRol());
    }
}