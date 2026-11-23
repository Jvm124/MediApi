/**
 * Guarda el token JWT en sessionStorage, que se borra al cerrar la pestaña.
 *
 * El usuario y su rol no se guardan: se piden al servidor con GET /usuarios/me
 * al iniciar sesión y cada vez que se carga la página con un token guardado.
 */

const TOKEN_KEY = 'hc_token';

export const tokenStorage = {
    get: (): string | null => sessionStorage.getItem(TOKEN_KEY),
    set: (token: string): void => sessionStorage.setItem(TOKEN_KEY, token),
    remove: (): void => sessionStorage.removeItem(TOKEN_KEY),
};
