import { z } from 'zod';
import { cabeEnBcrypt, MENSAJE_DEMASIADO_LARGA } from '@/schemas/contraseniaSchema';

export const loginSchema = z.object({
    correo: z
        .string()
        .min(1, 'El correo es obligatorio')
        .email('Correo inválido')
        .max(100, 'Máximo 100 caracteres'),
    // El mismo máximo que al registrarse, para que cualquier contraseña válida pueda entrar.
    contrasenia: z
        .string()
        .min(2, 'Mínimo 2 caracteres')
        .refine(cabeEnBcrypt, { message: MENSAJE_DEMASIADO_LARGA }),
});

export type LoginFormData = z.infer<typeof loginSchema>;