import type { MedicoAppointment } from '@/types/appointment.types';
import { formatFecha } from '@/utils/formatFecha';
import Card from '@/components/ui/Card';
import StatusBadge from '@/components/ui/StatusBadge';

interface Props {
    appointment: MedicoAppointment;
    // La hora del navegador, en milisegundos, como Date.now().
    ahora: number;
    onAtender: (id: number) => void;
    atendiendo: boolean;
}

const MedicoAppointmentCard = ({ appointment, ahora, onAtender, atendiendo }: Props) => {
    const { fecha, hora } = formatFecha(appointment.fecha);
    // Como en el backend (BUG-10), solo se marca como atendida desde su hora. La fecha llega sin zona horaria y el
    // navegador la lee en su hora local, igual que formatFecha. Si su reloj va adelantado, el backend la rechaza.
    const yaEsLaHora = new Date(appointment.fecha).getTime() <= ahora;

    return (
        <Card className="p-4 flex items-center justify-between gap-4">
            <div>
                <h3 className="font-semibold text-gray-900">{appointment.pacienteNombre}</h3>
                <p className="text-sm text-gray-500">{fecha} · {hora}</p>
            </div>

            {appointment.estado === 'PROGRAMADA' ? (
                <div className="flex flex-col items-end gap-1">
                    <button
                        onClick={() => onAtender(appointment.id)}
                        disabled={atendiendo || !yaEsLaHora}
                        className="bg-primary text-white text-sm font-medium px-4 py-2 rounded-lg enabled:hover:bg-primary-dark disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                    >
                        {atendiendo ? 'Guardando…' : 'Marcar atendida'}
                    </button>
                    {!yaEsLaHora && (
                        <span className="text-xs text-gray-500 text-right text-balance">Desde la hora de la consulta</span>
                    )}
                </div>
            ) : (
                <StatusBadge estado={appointment.estado} />
            )}
        </Card>
    );
};

export default MedicoAppointmentCard;
