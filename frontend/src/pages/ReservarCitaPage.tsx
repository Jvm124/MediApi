import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ReservarFormInput, ReservarFormData, reservarSchema } from '@/schemas/reservarSchema';
import { appointmentsApi } from '@/api/appointmentsApi';
import { medicosApi, type MedicoSelect } from '@/api/medicosApi';
import PublicLayout from '@/components/layout/PublicLayout';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import Spinner from '@/components/ui/Spinner';

const ReservarCitaPage = () => {
    const navigate = useNavigate();
    const [serverError, setServerError] = useState('');

    const [medicos, setMedicos] = useState<MedicoSelect[]>([]);
    const [loadingMedicos, setLoadingMedicos] = useState(true);
    const [medicosError, setMedicosError] = useState('');

    const {
        register,
        handleSubmit,
        watch,
        formState: { errors, isSubmitting },
        // <Entrada (string), Contexto, Salida (number)>
    } = useForm<ReservarFormInput, unknown, ReservarFormData>({ resolver: zodResolver(reservarSchema) });

    // Cuál médico está marcado ahora mismo (string, tal como lo da el radio).
    const selectedMedico = watch('idMedico');

    useEffect(() => {
        medicosApi
            .listarDisponibles()
            .then(setMedicos)
            .catch(() => setMedicosError('No se pudo cargar la lista de médicos'))
            .finally(() => setLoadingMedicos(false));
    }, []);

    const onSubmit = async (data: ReservarFormData) => {
        setServerError('');
        try {
            await appointmentsApi.create({
                idMedico: data.idMedico,
                fecha: data.fecha,
            });
            navigate('/paciente');
        } catch (err) {
            const message = axios.isAxiosError(err)
                ? err.response?.data?.message
                : undefined;
            setServerError(message || 'No se pudo realizar la reserva');
        }
    };

    const sinMedicos = !!medicosError || medicos.length === 0;

    return (
        <PublicLayout showFooter={false}>
            <div className="bg-primary min-h-[calc(100vh-64px)] flex items-center justify-center px-4 py-12">
                <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-lg">
                    <h2 className="text-center text-2xl font-bold mb-1">Reservar cita</h2>
                    <p className="text-center text-gray-500 text-sm mb-6">
                        Elige el médico y la fecha para tu consulta
                    </p>

                    {/* Columna: tarjetas de médicos arriba, fecha debajo */}
                    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5" noValidate>
                        {/* Selección de médico como tarjetas */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Médico</label>

                            {loadingMedicos ? (
                                <div className="flex items-center gap-2 text-gray-500 text-sm py-2">
                                    <Spinner /> Cargando médicos…
                                </div>
                            ) : medicosError ? (
                                <p className="text-red-500 text-sm">{medicosError}</p>
                            ) : (
                                <div className="space-y-3 max-h-72 overflow-y-auto pr-1">
                                    {medicos.map((m) => {
                                        const seleccionada = String(m.id) === selectedMedico;
                                        return (
                                            <label
                                                key={m.id}
                                                className={`block border rounded-xl p-4 cursor-pointer transition ${
                                                    seleccionada
                                                        ? 'border-primary ring-2 ring-primary/40 bg-blue-50'
                                                        : 'border-gray-200 hover:border-primary/60'
                                                }`}
                                            >
                                                {/* radio oculto: es lo que RHF realmente registra */}
                                                <input
                                                    type="radio"
                                                    value={m.id}
                                                    {...register('idMedico')}
                                                    className="sr-only"
                                                />
                                                <p className="font-semibold text-gray-900">{m.nombre}</p>
                                                <p className="text-sm text-gray-500">{m.especialidad}</p>
                                            </label>
                                        );
                                    })}
                                </div>
                            )}

                            {errors.idMedico && (
                                <p className="text-red-500 text-xs mt-2">{errors.idMedico.message}</p>
                            )}
                        </div>

                        {/* Fecha y hora */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Fecha y hora</label>
                            <Input
                                type="datetime-local"
                                {...register('fecha')}
                                error={errors.fecha?.message}
                            />
                        </div>

                        {serverError && (
                            <p className="text-red-500 text-sm text-center" role="alert">{serverError}</p>
                        )}

                        <div className="flex flex-col gap-2 mt-2">
                            <Button
                                type="submit"
                                isLoading={isSubmitting}
                                disabled={loadingMedicos || sinMedicos}
                            >
                                Reservar
                            </Button>
                            <Link to="/paciente" className="text-center text-gray-500 hover:underline text-sm">
                                Cancelar
                            </Link>
                        </div>
                    </form>
                </div>
            </div>
        </PublicLayout>
    );
};

export default ReservarCitaPage;
