import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import Spinner from '@/components/ui/Spinner';
import type { User } from '@/types/auth.types';

// allowedRoles es OPCIONAL:
//   - Si NO lo pasas  → solo exige estar logueado (como antes).
//   - Si lo pasas     → además exige que el rol del usuario esté en la lista.
interface ProtectedRouteProps {
    allowedRoles?: User['rol'][];
}

const ProtectedRoute = ({ allowedRoles }: ProtectedRouteProps) => {
    const { user, isAuthenticated, loading } = useAuth();

    // 1. Mientras averiguamos si hay sesión, mostramos spinner.
    if (loading) return <Spinner fullScreen />;

    // 2. Si no está logueado → al login.
    if (!isAuthenticated) return <Navigate to="/login" replace />;

    // 3. NUEVO: si la ruta pide roles específicos y el usuario no los tiene → fuera.
    if (allowedRoles && user && !allowedRoles.includes(user.rol)) {
        return <Navigate to="/no-autorizado" replace />;
    }

    // 4. Pasó todos los filtros → renderiza la ruta hija.
    return <Outlet />;
};

export default ProtectedRoute;
