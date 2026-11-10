import type { EstadoConsulta } from '@/types/appointment.types';

const estilos: Record<EstadoConsulta, string> = {
    PROGRAMADA: 'bg-primary/10 text-primary-dark',
    ATENDIDA: 'bg-green-100 text-green-700',
    CANCELADA: 'bg-red-100 text-red-700',
};

const etiquetas: Record<EstadoConsulta, string> = {
    PROGRAMADA: 'Programada',
    ATENDIDA: 'Atendida',
    CANCELADA: 'Cancelada',
};

interface Props {
    estado: EstadoConsulta;
}

const StatusBadge = ({ estado }: Props) => (
    <span className={`text-xs font-semibold px-2.5 py-1 rounded-full whitespace-nowrap ${estilos[estado]}`}>
        {etiquetas[estado]}
    </span>
);

export default StatusBadge;
