import { Phone, Mail } from 'lucide-react';

const Footer = () => {
    return (
        <footer className="bg-primary text-white">
            <div className="max-w-7xl mx-auto px-6 py-12 grid grid-cols-1 md:grid-cols-3 gap-8">
                <div>
                    <div className="bg-white inline-block px-4 py-3 rounded-md mb-4">
                        <p className="text-secondary font-bold text-lg">HEALTHCARE</p>
                        <p className="text-xs text-gray-500">Medical Care</p>
                    </div>
                    <p>Siempre contigo</p>
                </div>

                <div>
                    <h3 className="font-bold text-lg mb-3">Dirección</h3>
                    <p>Av. Lima Metropolitana,</p>
                    <p>Perú</p>
                    <h3 className="font-bold text-lg mt-4 mb-3">Contacto</h3>
                    <p className="flex items-center gap-2"><Phone size={18} /> 945714496</p>
                    <p className="flex items-center gap-2"><Mail size={18} /> healthcare@healthcare.com</p>
                </div>

                <div className="flex items-center justify-center">
                    <img src="/assets/libro_reclamaciones.png" alt="Libro de Reclamaciones" className="max-h-40" />
                </div>
            </div>
            <div className="bg-primary-dark text-center py-3 text-sm">
                © {new Date().getFullYear()} Healthcare. Todos los derechos reservados.
            </div>
        </footer>
    );
};

export default Footer;