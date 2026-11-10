import { useState } from 'react';
import { Menu, Calendar, FileText, LogOut } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { useNavigate } from 'react-router-dom';

const Sidebar = () => {
    const [open, setOpen] = useState(false);
    const { logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        await logout();
        navigate('/');
    };

    return (
        <>
            <button onClick={() => setOpen(!open)} className="p-2 text-primary" aria-label="Menú">
                <Menu size={28} />
            </button>

            {open && (
                <aside className="fixed inset-y-0 left-0 w-64 bg-primary text-white p-6 z-40 flex flex-col">
                    <button onClick={() => setOpen(false)} className="self-start mb-8" aria-label="Cerrar">
                        <Menu size={28} />
                    </button>

                    <nav className="flex-1 space-y-4">
                        <a href="#" className="flex items-center gap-3"><Calendar size={20} /> Reservar cita</a>
                        <a href="#" className="flex items-center gap-3"><FileText size={20} /> Mis documentos</a>
                    </nav>

                    <button onClick={handleLogout} className="flex items-center gap-3 mt-auto">
                        <LogOut size={20} /> Salir
                    </button>
                </aside>
            )}
        </>
    );
};

export default Sidebar;