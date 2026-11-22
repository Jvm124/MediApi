import { useEffect, useState } from 'react';

const CADA_CUANTO_MS = 15_000;

// La hora del navegador, en milisegundos (como Date.now()), puesta al día cada 15 segundos. Así una pantalla nota
// sola que llegó una hora, como la de una consulta, sin recargar la página.
export const useHoraActual = () => {
    const [ahora, setAhora] = useState(() => Date.now());

    useEffect(() => {
        const ponerAlDia = () => setAhora(Date.now());
        const temporizador = window.setInterval(ponerAlDia, CADA_CUANTO_MS);
        // Con la pestaña en segundo plano, el navegador espacia los temporizadores; al volver a ella, se pone al día.
        document.addEventListener('visibilitychange', ponerAlDia);
        return () => {
            window.clearInterval(temporizador);
            document.removeEventListener('visibilitychange', ponerAlDia);
        };
    }, []);

    return ahora;
};
