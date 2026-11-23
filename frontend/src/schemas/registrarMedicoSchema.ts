import { z } from 'zod';
import { contraseniaSegura } from '@/schemas/contraseniaSchema';

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
    // Como el rol en el alta de usuarios: el select da un texto, y '' es «sin elegir». Al validar, ese texto tiene
    // que ser una de las especialidades.
    especialidad: z.string().pipe(
        z.enum(['ORTOPEDIA', 'CARDIOLOGIA', 'GINECOLOGIA', 'DERMATOLOGIA'], { error: 'Selecciona especialidad' })
    ),
    contrasenia: contraseniaSegura,
    direccion: direccionSchema,
});

// Lo que tiene el formulario mientras se llena (la especialidad es un texto) y lo que sale al validarlo.
export type RegistrarMedicoFormInput = z.input<typeof registrarMedicoSchema>;
export type RegistrarMedicoFormData = z.output<typeof registrarMedicoSchema>;
