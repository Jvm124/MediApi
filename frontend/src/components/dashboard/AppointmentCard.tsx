import type { Appointment } from '@/types/appointment.types';
import { formatFecha } from '@/utils/formatFecha';
import Card from '@/components/ui/Card';
import StatusBadge from '@/components/ui/StatusBadge';

interface Props {
    appointment: Appointment;
}

const AppointmentCard = ({ appointment }: Props) => {
    const { fecha, hora } = formatFecha(appointment.fecha);

    return (
        <Card className="p-4 flex items-center justify-between gap-4">
            <div>
                <h3 className="font-semibold text-gray-900">{appointment.medicoNombre}</h3>
                <p className="text-sm text-gray-500">
                    {appointment.especialidad} · {fecha} · {hora}
                </p>
            </div>
            <StatusBadge estado={appointment.estado} />
        </Card>
    );
};

export default AppointmentCard;
