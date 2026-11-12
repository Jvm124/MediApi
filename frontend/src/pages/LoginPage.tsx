import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import PublicLayout from '@/components/layout/PublicLayout';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import { useAuth } from '@/hooks/useAuth';
import { loginSchema, type LoginFormData } from '@/schemas/loginSchema';
import { getErrorMessage } from '@/utils/getErrorMessage';

// Mapa: a qué ruta va cada rol después de iniciar sesión.
// Si mañana agregas un rol nuevo, solo lo añades aquí.
const RUTA_POR_ROL = {
    PACIENTE: '/paciente',
    MEDICO: '/medico',
    ADMINISTRADOR: '/admin',
} as const;

const LoginPage = () => {
    const navigate = useNavigate();
    const { login } = useAuth();
    const [serverError, setServerError] = useState('');

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<LoginFormData>({ resolver: zodResolver(loginSchema) });

    const onSubmit = async (data: LoginFormData) => {
        setServerError('');

        try {
            // login ahora devuelve el usuario, así podemos leer su rol.
            const usuario = await login(data);

            // Redirigimos según el rol. Si por alguna razón el rol no está en el
            // mapa, lo mandamos al login de nuevo por seguridad.
            const destino = RUTA_POR_ROL[usuario.rol] ?? '/login';
            navigate(destino);
        } catch (err) {
            setServerError(getErrorMessage(err, 'Credenciales inválidas'));
        }
    };

    return (
        <PublicLayout showFooter={false}>
            <div className="bg-primary min-h-[calc(100vh-64px)] flex items-center justify-center px-4 py-12">
                <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
                    <div className="flex justify-center mb-4">
                        <div className="text-center">
                            <p className="text-secondary font-bold text-2xl">HEALTHCARE</p>
                            <p className="text-xs text-gray-500">Medical Care</p>
                        </div>
                    </div>
                    <p className="text-center text-gray-500 mb-6">
                        Tu salud primero. Gestiona tus citas aquí.
                    </p>

                    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
                        <Input
                            type="email"
                            placeholder="Correo"
                            autoComplete="email"
                            {...register('correo')}
                            error={errors.correo?.message}
                        />
                        <Input
                            type="password"
                            placeholder="Contraseña"
                            autoComplete="current-password"
                            {...register('contrasenia')}
                            error={errors.contrasenia?.message}
                        />

                        {serverError && (
                            <p className="text-red-500 text-sm text-center" role="alert">
                                {serverError}
                            </p>
                        )}

                        <Button type="submit" className="w-full" isLoading={isSubmitting}>
                            Acceder
                        </Button>
                    </form>

                    <p className="text-center mt-4 text-sm">
                        <Link to="/register" className="text-primary hover:underline">
                            ¿Eres nuevo paciente? Regístrate aquí
                        </Link>
                    </p>
                </div>
            </div>
        </PublicLayout>
    );
};

export default LoginPage;
