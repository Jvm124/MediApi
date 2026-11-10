import { Routes, Route } from 'react-router-dom';
import LandingPage from '@/pages/LandingPage';
import LoginPage from '@/pages/LoginPage';
import RegisterPage from '@/pages/RegisterPage';
import DashboardPage from '@/pages/DashboardPage';
import MedicoDashboardPage from '@/pages/MedicoDashboardPage';
import AdminDashboardPage from '@/pages/AdminDashboardPage';
import UnauthorizedPage from '@/pages/UnauthorizedPage';
import NotFoundPage from '@/pages/NotFoundPage';
import ProtectedRoute from './ProtectedRoute';
import ReservarCitaPage from '@/pages/ReservarCitaPage';
import AdminMedicosPage from '@/pages/AdminMedicosPage';
import MedicoPerfilPage from "@/pages/MedicoPerfilPage.tsx";

const AppRoutes = () => {
    return (
        <Routes>
            {/* Rutas públicas: cualquiera entra */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/no-autorizado" element={<UnauthorizedPage />} />

            {/* Solo PACIENTE */}
            <Route element={<ProtectedRoute allowedRoles={['PACIENTE']} />}>
                <Route path="/paciente" element={<DashboardPage />} />
                <Route path="/reservar" element={<ReservarCitaPage />} />
            </Route>

            {/* Solo MEDICO */}
            <Route element={<ProtectedRoute allowedRoles={['MEDICO']} />}>
                <Route path="/medico" element={<MedicoDashboardPage />} />
                <Route path="/medico/perfil" element={<MedicoPerfilPage />} />
            </Route>

            {/* Solo ADMIN */}
            <Route element={<ProtectedRoute allowedRoles={['ADMINISTRADOR']} />}>
                <Route path="/admin" element={<AdminDashboardPage />} />
                <Route path="/admin/medicos" element={<AdminMedicosPage />} />
            </Route>

            {/* Cualquier otra URL → 404 */}
            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    );
};

export default AppRoutes;
