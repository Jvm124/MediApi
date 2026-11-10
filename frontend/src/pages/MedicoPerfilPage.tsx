import { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { CalendarDays, UserCog } from 'lucide-react';
import DashboardLayout, { type NavItem } from '@/components/dashboard/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import Spinner from '@/components/ui/Spinner';
import { medicosApi } from '@/api/medicosApi';
import { getErrorMessage } from '@/utils/getErrorMessage';
import { perfilMedicoSchema, type PerfilMedicoFormData } from '@/schemas/perfilMedicoSchema';

const nav: NavItem[] = [
    { label: 'Mi agenda', to: '/medico', icon: CalendarDays },
    { label: 'Mi perfil', to: '/medico/perfil', icon: UserCog },
];

const MedicoPerfilPage = () => {
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const [formError, setFormError] = useState('');
    const [formOk, setFormOk] = useState('');

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<PerfilMedicoFormData>({ resolver: zodResolver(perfilMedicoSchema) });

    const cargar = useCallback(async () => {
        setLoadError('');
        try {
            const perfil = await medicosApi.miPerfil();
            reset({ telefono: perfil.telefono, direccion: perfil.direccion });
        } catch (err) {
            setLoadError(getErrorMessage(err, 'No se pudo cargar tu perfil'));
        } finally {
            setLoading(false);
        }
    }, [reset]);

    useEffect(() => {
        cargar();
    }, [cargar]);

    const onSubmit = async (data: PerfilMedicoFormData) => {
        setFormError('');
        setFormOk('');
        try {
            await medicosApi.actualizarPerfil(data);
            setFormOk('Perfil actualizado');
        } catch (err) {
            setFormError(getErrorMessage(err, 'No se pudo actualizar el perfil'));
        }
    };

    return (
        <DashboardLayout title="Mi perfil" navItems={nav}>
            {loading ? (
                <Spinner />
            ) : loadError ? (
                <p className="text-red-500 text-sm">{loadError}</p>
            ) : (
                <Card className="p-6 max-w-2xl">
                    <h2 className="text-lg font-semibold mb-1">Editar mi perfil</h2>
                    <p className="text-sm text-gray-500 mb-4">Solo puedes cambiar tu teléfono y dirección.</p>

                    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 md:grid-cols-2" noValidate>
                        <div className="md:col-span-2">
                            <Input placeholder="Teléfono" {...register('telefono')} error={errors.telefono?.message} />
                        </div>

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
                            <Button type="submit" isLoading={isSubmitting}>Guardar cambios</Button>
                        </div>
                    </form>
                </Card>
            )}
        </DashboardLayout>
    );
};

export default MedicoPerfilPage;
