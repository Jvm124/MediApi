import axiosClient from './axiosClient';
import type {
    Appointment,
    MedicoAppointment,
    MotivoCancelamiento,
    NewAppointmentRequest,
} from '@/types/appointment.types';

export const appointmentsApi = {
    // --- Paciente ---
    getMyAppointments: async (): Promise<Appointment[]> => {
        const { data } = await axiosClient.get<Appointment[]>('/consultas/me');
        return data;
    },

    create: async (payload: NewAppointmentRequest): Promise<Appointment> => {
        const { data } = await axiosClient.post<Appointment>('/consultas', payload);
        return data;
    },

    // --- Médico ---
    getMedicoAppointments: async (): Promise<MedicoAppointment[]> => {
        const { data } = await axiosClient.get<MedicoAppointment[]>('/consultas/medico');
        return data;
    },

    // No dependemos del cuerpo de respuesta: tras atender, la página re-consulta.
    atender: async (id: number): Promise<void> => {
        await axiosClient.post(`/consultas/${id}/atender`, {});
    },

    // --- Recepción ---
    // Las consultas programadas de un paciente, de la más próxima a la más lejana (BUG-04).
    getConsultasDePaciente: async (idPaciente: number): Promise<Appointment[]> => {
        const { data } = await axiosClient.get<Appointment[]>(`/consultas/paciente/${idPaciente}`);
        return data;
    },

    // Cancelar con su motivo. Es un DELETE con cuerpo: axios lo manda en «data». El backend revisa quién puede
    // cancelar cada consulta y que falten al menos 24 horas.
    cancelar: async (id: number, motivo: MotivoCancelamiento): Promise<void> => {
        await axiosClient.delete(`/consultas/${id}`, { data: { motivo } });
    },
};
