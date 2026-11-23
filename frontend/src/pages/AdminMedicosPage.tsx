import { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Users, Stethoscope } from 'lucide-react';
import DashboardLayout, { type NavItem } from '@/components/dashboard/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import Spinner from '@/components/ui/Spinner';
import Paginacion from '@/components/ui/Paginacion';
import { medicosApi } from '@/api/medicosApi';
import { getErrorMessage } from '@/utils/getErrorMessage';
import {
    registrarMedicoSchema,
    type RegistrarMedicoFormInput,
    type RegistrarMedicoFormData,
} from '@/schemas/registrarMedicoSchema';
import type { Page } from '@/types/usuario.types';
import type { MedicoLista } from '@/types/medico.types';

const nav: NavItem[] = [
    { label: 'Usuarios', to: '/admin', icon: Users },
    { label: 'Médicos', to: '/admin/medicos', icon: Stethoscope },
];

// El formulario vacío, con la especialidad sin elegir. Se le pasa a reset() a propósito, como en el alta de
// usuarios: sin valores, reset() vacía los campos con el form.reset() del navegador, que React no ve, y si luego
// el navegador autocompleta (o se pega) el mismo valor que en el alta anterior, el formulario lo da por vacío.
// Pasa, por ejemplo, con la dirección de la clínica.
const FORMULARIO_VACIO: RegistrarMedicoFormInput = {
    nombre: '',
    especialidad: '',
    email: '',
    contrasenia: '',
    telefono: '',
    documento: '',
    direccion: { calle: '', numero: '', complemento: '', barrio: '', ciudad: '', codigo_postal: '', estado: '' },
};

// Pide una página de médicos activos. Si llega vacía y no es la primera, pide la última que queda: pasa al dar de
// baja al único médico de la última página, porque la lista solo muestra a los activos.
const pedirPagina = async (pagina: number): Promise<Page<MedicoLista>> => {
    const page = await medicosApi.listar(pagina);
    if (page.content.length === 0 && page.number > 0 && page.totalPages > 0) {
        return medicosApi.listar(page.totalPages - 1);
    }
    return page;
};

const AdminMedicosPage = () => {
    const [medicos, setMedicos] = useState<Page<MedicoLista> | null>(null);
    const [cargando, setCargando] = useState(true);
    const [listError, setListError] = useState('');
    const [formError, setFormError] = useState('');
    const [formOk, setFormOk] = useState('');

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<RegistrarMedicoFormInput, unknown, RegistrarMedicoFormData>({
        resolver: zodResolver(registrarMedicoSchema),
        defaultValues: FORMULARIO_VACIO,
    });

    // Pide una página de médicos (BUG-07). Como en el panel de usuarios, el estado solo cambia en los callbacks,
    // cuando llega la respuesta: así el efecto de abajo no cambia el estado de forma síncrona.
    const cargar = useCallback(
        (pagina: number) =>
            pedirPagina(pagina)
                .then((page) => {
                    setMedicos(page);
                    setListError('');
                })
                .catch((err) => setListError(getErrorMessage(err, 'No se pudieron cargar los médicos')))
                .finally(() => setCargando(false)),
        []
    );

    useEffect(() => {
        cargar(0);
    }, [cargar]);

    const irAPagina = (pagina: number) => {
        setCargando(true);
        return cargar(pagina);
    };

    // Después de un cambio se vuelve a pedir la página que se estaba viendo, no la primera.
    const recargar = () => irAPagina(medicos?.number ?? 0);

    const onSubmit = async (data: RegistrarMedicoFormData) => {
        setFormError('');
        setFormOk('');
        try {
            await medicosApi.registrar(data);
            setFormOk(`Médico ${data.nombre} registrado`);
            reset(FORMULARIO_VACIO);
            // El nuevo puede quedar en otra página, porque van por nombre.
            await recargar();
        } catch (err) {
            setFormError(getErrorMessage(err, 'No se pudo registrar el médico'));
        }
    };

    // Si la baja falla, el aviso sale encima de la lista y la lista se queda.
    const eliminar = async (m: MedicoLista) => {
        if (!window.confirm(`¿Dar de baja a ${m.nombre}?`)) return;
        setListError('');
        try {
            await medicosApi.eliminar(m.id);
            await recargar();
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
                    <div className="flex items-baseline justify-between gap-4 mb-4">
                        <h2 className="text-lg font-semibold">Médicos activos</h2>
                        {medicos && (
                            <span className="text-sm text-gray-500">{medicos.totalElements} en total</span>
                        )}
                    </div>

                    {listError && (
                        <p className="text-red-500 text-sm mb-4" role="alert">
                            {listError}
                        </p>
                    )}

                    {!medicos ? (
                        cargando && (
                            <div className="flex items-center gap-2 text-gray-500 text-sm">
                                <Spinner /> Cargando…
                            </div>
                        )
                    ) : medicos.content.length === 0 ? (
                        <p className="text-gray-500 text-sm">No hay médicos registrados.</p>
                    ) : (
                        <div className={cargando ? 'opacity-60' : ''}>
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
                                {medicos.content.map((m) => (
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
                                {medicos.content.map((m) => (
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

                            <Paginacion
                                pagina={medicos.number}
                                totalPaginas={medicos.totalPages}
                                onCambiar={irAPagina}
                                deshabilitada={cargando}
                            />
                        </div>
                    )}
                </Card>
            </div>
        </DashboardLayout>
    );
};

export default AdminMedicosPage;
