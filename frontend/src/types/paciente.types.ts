import type { DatosDireccion } from '@/types/auth.types';

// Coincide con DatosListaPaciente del backend (GET /pacientes).
export interface PacienteLista {
    id: number;
    nombre: string;
    email: string;
    telefono: string;
}

// Coincide con DatosDetallePaciente del backend (GET /pacientes/{id}), sin la dirección, que aquí no se usa.
export interface PacienteDetalle {
    id: number;
    nombre: string;
    email: string;
    telefono: string;
    documento: string;
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
