import axiosClient from './axiosClient';
import type { Page } from '@/types/usuario.types';
import type {
    MedicoLista,
    MedicoDetalle,
    NuevoMedicoRequest,
    ActualizarMedicoRequest,
    ActualizarPerfilRequest,
} from '@/types/medico.types';

export interface MedicoSelect {
    id: number;
    nombre: string;
    especialidad: string;
}

export const medicosApi = {
    listarDisponibles: async (): Promise<MedicoSelect[]> => {
        const { data } = await axiosClient.get<MedicoSelect[]>('/medicos/disponibles');
        return data;
    },

    listar: async (pagina = 0, size = 10): Promise<Page<MedicoLista>> => {
        const { data } = await axiosClient.get<Page<MedicoLista>>('/medicos', {
            params: { page: pagina, size, sort: 'nombre' },
        });
        return data;
    },

    detallar: async (id: number): Promise<MedicoDetalle> => {
        const { data } = await axiosClient.get<MedicoDetalle>(`/medicos/${id}`);
        return data;
    },

    miPerfil: async (): Promise<MedicoDetalle> => {
        const { data } = await axiosClient.get<MedicoDetalle>('/medicos/me');
        return data;
    },

    registrar: async (payload: NuevoMedicoRequest): Promise<MedicoDetalle> => {
        const { data } = await axiosClient.post<MedicoDetalle>('/medicos', payload);
        return data;
    },

    actualizar: async (id: number, payload: ActualizarMedicoRequest): Promise<MedicoDetalle> => {
        const { data } = await axiosClient.put<MedicoDetalle>(`/medicos/${id}`, payload);
        return data;
    },

    eliminar: async (id: number): Promise<void> => {
        await axiosClient.delete(`/medicos/${id}`);
    },

    actualizarPerfil: async (payload: ActualizarPerfilRequest): Promise<MedicoDetalle> => {
        const { data } = await axiosClient.put<MedicoDetalle>('/medicos/me', payload);
        return data;
    },
};
