import NewsCarousel from './NewsCarousel';
import type { NewsItem } from '@/types/appointment.types';

const mockNews: NewsItem[] = [
    { id: 1, titulo: 'Más de 3.400 horas médicas se perdieron en 2025', imagen: '/assets/Noticia1.png' },
    { id: 2, titulo: 'Hospital de Huaral implementa entrega diaria de citas médicas', imagen: '/assets/Noticia2.png' },
    { id: 3, titulo: 'EsSalud La Libertad implementa triaje con IA', imagen: '/assets/Noticia3.png' },
    { id: 4, titulo: 'Citas médicas en Essalud demoran hasta tres meses para atención', imagen: '/assets/Noticia4.jpg' },
    { id: 5, titulo: 'EsSalud La Libertad implementa triaje con IA', imagen: '/assets/Noticia5.png' },
];

const NewsSection = () => {
    return (
        <section id="news" className="py-16 bg-white">
            <div className="max-w-7xl mx-auto px-6">
                <h2 className="text-2xl font-bold mb-8">Noticias</h2>
                <NewsCarousel items={mockNews} />
            </div>
        </section>
    );
};

export default NewsSection;