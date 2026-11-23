export type EstadoConsulta = 'PROGRAMADA' | 'CANCELADA' | 'ATENDIDA';

// Por qué se cancela una consulta: MotivoCancelamiento del backend.
export type MotivoCancelamiento = 'PACIENTE_DESISTIO' | 'MEDICO_CANCELO' | 'OTROS';

// Vista del PACIENTE: le importa quién es su médico.
export interface Appointment {
    id: number;
    idMedico: number;
    medicoNombre: string;
    especialidad: string;
    idPaciente: number;
    fecha: string;                 // ISO LocalDateTime, "2026-08-10T10:00:00"
    estado: EstadoConsulta;
}

// Vista del MÉDICO: le importa a qué paciente atiende.
// Ajusta los nombres de campo a los de tu DTO real (DatosDetalleConsultaMedico).
export interface MedicoAppointment {
    id: number;
    idPaciente: number;
    pacienteNombre: string;
    idMedico: number;
    fecha: string;
    estado: EstadoConsulta;
}

export interface NewAppointmentRequest {
    idMedico: number;
    fecha: string;
    // Solo cuando reserva recepción. El paciente no lo manda: el backend usa su propia ficha.
    idPaciente?: number;
}

export interface NewsItem {
    id: number;
    titulo: string;
    imagen: string;
    descripcion?: string;
    url?: string;
}
