export const formatFecha = (iso: string) => {
    const dt = new Date(iso);
    const fecha = new Intl.DateTimeFormat('es-PE', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
    }).format(dt);
    const hora = new Intl.DateTimeFormat('es-PE', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
    }).format(dt);
    return { fecha, hora };
};
