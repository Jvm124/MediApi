import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, Users } from 'lucide-react';
import DashboardLayout, { type NavItem } from '@/components/dashboard/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import Spinner from '@/components/ui/Spinner';
import { pacientesApi } from '@/api/pacientesApi';
import { medicosApi, type MedicoSelect } from '@/api/medicosApi';
import { appointmentsApi } from '@/api/appointmentsApi';
import { getErrorMessage } from '@/utils/getErrorMessage';
import { formatFecha } from '@/utils/formatFecha';
import { reservarSchema, type ReservarFormInput, type ReservarFormData } from '@/schemas/reservarSchema';
import type { PacienteDetalle } from '@/types/paciente.types';

const nav: NavItem[] = [{ label: 'Pacientes', to: '/recepcion', icon: Users }];

// Como en el alta por mostrador, reset() recibe el formulario vacío para que React vea el cambio.
const FORMULARIO_VACIO: ReservarFormInput = { idMedico: '', fecha: '' };

// Reservar una consulta para un paciente desde recepción (BUG-04). Las reglas son las de la reserva del
// paciente (reservarSchema), y el backend las vuelve a revisar todas.
const RecepcionReservarPage = () => {
    const { idPaciente } = useParams();
    const [paciente, setPaciente] = useState<PacienteDetalle | null>(null);
    const [medicos, setMedicos] = useState<MedicoSelect[]>([]);
    const [cargando, setCargando] = useState(true);
    const [cargaError, setCargaError] = useState('');
    const [formError, setFormError] = useState('');
    const [formOk, setFormOk] = useState('');

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<ReservarFormInput, unknown, ReservarFormData>({
        resolver: zodResolver(reservarSchema),
        defaultValues: FORMULARIO_VACIO,
    });

    // El paciente y los médicos se piden a la vez. El estado solo cambia en los callbacks.
    useEffect(() => {
        Promise.all([pacientesApi.detallar(Number(idPaciente)), medicosApi.listarDisponibles()])
            .then(([datosPaciente, disponibles]) => {
                setPaciente(datosPaciente);
                setMedicos(disponibles);
            })
            .catch((err) => setCargaError(getErrorMessage(err, 'No se pudieron cargar los datos para reservar')))
            .finally(() => setCargando(false));
    }, [idPaciente]);

    const onSubmit = async (data: ReservarFormData) => {
        if (!paciente) return;
        setFormError('');
        setFormOk('');
        try {
            const consulta = await appointmentsApi.create({
                idMedico: data.idMedico,
                idPaciente: paciente.id,
                fecha: data.fecha,
            });
            const { fecha, hora } = formatFecha(consulta.fecha);
            // Sin punto final: la hora ya termina con el de «a. m.» o «p. m.».
            setFormOk(`Consulta reservada con ${consulta.medicoNombre} el ${fecha} a las ${hora}`);
            reset(FORMULARIO_VACIO);
        } catch (err) {
            setFormError(getErrorMessage(err, 'No se pudo reservar la consulta'));
        }
    };

    const sinMedicos = medicos.length === 0;

    return (
        <DashboardLayout title="Recepción" navItems={nav}>
            <div className="max-w-xl space-y-4">
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
                            <h2 className="text-lg font-semibold">Reservar consulta</h2>
                            <p className="text-sm text-gray-500 mb-4">
                                Para {paciente.nombre} · DNI {paciente.documento}
                            </p>

                            <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
                                <div>
                                    <label htmlFor="medico" className="block text-sm font-medium text-gray-700 mb-1">
                                        Médico
                                    </label>
                                    <select
                                        id="medico"
                                        {...register('idMedico')}
                                        disabled={sinMedicos}
                                        className={`input-field ${errors.idMedico ? 'border-red-500' : ''}`}
                                    >
                                        <option value="" disabled>
                                            Selecciona un médico
                                        </option>
                                        {medicos.map((m) => (
                                            <option key={m.id} value={m.id}>
                                                {m.nombre} · {m.especialidad}
                                            </option>
                                        ))}
                                    </select>
                                    {errors.idMedico && (
                                        <p className="text-red-500 text-xs mt-1">{errors.idMedico.message}</p>
                                    )}
                                    {sinMedicos && (
                                        <p className="text-gray-500 text-xs mt-1">
                                            No hay médicos activos para reservar.
                                        </p>
                                    )}
                                </div>

                                <Input
                                    type="datetime-local"
                                    label="Fecha y hora"
                                    {...register('fecha')}
                                    error={errors.fecha?.message}
                                />

                                {formError && (
                                    <p className="text-red-500 text-sm" role="alert">
                                        {formError}
                                    </p>
                                )}
                                {formOk && <p className="text-green-600 text-sm">{formOk}</p>}

                                <Button type="submit" isLoading={isSubmitting} disabled={sinMedicos}>
                                    Reservar
                                </Button>
                            </form>
                        </>
                    )}
                </Card>
            </div>
        </DashboardLayout>
    );
};

export default RecepcionReservarPage;
