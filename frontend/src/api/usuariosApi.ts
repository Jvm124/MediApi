import axiosClient from './axiosClient';
import type { UsuarioLista, NuevoUsuarioRequest, Page } from '@/types/usuario.types';

export const usuariosApi = {
    listar: async (pagina = 0, size = 10): Promise<Page<UsuarioLista>> => {
        const { data } = await axiosClient.get<Page<UsuarioLista>>('/usuarios', {
            params: { page: pagina, size, sort: 'correo' },
        });
        return data;
    },

    registrar: async (payload: NuevoUsuarioRequest): Promise<void> => {
        await axiosClient.post('/usuarios', payload);
    },


    suspender: async (id: number): Promise<void> => {
        await axiosClient.patch(`/usuarios/${id}/suspender`);
    },


    reactivar: async (id: number): Promise<void> => {
        await axiosClient.patch(`/usuarios/${id}/reactivar`);
    },

    darDeBaja: async (id: number): Promise<void> => {
        await axiosClient.patch(`/usuarios/${id}/baja`);
    },
};
