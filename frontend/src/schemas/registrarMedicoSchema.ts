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

export const registrarMedicoSchema = z.object({
    nombre: z.string().min(1, 'Requerido'),
    email: z.email('Correo inválido'),
    telefono: z.string().regex(/^9\d{8}$/, 'Empieza en 9, 9 dígitos'),
    documento: z.string().regex(/^\d{8}$/, '8 dígitos'),
    especialidad: z.enum(['ORTOPEDIA', 'CARDIOLOGIA', 'GINECOLOGIA', 'DERMATOLOGIA'], {
        error: 'Selecciona especialidad',
    }),
    contrasenia: z.string().min(6, 'Mínimo 6 caracteres'),
    direccion: direccionSchema,
});

export type RegistrarMedicoFormData = z.infer<typeof registrarMedicoSchema>;
