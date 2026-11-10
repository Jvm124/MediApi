import { Link, NavLink } from 'react-router-dom';

const Navbar = () => {
    const linkClass = ({ isActive }: { isActive: boolean }) =>
        `text-gray-700 hover:text-primary transition ${isActive ? 'text-primary font-semibold' : ''}`;

    return (
        <header className="bg-white shadow-sm sticky top-0 z-40">
            <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
                <Link to="/" className="flex items-center gap-2">
                    <img src="/assets/logo.png" alt="Healthcare" className="h-10" />
                    <div className="leading-tight">
                        <p className="text-secondary font-bold text-lg">HEALTHCARE</p>
                        <p className="text-xs text-gray-500">Medical Care</p>
                    </div>
                </Link>

                <ul className="hidden md:flex items-center gap-8">
                    <li><NavLink to="/" end className={linkClass}>Inicio</NavLink></li>
                    <li><a href="#about" className="text-gray-700 hover:text-primary">Sobre nosotros</a></li>
                    <li><a href="#news" className="text-gray-700 hover:text-primary">Noticias</a></li>
                </ul>

                <Link to="/login" className="btn-primary">Ingresar</Link>
            </nav>
        </header>
    );
};

export default Navbar;