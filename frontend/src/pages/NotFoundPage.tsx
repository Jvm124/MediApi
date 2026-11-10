import { Link } from 'react-router-dom';

const NotFoundPage = () => (
    <div className="min-h-screen flex flex-col items-center justify-center bg-primary text-white">
        <h1 className="text-6xl font-bold">404</h1>
        <p className="mt-2">Página no encontrada</p>
        <Link to="/" className="mt-4 underline">Volver al inicio</Link>
    </div>
);

export default NotFoundPage;