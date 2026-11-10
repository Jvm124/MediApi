import { z } from 'zod';
import { contraseniaSegura } from '@/schemas/contraseniaSchema';

// El backend solo permite crear staff por este endpoint (rechaza MEDICO/PACIENTE),
// así que el formulario solo ofrece esos dos roles.
export const registrarUsuarioSchema = z.object({
    correo: z.email('Correo inválido'),
    contrasenia: contraseniaSegura,
    rol: z.enum(['RECEPCIONISTA', 'ADMINISTRADOR'], { error: 'Selecciona un rol' }),
});

export type RegistrarUsuarioFormData = z.infer<typeof registrarUsuarioSchema>;
