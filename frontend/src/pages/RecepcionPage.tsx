import { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Users } from 'lucide-react';
import DashboardLayout, { type NavItem } from '@/components/dashboard/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import Spinner from '@/components/ui/Spinner';
import Paginacion from '@/components/ui/Paginacion';
import { pacientesApi } from '@/api/pacientesApi';
import { getErrorMessage } from '@/utils/getErrorMessage';
import { altaMostradorSchema, type AltaMostradorFormData } from '@/schemas/altaMostradorSchema';
import type { Page } from '@/types/usuario.types';
import type { PacienteLista } from '@/types/paciente.types';

const nav: NavItem[] = [{ label: 'Pacientes', to: '/recepcion', icon: Users }];

// El formulario vacío. Se le pasa a reset() a propósito: sin valores, reset() vacía los campos con el
// form.reset() del navegador, que React no ve. Si luego se pega (o el navegador autocompleta) el mismo valor
// que había antes, React no avisa del cambio y el formulario da el campo por vacío.
const FORMULARIO_VACIO: AltaMostradorFormData = {
    nombre: '',
    documento: '',
    telefono: '',
    email: '',
    direccion: { calle: '', numero: '', complemento: '', barrio: '', ciudad: '', codigo_postal: '', estado: '' },
};

// Panel de recepción (BUG-04): la lista de pacientes y el alta por mostrador.
const RecepcionPage = () => {
    const [pacientes, setPacientes] = useState<Page<PacienteLista> | null>(null);
    const [cargando, setCargando] = useState(true);
    const [listError, setListError] = useState('');
    const [formError, setFormError] = useState('');
    const [formOk, setFormOk] = useState('');

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<AltaMostradorFormData>({
        resolver: zodResolver(altaMostradorSchema),
        defaultValues: FORMULARIO_VACIO,
    });

    // Pide una página de pacientes. El estado solo cambia en los callbacks, cuando llega la respuesta: así el
    // efecto de abajo no cambia el estado de forma síncrona (la regla react-hooks/set-state-in-effect).
    const cargar = useCallback(
        (pagina: number) =>
            pacientesApi
                .listar(pagina)
                .then((page) => {
                    setPacientes(page);
                    setListError('');
                })
                .catch((err) => setListError(getErrorMessage(err, 'No se pudieron cargar los pacientes')))
                .finally(() => setCargando(false)),
        []
    );

    useEffect(() => {
        cargar(0);
    }, [cargar]);

    const irAPagina = (pagina: number) => {
        setCargando(true);
        cargar(pagina);
    };

    const onSubmit = async (data: AltaMostradorFormData) => {
        setFormError('');
        setFormOk('');
        try {
            await pacientesApi.registrarEnMostrador(data);
            setFormOk(`Paciente ${data.nombre} registrado`);
            reset(FORMULARIO_VACIO);
            // Se vuelve a pedir la página que se estaba viendo; el nuevo puede estar en otra, porque van por nombre.
            setCargando(true);
            await cargar(pacientes?.number ?? 0);
        } catch (err) {
            setFormError(getErrorMessage(err, 'No se pudo registrar al paciente'));
        }
    };

    return (
        <DashboardLayout title="Recepción" navItems={nav}>
            <div className="grid gap-6 lg:grid-cols-3">
                <Card className="p-6 lg:col-span-2 h-fit">
                    <div className="flex items-baseline justify-between gap-4 mb-4">
                        <h2 className="text-lg font-semibold">Pacientes</h2>
                        {pacientes && (
                            <span className="text-sm text-gray-500">{pacientes.totalElements} en total</span>
                        )}
                    </div>

                    {listError && (
                        <p className="text-red-500 text-sm mb-4" role="alert">
                            {listError}
                        </p>
                    )}

                    {!pacientes ? (
                        cargando && (
                            <div className="flex items-center gap-2 text-gray-500 text-sm">
                                <Spinner /> Cargando…
                            </div>
                        )
                    ) : pacientes.content.length === 0 ? (
                        <p className="text-gray-500 text-sm">Todavía no hay pacientes.</p>
                    ) : (
                        <div className={cargando ? 'opacity-60' : ''}>
                            <table className="w-full text-sm hidden md:table">
                                <thead>
                                <tr className="text-left text-gray-500 border-b border-gray-200">
                                    <th className="py-2 pr-4 font-medium">Nombre</th>
                                    <th className="py-2 pr-4 font-medium">Correo</th>
                                    <th className="py-2 font-medium">Teléfono</th>
                                </tr>
                                </thead>
                                <tbody>
                                {pacientes.content.map((p) => (
                                    <tr key={p.id} className="border-b border-gray-100 last:border-0">
                                        <td className="py-3 pr-4">{p.nombre}</td>
                                        <td className="py-3 pr-4 break-all">{p.email}</td>
                                        <td className="py-3">{p.telefono}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>

                            <div className="md:hidden space-y-3">
                                {pacientes.content.map((p) => (
                                    <div key={p.id} className="border border-gray-200 rounded-xl p-4">
                                        <p className="font-semibold">{p.nombre}</p>
                                        <p className="text-sm text-gray-500 break-all">{p.email}</p>
                                        <p className="text-sm text-gray-500">{p.telefono}</p>
                                    </div>
                                ))}
                            </div>

                            <Paginacion
                                pagina={pacientes.number}
                                totalPaginas={pacientes.totalPages}
                                onCambiar={irAPagina}
                                deshabilitada={cargando}
                            />
                        </div>
                    )}
                </Card>

                <Card className="p-6 h-fit">
                    <h2 className="text-lg font-semibold">Alta por mostrador</h2>
                    <p className="text-sm text-gray-500 mb-4">El paciente queda registrado sin cuenta web.</p>

                    {/* autoComplete off: el navegador no debe proponer los datos de quien atiende el mostrador */}
                    <form
                        onSubmit={handleSubmit(onSubmit)}
                        className="flex flex-col gap-4"
                        autoComplete="off"
                        noValidate
                    >
                        <Input
                            placeholder="Nombre completo"
                            {...register('nombre')}
                            error={errors.nombre?.message}
                        />
                        <Input
                            placeholder="DNI"
                            maxLength={8}
                            inputMode="numeric"
                            {...register('documento')}
                            error={errors.documento?.message}
                        />
                        <Input
                            placeholder="Teléfono"
                            maxLength={9}
                            inputMode="numeric"
                            {...register('telefono')}
                            error={errors.telefono?.message}
                        />
                        <Input
                            type="email"
                            placeholder="Correo electrónico"
                            {...register('email')}
                            error={errors.email?.message}
                        />

                        <p className="font-semibold text-sm border-t pt-4">Dirección</p>
                        <Input
                            placeholder="Calle / Av."
                            {...register('direccion.calle')}
                            error={errors.direccion?.calle?.message}
                        />
                        <Input
                            placeholder="Número (opcional)"
                            {...register('direccion.numero')}
                            error={errors.direccion?.numero?.message}
                        />
                        <Input
                            placeholder="Complemento (opcional)"
                            {...register('direccion.complemento')}
                            error={errors.direccion?.complemento?.message}
                        />
                        <Input
                            placeholder="Barrio / Distrito"
                            {...register('direccion.barrio')}
                            error={errors.direccion?.barrio?.message}
                        />
                        <Input
                            placeholder="Ciudad"
                            {...register('direccion.ciudad')}
                            error={errors.direccion?.ciudad?.message}
                        />
                        <Input
                            placeholder="Código postal"
                            maxLength={5}
                            inputMode="numeric"
                            {...register('direccion.codigo_postal')}
                            error={errors.direccion?.codigo_postal?.message}
                        />
                        <Input
                            placeholder="Departamento"
                            {...register('direccion.estado')}
                            error={errors.direccion?.estado?.message}
                        />

                        {formError && (
                            <p className="text-red-500 text-sm" role="alert">
                                {formError}
                            </p>
                        )}
                        {formOk && <p className="text-green-600 text-sm">{formOk}</p>}

                        <Button type="submit" isLoading={isSubmitting}>
                            Registrar paciente
                        </Button>
                    </form>
                </Card>
            </div>
        </DashboardLayout>
    );
};

export default RecepcionPage;
