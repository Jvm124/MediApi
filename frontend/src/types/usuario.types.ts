export type Rol = 'ADMINISTRADOR' | 'RECEPCIONISTA' | 'MEDICO' | 'PACIENTE';

export type EstadoUsuario = 'ACTIVO' | 'SUSPENDIDO' | 'BAJA';

// Coincide con DatosListaUsuario del backend
export interface UsuarioLista {
    id: number;
    correo: string;
    rol: Rol;
    estado: EstadoUsuario;
}

// Lo que enviamos a POST /usuarios (DatosRegistroUsuario)
export interface NuevoUsuarioRequest {
    correo: string;
    contrasenia: string;
    rol: Rol;
}

// Spring Data Page (solo los campos que usamos)
export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number; // página actual (base 0)
    size: number;
}
