import { Link } from 'react-router-dom';

const UnauthorizedPage = () => {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-white px-4 text-center">
            <h1 className="text-6xl font-bold text-primary mb-2">403</h1>
            <p className="text-lg text-gray-700 mb-6">
                No tienes permiso para acceder a esta página.
            </p>
            <Link
                to="/login"
                className="bg-primary text-white px-6 py-2 rounded-full hover:opacity-90 transition"
            >
                Volver al inicio de sesión
            </Link>
        </div>
    );
};

export default UnauthorizedPage;
