import { useState } from 'react';
import { Menu, Calendar, LogOut } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { Link, useNavigate } from 'react-router-dom';

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
                        <Link to="/reservar" className="flex items-center gap-3">
                            <Calendar size={20} /> Reservar cita
                        </Link>
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