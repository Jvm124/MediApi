package com.voll.api.domain.usuario;

public record DatosListaUsuario(
        Long id,
        String correo,
        Rol rol,
        EstadoUsuario estado
) {
    public DatosListaUsuario(Usuario usuario) {
        this(usuario.getId(), usuario.getCorreo(), usuario.getRol(), usuario.getEstado());
    }
}