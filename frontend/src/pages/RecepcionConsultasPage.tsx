import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Users } from 'lucide-react';
import DashboardLayout, { type NavItem } from '@/components/dashboard/DashboardLayout';
import Card from '@/components/ui/Card';
import Spinner from '@/components/ui/Spinner';
import { pacientesApi } from '@/api/pacientesApi';
import { appointmentsApi } from '@/api/appointmentsApi';
import { getErrorMessage } from '@/utils/getErrorMessage';
import { formatFecha } from '@/utils/formatFecha';
import type { PacienteDetalle } from '@/types/paciente.types';
import type { Appointment, MotivoCancelamiento } from '@/types/appointment.types';

const nav: NavItem[] = [{ label: 'Pacientes', to: '/recepcion', icon: Users }];

// Los valores de MotivoCancelamiento del backend, con el texto que ve recepción.
const MOTIVOS: { valor: MotivoCancelamiento; texto: string }[] = [
    { valor: 'PACIENTE_DESISTIO', texto: 'El paciente desistió' },
    { valor: 'MEDICO_CANCELO', texto: 'El médico canceló' },
    { valor: 'OTROS', texto: 'Otro motivo' },
];

interface ConsultaProps {
    consulta: Appointment;
    onCancelada: (consulta: Appointment) => void;
}

// Una consulta de la lista. «Cancelar» primero pide el motivo, y solo «Sí, cancelar» llama al backend.
const ConsultaProgramada = ({ consulta, onCancelada }: ConsultaProps) => {
    const [abierta, setAbierta] = useState(false);
    const [motivo, setMotivo] = useState<MotivoCancelamiento | ''>('');
    const [error, setError] = useState('');
    const [cancelando, setCancelando] = useState(false);
    const { fecha, hora } = formatFecha(consulta.fecha);
    const idMotivo = `motivo-${consulta.id}`;

    const abrir = () => {
        setAbierta(true);
        setMotivo('');
        setError('');
    };

    const confirmar = async () => {
        if (!motivo) {
            setError('Elige el motivo de la cancelación');
            return;
        }
        setCancelando(true);
        setError('');
        try {
            await appointmentsApi.cancelar(consulta.id, motivo);
            onCancelada(consulta); // la página la quita de la lista
        } catch (err) {
            // Por ejemplo, si faltan menos de 24 horas: el mensaje es el del backend.
            setError(getErrorMessage(err, 'No se pudo cancelar la consulta'));
            setCancelando(false);
        }
    };

    return (
        <li className="border border-gray-200 rounded-xl p-4">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <p className="font-semibold">
                        {fecha} · {hora}
                    </p>
                    <p className="text-sm text-gray-500">
                        {consulta.medicoNombre} · {consulta.especialidad}
                    </p>
                </div>
                {!abierta && (
                    <button type="button" onClick={abrir} className="text-sm font-medium text-red-500 hover:underline">
                        Cancelar
                    </button>
                )}
            </div>

            {abierta && (
                <div className="mt-4 pt-4 border-t border-gray-100 flex flex-col gap-3">
                    <p className="text-sm font-medium">¿Cancelar esta consulta?</p>
                    <div>
                        <label htmlFor={idMotivo} className="block text-sm font-medium text-gray-700 mb-1">
                            Motivo
                        </label>
                        <select
                            id={idMotivo}
                            value={motivo}
                            onChange={(e) => setMotivo(e.target.value as MotivoCancelamiento)}
                            className="input-field"
                            autoFocus
                        >
                            <option value="" disabled>
                                Elige el motivo
                            </option>
                            {MOTIVOS.map((m) => (
                                <option key={m.valor} value={m.valor}>
                                    {m.texto}
                                </option>
                            ))}
                        </select>
                    </div>
                    {error && (
                        <p className="text-red-500 text-sm" role="alert">
                            {error}
                        </p>
                    )}
                    <div className="flex justify-end gap-2">
                        <button
                            type="button"
                            onClick={() => setAbierta(false)}
                            disabled={cancelando}
                            className="px-4 py-2 rounded-md text-sm font-medium text-gray-700 enabled:hover:bg-gray-100
                                disabled:opacity-50"
                        >
                            No
                        </button>
                        <button
                            type="button"
                            onClick={confirmar}
                            disabled={cancelando}
                            className="px-4 py-2 rounded-md text-sm font-medium bg-red-600 text-white
                                enabled:hover:bg-red-700 disabled:opacity-50"
                        >
                            {cancelando ? 'Cancelando…' : 'Sí, cancelar'}
                        </button>
                    </div>
                </div>
            )}
        </li>
    );
};

// Las consultas programadas de un paciente, para verlas y cancelarlas desde recepción (BUG-04).
const RecepcionConsultasPage = () => {
    const { idPaciente } = useParams();
    const [paciente, setPaciente] = useState<PacienteDetalle | null>(null);
    const [consultas, setConsultas] = useState<Appointment[]>([]);
    const [cargando, setCargando] = useState(true);
    const [cargaError, setCargaError] = useState('');
    const [aviso, setAviso] = useState('');

    // El paciente y sus consultas se piden a la vez. El estado solo cambia en los callbacks.
    useEffect(() => {
        const id = Number(idPaciente);
        Promise.all([pacientesApi.detallar(id), appointmentsApi.getConsultasDePaciente(id)])
            .then(([datosPaciente, programadas]) => {
                setPaciente(datosPaciente);
                setConsultas(programadas);
            })
            .catch((err) => setCargaError(getErrorMessage(err, 'No se pudieron cargar las consultas')))
            .finally(() => setCargando(false));
    }, [idPaciente]);

    // La cancelada ya no está programada: sale de la lista, como saldría al volver a pedirla.
    const alCancelar = (consulta: Appointment) => {
        const { fecha, hora } = formatFecha(consulta.fecha);
        // Sin punto final: la hora ya termina con el de «a. m.» o «p. m.».
        setAviso(`Se canceló la consulta del ${fecha} a las ${hora}`);
        setConsultas((actuales) => actuales.filter((c) => c.id !== consulta.id));
    };

    return (
        <DashboardLayout title="Recepción" navItems={nav}>
            <div className="max-w-2xl space-y-4">
                <Link to="/recepcion" className="inline-flex items-center gap-1 text-sm text-primary hover:underline">
                    <ArrowLeft size={16} /> Volver a pacientes
                </Link>

                <Card className="p-6">
                    {cargando ? (
                        <div className="flex items-center gap-2 text-gray-500 text-sm">
                            <Spinner /> Cargando…
                        </div>
                    ) : !paciente ? (
                        <p className="text-red-500 text-sm" role="alert">
                            {cargaError}
                        </p>
                    ) : (
                        <>
                            <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2 mb-4">
                                <div>
                                    <h2 className="text-lg font-semibold">Consultas programadas</h2>
                                    <p className="text-sm text-gray-500">
                                        De {paciente.nombre} · DNI {paciente.documento}
                                    </p>
                                </div>
                                <Link
                                    to={`/recepcion/reservar/${paciente.id}`}
                                    className="text-sm font-medium text-primary hover:underline"
                                >
                                    Reservar consulta
                                </Link>
                            </div>

                            {aviso && <p className="text-green-600 text-sm mb-4">{aviso}</p>}

                            {consultas.length === 0 ? (
                                <p className="text-gray-500 text-sm">No tiene consultas programadas.</p>
                            ) : (
                                <ul className="space-y-3">
                                    {consultas.map((c) => (
                                        <ConsultaProgramada key={c.id} consulta={c} onCancelada={alCancelar} />
                                    ))}
                                </ul>
                            )}
                        </>
                    )}
                </Card>
            </div>
        </DashboardLayout>
    );
};

export default RecepcionConsultasPage;
