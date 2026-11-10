export type Especialidad = 'ORTOPEDIA' | 'CARDIOLOGIA' | 'GINECOLOGIA' | 'DERMATOLOGIA';

export interface Direccion {
    calle: string;
    numero?: string;
    complemento?: string;
    barrio: string;
    ciudad: string;
    codigo_postal: string;
    estado: string;
}

export interface MedicoLista {
    id: number;
    nombre: string;
    email: string;
    documento: string;
    especialidad: Especialidad;
}

export interface MedicoDetalle {
    id: number;
    nombre: string;
    email: string;
    telefono: string;
    documento: string;
    especialidad: Especialidad;
    direccion: Direccion;
}

export interface NuevoMedicoRequest {
    nombre: string;
    email: string;
    telefono: string;
    documento: string;
    especialidad: Especialidad;
    contrasenia: string;
    direccion: Direccion;
}

export interface ActualizarMedicoRequest {
    nombre?: string;
    telefono?: string;
    especialidad?: Especialidad;
    direccion?: Direccion;
}

export interface ActualizarPerfilRequest {
    telefono?: string;
    direccion?: Direccion;
}
