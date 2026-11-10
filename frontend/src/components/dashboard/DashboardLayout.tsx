import { useState, type ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { Menu, X, LogOut, type LucideIcon } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';

export interface NavItem {
    label: string;
    to: string;
    icon: LucideIcon;
}

interface Props {
    title: string;
    navItems: NavItem[];
    children: ReactNode;
}

const DashboardLayout = ({ title, navItems, children }: Props) => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [open, setOpen] = useState(false);

    const handleLogout = async () => {
        await logout();
        navigate('/');
    };

    const iniciales =
        user?.nombreCompleto
            ?.split(' ')
            .slice(0, 2)
            .map((p) => p[0])
            .join('')
            .toUpperCase() ?? '';

    const inner = (
        <div className="flex flex-col h-full">
            <div className="flex items-center gap-2 font-extrabold text-lg mb-6 px-1">
                <span className="w-8 h-8 rounded-lg bg-primary text-white flex items-center justify-center">V</span>
                Voll.med
            </div>

            <nav className="flex-1 space-y-1">
                {navItems.map(({ label, to, icon: Icon }) => (
                    <NavLink
                        key={to}
                        to={to}
                        end
                        onClick={() => setOpen(false)}
                        className={({ isActive }) =>
                            `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                                isActive ? 'bg-primary/10 text-primary-dark' : 'text-gray-500 hover:bg-gray-100'
                            }`
                        }
                    >
                        <Icon size={18} /> {label}
                    </NavLink>
                ))}
            </nav>

            <button
                onClick={handleLogout}
                className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-gray-500 hover:bg-gray-100 mt-auto"
            >
                <LogOut size={18} /> Cerrar sesión
            </button>
        </div>
    );

    return (
        <div className="min-h-screen bg-gray-50">
            <aside className="hidden lg:flex flex-col fixed inset-y-0 left-0 w-60 bg-white border-r border-gray-200 p-4">
                {inner}
            </aside>

            {open && (
                <>
                    <div className="fixed inset-0 bg-black/30 z-40 lg:hidden" onClick={() => setOpen(false)} />
                    <aside className="fixed inset-y-0 left-0 w-64 bg-white p-4 z-50 lg:hidden shadow-xl">
                        <button onClick={() => setOpen(false)} className="mb-4 text-gray-500" aria-label="Cerrar menú">
                            <X size={22} />
                        </button>
                        {inner}
                    </aside>
                </>
            )}

            <div className="lg:pl-60">
                <header className="sticky top-0 z-30 bg-white/80 backdrop-blur border-b border-gray-200 px-4 md:px-8 py-3 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <button onClick={() => setOpen(true)} className="lg:hidden text-gray-600" aria-label="Abrir menú">
                            <Menu size={24} />
                        </button>
                        <h1 className="text-lg font-bold">{title}</h1>
                    </div>
                    <div className="w-9 h-9 rounded-full bg-gradient-to-br from-primary to-primary-light text-white flex items-center justify-center text-sm font-bold">
                        {iniciales}
                    </div>
                </header>

                <main className="p-4 md:p-8 max-w-5xl">{children}</main>
            </div>
        </div>
    );
};

export default DashboardLayout;
