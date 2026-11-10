/**
 * Manejo del token JWT y del rol del usuario.
 *
 * Guardamos también el ROL porque tu backend, al recargar la página, no tiene
 * un endpoint /me que devuelva el usuario. Entonces, para no perder la sesión
 * al refrescar, recordamos el rol junto con el token y reconstruimos el usuario
 * mínimo desde aquí.
 *
 * Usamos sessionStorage (se borra al cerrar la pestaña).
 */

const TOKEN_KEY = 'hc_token';
const ROL_KEY = 'hc_rol';

export const tokenStorage = {
    get: (): string | null => sessionStorage.getItem(TOKEN_KEY),
    set: (token: string): void => sessionStorage.setItem(TOKEN_KEY, token),
    remove: (): void => {
        sessionStorage.removeItem(TOKEN_KEY);
        sessionStorage.removeItem(ROL_KEY);
    },

    // NUEVO: guardado y lectura del rol.
    getRol: (): string | null => sessionStorage.getItem(ROL_KEY),
    setRol: (rol: string): void => sessionStorage.setItem(ROL_KEY, rol),
};
