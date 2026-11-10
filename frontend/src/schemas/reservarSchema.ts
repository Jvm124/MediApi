import { z } from 'zod';

const HORA_APERTURA = 7;
const HORA_CIERRE = 18;
const MIN_ANTICIPACION_MS = 30 * 60 * 1000;

export const reservarSchema = z.object({
    idMedico: z
        .string({ error: 'Selecciona un médico' })
        .min(1, 'Selecciona un médico')
        .transform(Number),

    fecha: z
        .string()
        .min(1, 'Selecciona fecha y hora')
        .refine(
            (v) => new Date(v).getTime() > Date.now() + MIN_ANTICIPACION_MS,
            'La cita debe reservarse con al menos 30 minutos de anticipación',
        )
        .refine((v) => new Date(v).getDay() !== 0, 'La clínica no atiende los domingos')
        .refine((v) => {
            const hora = new Date(v).getHours();
            return hora >= HORA_APERTURA && hora < HORA_CIERRE;
        }, 'El horario de atención es de 7:00 a 18:00'),
});

export type ReservarFormInput = z.input<typeof reservarSchema>;
export type ReservarFormData = z.output<typeof reservarSchema>;
