import { useCallback, useEffect, useState } from 'react';
import { CalendarDays, UserCog } from 'lucide-react';
import DashboardLayout, { type NavItem } from '@/components/dashboard/DashboardLayout';
import MedicoAppointmentCard from '@/components/dashboard/MedicoAppointmentCard';
import Card from '@/components/ui/Card';
import Spinner from '@/components/ui/Spinner';
import { appointmentsApi } from '@/api/appointmentsApi';
import { useHoraActual } from '@/hooks/useHoraActual';
import { getErrorMessage } from '@/utils/getErrorMessage';
import type { MedicoAppointment } from '@/types/appointment.types';

const nav: NavItem[] = [
    { label: 'Mi agenda', to: '/medico', icon: CalendarDays },
    { label: 'Mi perfil', to: '/medico/perfil', icon: UserCog },
];

const MedicoDashboardPage = () => {
    const [citas, setCitas] = useState<MedicoAppointment[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    // Si falla «Marcar atendida», el aviso sale encima de la agenda, sin ocultarla como el error de carga.
    const [errorAlAtender, setErrorAlAtender] = useState('');
    const [atendiendoId, setAtendiendoId] = useState<number | null>(null);
    // Cada tarjeta mira esta hora para saber si ya llegó la de su consulta.
    const ahora = useHoraActual();

    const cargar = useCallback(async () => {
        setError('');
        try {
            setCitas(await appointmentsApi.getMedicoAppointments());
        } catch (err) {
            setError(getErrorMessage(err, 'No se pudieron cargar tus consultas'));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        cargar();
    }, [cargar]);

    const atender = async (id: number) => {
        setAtendiendoId(id);
        setErrorAlAtender('');
        try {
            await appointmentsApi.atender(id);
            await cargar();
        } catch (err) {
            setErrorAlAtender(getErrorMessage(err, 'No se pudo marcar como atendida'));
        } finally {
            setAtendiendoId(null);
        }
    };

    return (
        <DashboardLayout title="Mi agenda" navItems={nav}>
            <h2 className="text-lg font-semibold mb-4">Tus consultas</h2>

            {errorAlAtender && (
                <p className="text-red-500 text-sm mb-4" role="alert">{errorAlAtender}</p>
            )}

            {loading ? (
                <Spinner />
            ) : error ? (
                <p className="text-red-500 text-sm">{error}</p>
            ) : citas.length === 0 ? (
                <Card className="p-6 text-center text-gray-500 text-sm">No tienes consultas.</Card>
            ) : (
                <div className="space-y-3">
                    {citas.map((c) => (
                        <MedicoAppointmentCard
                            key={c.id}
                            appointment={c}
                            ahora={ahora}
                            onAtender={atender}
                            atendiendo={atendiendoId === c.id}
                        />
                    ))}
                </div>
            )}
        </DashboardLayout>
    );
};

export default MedicoDashboardPage;
