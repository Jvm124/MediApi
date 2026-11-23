import { z } from 'zod';

// Alta por mostrador (POST /pacientes/registro-staff). Las reglas y los mensajes son los del registro de
// pacientes (registerSchema), sin contraseña ni términos: este paciente no tiene cuenta web.
const direccionSchema = z.object({
    calle: z.string().min(1, 'La calle es obligatoria'),
    numero: z.string().optional(),
    complemento: z.string().optional(),
    barrio: z.string().min(1, 'El barrio/distrito es obligatorio'),
    ciudad: z.string().min(1, 'La ciudad es obligatoria'),
    codigo_postal: z.string().regex(/^\d{5}$/, 'Código postal de 5 dígitos'),
    estado: z.string().min(1, 'El departamento es obligatorio'),
});

export const altaMostradorSchema = z.object({
    nombre: z
        .string()
        .min(3, 'Mínimo 3 caracteres')
        .max(100, 'Máximo 100 caracteres')
        .regex(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/, 'Solo se permiten letras y espacios'),
    documento: z.string().regex(/^\d{8}$/, 'El DNI debe tener exactamente 8 dígitos'),
    telefono: z.string().regex(/^9\d{8}$/, 'Debe empezar con 9 y tener 9 dígitos'),
    // Obligatorio, como en el backend y en la tabla pacientes (V2: email NOT NULL UNIQUE). Ver el ADR 0007.
    email: z.email('Correo inválido').max(100, 'Máximo 100 caracteres'),
    direccion: direccionSchema,
});

export type AltaMostradorFormData = z.infer<typeof altaMostradorSchema>;
