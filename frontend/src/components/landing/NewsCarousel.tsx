import { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import type { NewsItem } from '@/types/appointment.types';

interface Props {
    items: NewsItem[];
}

const NewsCarousel = ({ items }: Props) => {
    const [index, setIndex] = useState(0);
    const itemsPerView = 3;
    const maxIndex = Math.max(0, items.length - itemsPerView);

    const prev = () => setIndex((i) => Math.max(0, i - 1));
    const next = () => setIndex((i) => Math.min(maxIndex, i + 1));

    return (
        <div className="relative bg-primary rounded-lg p-8">
            <button
                onClick={prev}
                disabled={index === 0}
                className="absolute left-2 top-1/2 -translate-y-1/2 text-white disabled:opacity-30"
                aria-label="Anterior"
            >
                <ChevronLeft size={32} />
            </button>

            <div className="flex gap-4 justify-center overflow-hidden">
                {items.slice(index, index + itemsPerView).map((item) => (
                    <div key={item.id} className="bg-white rounded-md overflow-hidden shadow w-1/3 min-w-0">
                        <img src={item.imagen} alt={item.titulo} className="w-full h-48 object-cover" />
                        <div className="p-3">
                            <p className="text-sm font-semibold">{item.titulo}</p>
                        </div>
                    </div>
                ))}
            </div>

            <button
                onClick={next}
                disabled={index >= maxIndex}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-white disabled:opacity-30"
                aria-label="Siguiente"
            >
                <ChevronRight size={32} />
            </button>

            <div className="flex justify-center gap-2 mt-4">
                {Array.from({ length: maxIndex + 1 }).map((_, i) => (
                    <span
                        key={i}
                        className={`h-1 w-8 rounded ${i === index ? 'bg-white' : 'bg-white/40'}`}
                    />
                ))}
            </div>
        </div>
    );
};

export default NewsCarousel;