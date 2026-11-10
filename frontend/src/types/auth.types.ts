export interface User {
    id: number;
    nombreCompleto: string;
    correo: string;
    dni: string;
    telefono: string;
    rol: 'PACIENTE' | 'MEDICO' | 'ADMINISTRADOR';   // ← era 'ADMIN'
}

export interface LoginRequest {
    correo: string;
    contrasenia: string;
    recaptchaToken?: string; // ← opcional
}

export interface DatosDireccion {
    calle: string;
    numero?: string;
    complemento?: string;
    barrio: string;
    ciudad: string;
    codigo_postal: string;
    estado: string;
}

export interface RegisterRequest {
    nombreCompleto: string;
    dni: string;
    telefono: string;
    email: string;
    password: string;
    direccion: DatosDireccion;
}
export interface ReservarRequest {
    medico: number;
    paciente: number;
    fecha: Date;
}
// CAMBIO IMPORTANTE:
// Tu backend NO devuelve un objeto "user", devuelve el token y el rol como texto:
//   { "token": "...", "rol": "PACIENTE" }
// Así que la respuesta se declara con esa forma real.
//
// ⚠️ VERIFICA en el navegador (F12 → Network → clic en la petición "login"
//    → pestaña "Response") que los campos se llamen EXACTAMENTE "token" y "rol".
//    Si tu backend los nombra distinto (por ejemplo "tokenJWT"), cambia los
//    nombres aquí para que coincidan.
export interface AuthResponse {
    token: string;
    rol: User['rol'];
}
