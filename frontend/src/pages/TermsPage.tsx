import PublicLayout from '@/components/layout/PublicLayout';

// La página a la que lleva la casilla de los términos del registro. Los términos todavía no están escritos,
// así que por ahora solo avisa que se están preparando.
const TermsPage = () => {
    return (
        <PublicLayout>
            <section className="max-w-3xl mx-auto px-6 py-16">
                <h1 className="text-2xl font-bold mb-4">Términos y Condiciones</h1>
                <p className="text-gray-600 leading-relaxed">
                    Estamos preparando los términos y condiciones de MediApi. Cuando estén listos, los encontrarás
                    en esta página.
                </p>
            </section>
        </PublicLayout>
    );
};

export default TermsPage;
