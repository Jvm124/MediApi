import { z } from 'zod';

// La misma regla que @ContraseniaSegura en el backend
// (backend/src/main/java/com/voll/api/domain/usuario/ContraseniaSegura.java).
// BCrypt, que cifra las contraseñas en el backend, no admite más de 72 bytes. Se cuentan bytes y no
// letras porque una letra con tilde o una ñ ocupa 2.
const BYTES_MAXIMOS = 72;

export const MENSAJE_DEMASIADO_LARGA = 'Máximo 72 caracteres (las letras con tilde y la ñ cuentan como 2)';

export const cabeEnBcrypt = (contrasenia: string): boolean =>
    new TextEncoder().encode(contrasenia).length <= BYTES_MAXIMOS;

// Para las contraseñas nuevas: registro de pacientes y alta de médicos y personal.
// \p{Lu} es cualquier letra mayúscula (también Á o Ñ) y \p{Ll}, cualquier minúscula.
export const contraseniaSegura = z
    .string()
    .min(8, 'Mínimo 8 caracteres')
    .regex(/\p{Lu}/u, 'Debe contener al menos una mayúscula')
    .regex(/\p{Ll}/u, 'Debe contener al menos una minúscula')
    .regex(/\d/, 'Debe contener al menos un número')
    .refine(cabeEnBcrypt, { message: MENSAJE_DEMASIADO_LARGA });
