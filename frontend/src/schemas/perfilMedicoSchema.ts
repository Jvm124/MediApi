import { z } from 'zod';

const direccionSchema = z.object({
    calle: z.string().min(1, 'Requerido'),
    numero: z.string().optional(),
    complemento: z.string().optional(),
    barrio: z.string().min(1, 'Requerido'),
    ciudad: z.string().min(1, 'Requerido'),
    codigo_postal: z.string().regex(/^\d{5}$/, '5 dígitos'),
    estado: z.string().min(1, 'Requerido'),
});

export const perfilMedicoSchema = z.object({
    telefono: z.string().regex(/^9\d{8}$/, 'Empieza en 9, 9 dígitos'),
    direccion: direccionSchema,
});

export type PerfilMedicoFormData = z.infer<typeof perfilMedicoSchema>;
