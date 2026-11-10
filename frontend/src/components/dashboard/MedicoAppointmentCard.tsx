import type { MedicoAppointment } from '@/types/appointment.types';
import { formatFecha } from '@/utils/formatFecha';
import Card from '@/components/ui/Card';
import StatusBadge from '@/components/ui/StatusBadge';

interface Props {
    appointment: MedicoAppointment;
    onAtender: (id: number) => void;
    atendiendo: boolean;
}

const MedicoAppointmentCard = ({ appointment, onAtender, atendiendo }: Props) => {
    const { fecha, hora } = formatFecha(appointment.fecha);

    return (
        <Card className="p-4 flex items-center justify-between gap-4">
            <div>
                <h3 className="font-semibold text-gray-900">{appointment.pacienteNombre}</h3>
                <p className="text-sm text-gray-500">{fecha} · {hora}</p>
            </div>

            {appointment.estado === 'PROGRAMADA' ? (
                <button
                    onClick={() => onAtender(appointment.id)}
                    disabled={atendiendo}
                    className="bg-primary text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-primary-dark disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap"
                >
                    {atendiendo ? 'Guardando…' : 'Marcar atendida'}
                </button>
            ) : (
                <StatusBadge estado={appointment.estado} />
            )}
        </Card>
    );
};

export default MedicoAppointmentCard;
