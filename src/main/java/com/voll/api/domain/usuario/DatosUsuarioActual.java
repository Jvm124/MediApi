package com.voll.api.domain.usuario;

public record DatosUsuarioActual(
        Long id,
        String nombreCompleto,
        String correo,
        String dni,
        String telefono,
        Rol rol
) {
    public static DatosUsuarioActual desde(Usuario usuario) {
        String nombre = "";
        String dni = "";
        String telefono = "";

        if (usuario.getMedico() != null) {
            var m = usuario.getMedico();
            nombre   = m.getNombre();
            dni      = m.getDocumento();
            telefono = m.getTelefono();
        } else if (usuario.getPaciente() != null) {
            var p = usuario.getPaciente();
            nombre   = p.getNombre();
            dni      = p.getDocumento();
            telefono = p.getTelefono();
        }

        return new DatosUsuarioActual(
                usuario.getId(), nombre, usuario.getCorreo(), dni, telefono, usuario.getRol()
        );
    }
}