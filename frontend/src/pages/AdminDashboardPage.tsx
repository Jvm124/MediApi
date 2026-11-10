import { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Users, Stethoscope } from 'lucide-react';
import DashboardLayout, { type NavItem } from '@/components/dashboard/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import Spinner from '@/components/ui/Spinner';
import { useAuth } from '@/hooks/useAuth';
import { usuariosApi } from '@/api/usuariosApi';
import { getErrorMessage } from '@/utils/getErrorMessage';
import { registrarUsuarioSchema, type RegistrarUsuarioFormData } from '@/schemas/registrarUsuarioSchema';
import type { UsuarioLista, EstadoUsuario } from '@/types/usuario.types';

const nav: NavItem[] = [
    { label: 'Usuarios', to: '/admin', icon: Users },
    { label: 'Médicos', to: '/admin/medicos', icon: Stethoscope },
];

const ESTADO_STYLES: Record<EstadoUsuario, string> = {
    ACTIVO: 'bg-green-100 text-green-700',
    SUSPENDIDO: 'bg-amber-100 text-amber-700',
    BAJA: 'bg-gray-100 text-gray-500',
};

const ESTADO_LABEL: Record<EstadoUsuario, string> = {
    ACTIVO: 'Activo',
    SUSPENDIDO: 'Suspendido',
    BAJA: 'Baja',
};

const EstadoPill = ({ estado }: { estado: EstadoUsuario }) => (
    <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${ESTADO_STYLES[estado]}`}>
        {ESTADO_LABEL[estado]}
    </span>
);

const AccionLink = ({ label, color, onClick }: { label: string; color: string; onClick: () => void }) => (
    <button onClick={onClick} className={`text-sm font-medium hover:underline ${color}`}>
        {label}
    </button>
);

const AdminDashboardPage = () => {
    const { user } = useAuth();

    const [usuarios, setUsuarios] = useState<UsuarioLista[]>([]);
    const [loading, setLoading] = useState(true);
    const [listError, setListError] = useState('');
    const [formError, setFormError] = useState('');
    const [formOk, setFormOk] = useState('');

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<RegistrarUsuarioFormData>({ resolver: zodResolver(registrarUsuarioSchema) });

    const cargar = useCallback(async () => {
        setListError('');
        try {
            const page = await usuariosApi.listar();
            setUsuarios(page.content);
        } catch (err) {
            setListError(getErrorMessage(err, 'No se pudieron cargar los usuarios'));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        cargar();
    }, [cargar]);

    const onSubmit = async (data: RegistrarUsuarioFormData) => {
        setFormError('');
        setFormOk('');
        try {
            await usuariosApi.registrar(data);
            setFormOk(`Usuario ${data.correo} creado`);
            reset();
            await cargar();
        } catch (err) {
            setFormError(getErrorMessage(err, 'No se pudo crear el usuario'));
        }
    };

    const ejecutar = async (accion: () => Promise<void>, errorMsg: string) => {
        setListError('');
        try {
            await accion();
            await cargar();
        } catch (err) {
            setListError(getErrorMessage(err, errorMsg));
        }
    };

    const suspender = (u: UsuarioLista) => {
        if (!window.confirm(`¿Suspender a ${u.correo}? No podrá iniciar sesión hasta reactivarlo.`)) return;
        ejecutar(() => usuariosApi.suspender(u.id), 'No se pudo suspender el usuario');
    };

    const reactivar = (u: UsuarioLista) =>
        ejecutar(() => usuariosApi.reactivar(u.id), 'No se pudo reactivar el usuario');

    const darDeBaja = (u: UsuarioLista) => {
        if (!window.confirm(`¿Dar de baja a ${u.correo}? Se puede reincorporar más adelante.`)) return;
        ejecutar(() => usuariosApi.darDeBaja(u.id), 'No se pudo dar de baja el usuario');
    };

    // Los botones disponibles dependen del estado (refleja las transiciones válidas del backend).
    // Un usuario no puede accionar sobre su propia cuenta.
    const Acciones = ({ u }: { u: UsuarioLista }) => {
        if (u.id === user?.id) {
            return <span className="text-xs text-gray-400">—</span>;
        }
        return (
            <div className="inline-flex gap-3">
                {u.estado === 'ACTIVO' && (
                    <>
                        <AccionLink label="Suspender" color="text-amber-600" onClick={() => suspender(u)} />
                        <AccionLink label="Dar de baja" color="text-red-500" onClick={() => darDeBaja(u)} />
                    </>
                )}
                {u.estado === 'SUSPENDIDO' && (
                    <>
                        <AccionLink label="Reactivar" color="text-green-600" onClick={() => reactivar(u)} />
                        <AccionLink label="Dar de baja" color="text-red-500" onClick={() => darDeBaja(u)} />
                    </>
                )}
                {u.estado === 'BAJA' && (
                    <AccionLink label="Reincorporar" color="text-green-600" onClick={() => reactivar(u)} />
                )}
            </div>
        );
    };

    return (
        <DashboardLayout title="Administración" navItems={nav}>
            <div className="grid gap-6 lg:grid-cols-3">
                <Card className="p-6 lg:col-span-1 h-fit">
                    <h2 className="text-lg font-semibold mb-4">Registrar usuario</h2>
                    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
                        <Input
                            type="email"
                            placeholder="Correo"
                            {...register('correo')}
                            error={errors.correo?.message}
                        />
                        <Input
                            type="password"
                            placeholder="Contraseña"
                            {...register('contrasenia')}
                            error={errors.contrasenia?.message}
                        />
                        <div>
                            <select
                                {...register('rol')}
                                defaultValue=""
                                className={`input-field ${errors.rol ? 'border-red-500' : ''}`}
                            >
                                <option value="" disabled>
                                    Selecciona un rol
                                </option>
                                <option value="RECEPCIONISTA">Recepcionista</option>
                                <option value="ADMINISTRADOR">Administrador</option>
                            </select>
                            {errors.rol && <p className="text-red-500 text-xs mt-1">{errors.rol.message}</p>}
                        </div>

                        {formError && <p className="text-red-500 text-sm">{formError}</p>}
                        {formOk && <p className="text-green-600 text-sm">{formOk}</p>}

                        <Button type="submit" isLoading={isSubmitting}>
                            Crear usuario
                        </Button>
                    </form>
                </Card>

                <Card className="p-6 lg:col-span-2">
                    <h2 className="text-lg font-semibold mb-4">Usuarios</h2>

                    {loading ? (
                        <div className="flex items-center gap-2 text-gray-500 text-sm">
                            <Spinner /> Cargando…
                        </div>
                    ) : listError ? (
                        <p className="text-red-500 text-sm">{listError}</p>
                    ) : usuarios.length === 0 ? (
                        <p className="text-gray-500 text-sm">No hay usuarios.</p>
                    ) : (
                        <>
                            <table className="w-full text-sm hidden md:table">
                                <thead>
                                <tr className="text-left text-gray-500 border-b border-gray-200">
                                    <th className="py-2 pr-4 font-medium">Correo</th>
                                    <th className="py-2 pr-4 font-medium">Rol</th>
                                    <th className="py-2 pr-4 font-medium">Estado</th>
                                    <th className="py-2 text-right font-medium">Acción</th>
                                </tr>
                                </thead>
                                <tbody>
                                {usuarios.map((u) => (
                                    <tr key={u.id} className="border-b border-gray-100 last:border-0">
                                        <td className="py-3 pr-4">{u.correo}</td>
                                        <td className="py-3 pr-4">{u.rol}</td>
                                        <td className="py-3 pr-4">
                                            <EstadoPill estado={u.estado} />
                                        </td>
                                        <td className="py-3 text-right">
                                            <Acciones u={u} />
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>

                            <div className="md:hidden space-y-3">
                                {usuarios.map((u) => (
                                    <div key={u.id} className="border border-gray-200 rounded-xl p-4">
                                        <div className="flex items-center justify-between mb-1">
                                            <span className="font-semibold break-all">{u.correo}</span>
                                            <EstadoPill estado={u.estado} />
                                        </div>
                                        <p className="text-sm text-gray-500">{u.rol}</p>
                                        <div className="text-right mt-2">
                                            <Acciones u={u} />
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

export default AdminDashboardPage;
