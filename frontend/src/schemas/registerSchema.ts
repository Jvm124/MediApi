import { z } from 'zod';

export const registerSchema = z
    .object({
        nombreCompleto: z
            .string()
            .min(3, 'Mínimo 3 caracteres')
            .max(100, 'Máximo 100 caracteres')
            .regex(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/, 'Solo se permiten letras y espacios'),
        dni: z.string().regex(/^\d{8}$/, 'El DNI debe tener exactamente 8 dígitos'),
        telefono: z.string().regex(/^9\d{8}$/, 'Debe empezar con 9 y tener 9 dígitos'),
        email: z.string().email('Correo inválido').max(100),
        password: z
            .string()
            .min(8, 'Mínimo 8 caracteres')
            .max(64, 'Máximo 64 caracteres')
            .regex(/[A-Z]/, 'Debe contener al menos una mayúscula')
            .regex(/[a-z]/, 'Debe contener al menos una minúscula')
            .regex(/[0-9]/, 'Debe contener al menos un número')
            .regex(/[^A-Za-z0-9]/, 'Debe contener al menos un carácter especial'),
        confirmPassword: z.string(),
        direccion: z.object({
            calle: z.string().min(1, 'La calle es obligatoria'),
            numero: z.string().optional(),
            complemento: z.string().optional(),
            barrio: z.string().min(1, 'El barrio/distrito es obligatorio'),
            ciudad: z.string().min(1, 'La ciudad es obligatoria'),
            codigo_postal: z.string().regex(/^\d{5}$/, 'Código postal de 5 dígitos'),
            estado: z.string().min(1, 'El departamento es obligatorio'),
        }),
        acceptTerms: z.boolean().refine((v) => v === true, {
            message: 'Debes aceptar los términos y condiciones',
        }),
    })
    .refine((data) => data.password === data.confirmPassword, {
        message: 'Las contraseñas no coinciden',
        path: ['confirmPassword'],
    });

export type RegisterFormData = z.infer<typeof registerSchema>;