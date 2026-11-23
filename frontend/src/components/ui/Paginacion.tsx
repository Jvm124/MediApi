interface Props {
    pagina: number; // la página que se ve, empezando en 0 (como la devuelve Spring)
    totalPaginas: number;
    onCambiar: (pagina: number) => void;
    deshabilitada?: boolean;
}

const boton =
    'px-3 py-1.5 rounded-md border border-primary text-primary font-medium hover:bg-blue-50 ' +
    'disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-transparent';

// «Anterior» y «Siguiente» para una lista que el backend devuelve por páginas (Page de Spring).
// Con una sola página no muestra nada.
const Paginacion = ({ pagina, totalPaginas, onCambiar, deshabilitada = false }: Props) => {
    if (totalPaginas <= 1) return null;

    return (
        <nav className="flex flex-wrap items-center justify-between gap-3 pt-4 text-sm" aria-label="Páginas">
            <button
                type="button"
                className={boton}
                onClick={() => onCambiar(pagina - 1)}
                disabled={deshabilitada || pagina === 0}
            >
                Anterior
            </button>
            <span className="text-gray-500">
                Página {pagina + 1} de {totalPaginas}
            </span>
            <button
                type="button"
                className={boton}
                onClick={() => onCambiar(pagina + 1)}
                disabled={deshabilitada || pagina >= totalPaginas - 1}
            >
                Siguiente
            </button>
        </nav>
    );
};

export default Paginacion;
