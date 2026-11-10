const AboutSection = () => {
    return (
        <section id="about" className="py-16 bg-white">
            <div className="max-w-7xl mx-auto px-6">
                <h2 className="text-2xl font-bold mb-8">Sobre Nosotros</h2>
                <div className="grid md:grid-cols-2 gap-12 items-center">
                    <div className="space-y-6">
                        <div>
                            <h3 className="text-xl font-bold mb-2">Misión</h3>
                            <p className="text-gray-600 leading-relaxed">
                                Brindar a nuestros pacientes una atención médica de calidad mediante una plataforma
                                ágil que elimine las colas y facilite el acceso a citas con especialistas certificados.
                            </p>
                        </div>
                        <div>
                            <h3 className="text-xl font-bold mb-2">Visión</h3>
                            <p className="text-gray-600 leading-relaxed">
                                Ser la plataforma líder en gestión de citas médicas en el Perú, integrando tecnología
                                e inteligencia artificial para mejorar la experiencia del paciente.
                            </p>
                        </div>
                    </div>
                    <div className="flex justify-center">
                        <img src="/assets/about.png" alt="Atención médica" className="max-h-80" />
                    </div>
                </div>
            </div>
        </section>
    );
};

export default AboutSection;