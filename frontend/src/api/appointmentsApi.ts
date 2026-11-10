import axiosClient from './axiosClient';
import type { Appointment, MedicoAppointment, NewAppointmentRequest } from '@/types/appointment.types';

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
};
