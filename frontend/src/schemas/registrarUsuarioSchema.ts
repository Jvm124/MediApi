import { z } from 'zod';
import { contraseniaSegura } from '@/schemas/contraseniaSchema';

// El backend solo permite crear staff por este endpoint (rechaza MEDICO/PACIENTE),
// así que el formulario solo ofrece esos dos roles.
export const registrarUsuarioSchema = z.object({
    correo: z.email('Correo inválido'),
    contrasenia: contraseniaSegura,
    // El select da un texto, y '' es «sin elegir»: así el formulario puede empezar vacío y volver a quedar vacío
    // después de un alta. Al validar, ese texto tiene que ser uno de los dos roles.
    rol: z.string().pipe(z.enum(['RECEPCIONISTA', 'ADMINISTRADOR'], { error: 'Selecciona un rol' })),
});

// Lo que tiene el formulario mientras se llena (el rol es un texto) y lo que sale al validarlo (uno de los roles).
export type RegistrarUsuarioFormInput = z.input<typeof registrarUsuarioSchema>;
export type RegistrarUsuarioFormData = z.output<typeof registrarUsuarioSchema>;
