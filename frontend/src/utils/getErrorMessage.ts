import axios from 'axios';

// El backend responde los errores con el formato ProblemDetail (RFC 9457), por ejemplo:
// { "status": 400, "title": "Bad Request", "detail": "Ya existe un paciente con ese email" }
// Se muestra "detail". Si no viene (por ejemplo, un 401 sin cuerpo), se usa el mensaje por defecto.
export const getErrorMessage = (err: unknown, fallback: string): string => {
    if (axios.isAxiosError(err)) {
        const detail = err.response?.data?.detail;
        if (typeof detail === 'string' && detail.trim() !== '') return detail;
    }
    return fallback;
};
