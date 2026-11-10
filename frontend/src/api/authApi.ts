import axiosClient from './axiosClient';
import type { LoginRequest, AuthResponse, User, DatosDireccion } from '@/types/auth.types';

export const authApi = {
    login: async (data: LoginRequest): Promise<AuthResponse> => {
        const response = await axiosClient.post<AuthResponse>('/login', data);
        return response.data;
    },
    registrarPaciente: async (data: {
        nombre: string;
        email: string;
        telefono: string;
        documento: string;
        direccion: DatosDireccion;
        contrasenia: string;
    }): Promise<void> => {
        await axiosClient.post('/pacientes', data);
    },


    getMe: async (): Promise<User> => {
        const { data } = await axiosClient.get<User>('/usuarios/me');
        return data;
    },
    logout: async (): Promise<void> => {
        return Promise.resolve();
    },
    reservar: async (data: {
        idMedico: number;
        idPaciente: number;
        fecha: Date;

    }): Promise<void> => {
        await axiosClient.post('/pacientes', data);
    },

};