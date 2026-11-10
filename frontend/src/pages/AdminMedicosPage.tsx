import { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Users, Stethoscope } from 'lucide-react';
import DashboardLayout, { type NavItem } from '@/components/dashboard/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import Spinner from '@/components/ui/Spinner';
import { medicosApi } from '@/api/medicosApi';
import { getErrorMessage } from '@/utils/getErrorMessage';
import { registrarMedicoSchema, type RegistrarMedicoFormData } from '@/schemas/registrarMedicoSchema.ts';
import type { MedicoLista } from '@/types/medico.types';

const nav: NavItem[] = [
    { label: 'Usuarios', to: '/admin', icon: Users },
    { label: 'Médicos', to: '/admin/medicos', icon: Stethoscope },
];

const AdminMedicosPage = () => {
    const [medicos, setMedicos] = useState<MedicoLista[]>([]);
    const [loading, setLoading] = useState(true);
    const [listError, setListError] = useState('');
    const [formError, setFormError] = useState('');
    const [formOk, setFormOk] = useState('');

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<RegistrarMedicoFormData>({ resolver: zodResolver(registrarMedicoSchema) });

    const cargar = useCallback(async () => {
        setListError('');
        try {
            const page = await medicosApi.listar();
            setMedicos(page.content);
        } catch (err) {
            setListError(getErrorMessage(err, 'No se pudieron cargar los médicos'));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        cargar();
    }, [cargar]);

    const onSubmit = async (data: RegistrarMedicoFormData) => {
        setFormError('');
        setFormOk('');
        try {
            await medicosApi.registrar(data);
            setFormOk(`Médico ${data.nombre} registrado`);
            reset();
            await cargar();
        } catch (err) {
            setFormError(getErrorMessage(err, 'No se pudo registrar el médico'));
        }
    };

    const eliminar = async (m: MedicoLista) => {
        if (!window.confirm(`¿Dar de baja a ${m.nombre}?`)) return;
        try {
            await medicosApi.eliminar(m.id);
            await cargar();
        } catch (err) {
            setListError(getErrorMessage(err, 'No se pudo dar de baja al médico'));
        }
    };

    return (
        <DashboardLayout title="Médicos" navItems={nav}>
            <div className="space-y-6">
                <Card className="p-6">
                    <h2 className="text-lg font-semibold mb-4">Registrar médico</h2>
                    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 md:grid-cols-2" noValidate>
                        <Input placeholder="Nombre" {...register('nombre')} error={errors.nombre?.message} />
                        <div>
                            <select
                                {...register('especialidad')}
                                defaultValue=""
                                className={`input-field ${errors.especialidad ? 'border-red-500' : ''}`}
                            >
                                <option value="" disabled>Especialidad</option>
                                <option value="ORTOPEDIA">Ortopedia</option>
                                <option value="CARDIOLOGIA">Cardiología</option>
                                <option value="GINECOLOGIA">Ginecología</option>
                                <option value="DERMATOLOGIA">Dermatología</option>
                            </select>
                            {errors.especialidad && <p className="text-red-500 text-xs mt-1">{errors.especialidad.message}</p>}
                        </div>

                        <Input type="email" placeholder="Correo" {...register('email')} error={errors.email?.message} />
                        <Input type="password" placeholder="Contraseña" {...register('contrasenia')} error={errors.contrasenia?.message} />
                        <Input placeholder="Teléfono" {...register('telefono')} error={errors.telefono?.message} />
                        <Input placeholder="Documento (DNI)" {...register('documento')} error={errors.documento?.message} />

                        <div className="md:col-span-2 pt-2 text-sm font-semibold text-gray-500">Dirección</div>
                        <Input placeholder="Calle" {...register('direccion.calle')} error={errors.direccion?.calle?.message} />
                        <Input placeholder="Número" {...register('direccion.numero')} error={errors.direccion?.numero?.message} />
                        <Input placeholder="Complemento (opcional)" {...register('direccion.complemento')} error={errors.direccion?.complemento?.message} />
                        <Input placeholder="Barrio" {...register('direccion.barrio')} error={errors.direccion?.barrio?.message} />
                        <Input placeholder="Ciudad" {...register('direccion.ciudad')} error={errors.direccion?.ciudad?.message} />
                        <Input placeholder="Código postal" {...register('direccion.codigo_postal')} error={errors.direccion?.codigo_postal?.message} />
                        <Input placeholder="Estado / Región" {...register('direccion.estado')} error={errors.direccion?.estado?.message} />

                        {formError && <p className="md:col-span-2 text-red-500 text-sm">{formError}</p>}
                        {formOk && <p className="md:col-span-2 text-green-600 text-sm">{formOk}</p>}

                        <div className="md:col-span-2">
                            <Button type="submit" isLoading={isSubmitting}>Registrar médico</Button>
                        </div>
                    </form>
                </Card>

                <Card className="p-6">
                    <h2 className="text-lg font-semibold mb-4">Médicos activos</h2>

                    {loading ? (
                        <div className="flex items-center gap-2 text-gray-500 text-sm"><Spinner /> Cargando…</div>
                    ) : listError ? (
                        <p className="text-red-500 text-sm">{listError}</p>
                    ) : medicos.length === 0 ? (
                        <p className="text-gray-500 text-sm">No hay médicos registrados.</p>
                    ) : (
                        <>
                            <table className="w-full text-sm hidden md:table">
                                <thead>
                                <tr className="text-left text-gray-500 border-b border-gray-200">
                                    <th className="py-2 pr-4 font-medium">Nombre</th>
                                    <th className="py-2 pr-4 font-medium">Especialidad</th>
                                    <th className="py-2 pr-4 font-medium">Documento</th>
                                    <th className="py-2 text-right font-medium">Acción</th>
                                </tr>
                                </thead>
                                <tbody>
                                {medicos.map((m) => (
                                    <tr key={m.id} className="border-b border-gray-100 last:border-0">
                                        <td className="py-3 pr-4">{m.nombre}</td>
                                        <td className="py-3 pr-4">{m.especialidad}</td>
                                        <td className="py-3 pr-4">{m.documento}</td>
                                        <td className="py-3 text-right">
                                            <button onClick={() => eliminar(m)} className="text-sm font-medium text-red-500 hover:underline">
                                                Dar de baja
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>

                            <div className="md:hidden space-y-3">
                                {medicos.map((m) => (
                                    <div key={m.id} className="border border-gray-200 rounded-xl p-4">
                                        <div className="flex items-center justify-between mb-1">
                                            <span className="font-semibold">{m.nombre}</span>
                                            <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-primary/10 text-primary-dark">
                                                {m.especialidad}
                                            </span>
                                        </div>
                                        <p className="text-sm text-gray-500">DNI {m.documento}</p>
                                        <div className="text-right mt-2">
                                            <button onClick={() => eliminar(m)} className="text-sm font-medium text-red-500 hover:underline">
                                                Dar de baja
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </>
                    )}
                </Card>
            </div>
        </DashboardLayout>
    );
};

export default AdminMedicosPage;
