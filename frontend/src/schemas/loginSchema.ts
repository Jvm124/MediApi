import { z } from 'zod';

export const loginSchema = z.object({
    correo: z
        .string()
        .min(1, 'El correo es obligatorio')
        .email('Correo inválido')
        .max(100, 'Máximo 100 caracteres'),
    contrasenia: z
        .string()
        .min(2, 'Mínimo 2 caracteres')
        .max(64, 'Máximo 64 caracteres'),
});

export type LoginFormData = z.infer<typeof loginSchema>;