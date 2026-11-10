const HeroSection = () => {
    return (
        <section className="bg-primary text-white relative overflow-hidden">
            <div className="max-w-7xl mx-auto px-6 py-20 grid md:grid-cols-2 gap-8 items-center">
                <div>
                    <h1 className="text-3xl md:text-4xl font-bold mb-4">
                        No más colas para poder registrar una cita
                    </h1>
                    <p className="text-lg text-white/90">
                        En Healthcare podrás agendar tu cita inmediatamente.
                    </p>
                </div>
                <div className="flex justify-center">
                    <img
                        src="/assets/doctores.png"
                        alt="Equipo médico"
                        className="max-h-96 object-contain"
                    />
                </div>
            </div>
        </section>
    );
};

export default HeroSection;