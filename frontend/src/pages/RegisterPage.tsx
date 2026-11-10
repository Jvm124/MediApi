import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import PublicLayout from '@/components/layout/PublicLayout';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import { useAuth } from '@/hooks/useAuth';
import { registerSchema, type RegisterFormData } from '@/schemas/registerSchema';

const RegisterPage = () => {
    const navigate = useNavigate();
    const { register: registerUser } = useAuth();
    const [serverError, setServerError] = useState('');

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<RegisterFormData>({ resolver: zodResolver(registerSchema) });

    const onSubmit = async (data: RegisterFormData) => {
        setServerError('');
        try {
            const { confirmPassword, acceptTerms, ...payload } = data;
            await registerUser(payload);
            navigate('/paciente');   // ← antes '/dashboard' (que da 404)
        } catch (err: any) {
            setServerError(err?.response?.data?.message || 'No se pudo crear la cuenta');
        }
    };

    return (
        <PublicLayout showFooter={false}>
            <div className="bg-primary min-h-[calc(100vh-64px)] flex items-center justify-center px-4 py-12">
                <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-2xl">
                    <h2 className="text-center text-2xl font-bold mb-1">Crea tu cuenta</h2>
                    <p className="text-center text-gray-500 text-sm mb-6">
                        Ingresa tus datos para empezar a reservar citas
                    </p>

                    <form onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-1 md:grid-cols-2 gap-4" noValidate>
                        <Input placeholder="Nombre completo" {...register('nombreCompleto')} error={errors.nombreCompleto?.message} />
                        <Input placeholder="DNI" maxLength={8} {...register('dni')} error={errors.dni?.message} />
                        <Input placeholder="Teléfono" maxLength={9} {...register('telefono')} error={errors.telefono?.message} />
                        <Input type="email" placeholder="Correo electrónico" {...register('email')} error={errors.email?.message} />
                        <Input type="password" placeholder="Contraseña" autoComplete="new-password" {...register('password')} error={errors.password?.message} />
                        <Input type="password" placeholder="Repetir contraseña" autoComplete="new-password" {...register('confirmPassword')} error={errors.confirmPassword?.message} />
                        <div className="md:col-span-2 border-t pt-4 mt-2">
                            <p className="font-semibold text-sm mb-2">Dirección</p>
                        </div>
                        <Input placeholder="Calle / Av." {...register('direccion.calle')} error={errors.direccion?.calle?.message} />
                        <Input placeholder="Número (opcional)" {...register('direccion.numero')} error={errors.direccion?.numero?.message} />
                        <Input placeholder="Complemento (opcional)" {...register('direccion.complemento')} error={errors.direccion?.complemento?.message} />
                        <Input placeholder="Barrio / Distrito" {...register('direccion.barrio')} error={errors.direccion?.barrio?.message} />
                        <Input placeholder="Ciudad" {...register('direccion.ciudad')} error={errors.direccion?.ciudad?.message} />
                        <Input placeholder="Código postal" maxLength={5} {...register('direccion.codigo_postal')} error={errors.direccion?.codigo_postal?.message} />
                        <Input placeholder="Departamento" {...register('direccion.estado')} error={errors.direccion?.estado?.message} />
                        <div className="md:col-span-2 flex items-start gap-2 text-sm mt-2">
                            <input type="checkbox" id="terms" {...register('acceptTerms')} className="mt-1" />
                            <label htmlFor="terms">
                                He leído y acepto los{' '}
                                <a href="/terms" className="text-primary underline">Términos y Condiciones</a> de MediApp
                            </label>
                        </div>
                        {errors.acceptTerms && (
                            <p className="md:col-span-2 text-red-500 text-xs">{errors.acceptTerms.message}</p>
                        )}

                        {serverError && (
                            <p className="md:col-span-2 text-red-500 text-sm text-center" role="alert">{serverError}</p>
                        )}

                        <div className="md:col-span-2 flex flex-col gap-2 mt-4">
                            <Button type="submit" isLoading={isSubmitting}>Registrarme</Button>
                            <Link to="/login" className="text-center text-gray-500 hover:underline text-sm">
                                Cancelar
                            </Link>
                        </div>
                    </form>
                </div>
            </div>
        </PublicLayout>
    );
};

export default RegisterPage;