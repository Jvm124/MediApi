import axiosClient from './axiosClient';
import type { Page } from '@/types/usuario.types';
import type { PacienteLista, NuevoPacienteMostradorRequest } from '@/types/paciente.types';

export const pacientesApi = {
    // Los pacientes activos, por nombre. La página empieza en 0, como en Spring.
    listar: async (pagina = 0, size = 10): Promise<Page<PacienteLista>> => {
        const { data } = await axiosClient.get<Page<PacienteLista>>('/pacientes', {
            params: { page: pagina, size, sort: 'nombre' },
        });
        return data;
    },

    // Alta por mostrador: crea la ficha del paciente sin cuenta web.
    registrarEnMostrador: async (payload: NuevoPacienteMostradorRequest): Promise<void> => {
        await axiosClient.post('/pacientes/registro-staff', payload);
    },
};
