import type { DatosDireccion } from '@/types/auth.types';

// Coincide con DatosListaPaciente del backend (GET /pacientes).
export interface PacienteLista {
    id: number;
    nombre: string;
    email: string;
    telefono: string;
}

// Lo que enviamos a POST /pacientes/registro-staff (DatosRegistroPacientePorStaff): el alta por mostrador,
// que crea la ficha del paciente sin cuenta web.
export interface NuevoPacienteMostradorRequest {
    nombre: string;
    documento: string;
    telefono: string;
    email: string;
    direccion: DatosDireccion;
}
