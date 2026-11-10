import PublicLayout from '@/components/layout/PublicLayout';
import HeroSection from '@/components/landing/HeroSection';
import AboutSection from '@/components/landing/AboutSection';
import NewsSection from '@/components/landing/NewsSection';

const LandingPage = () => {
    return (
        <PublicLayout>
            <HeroSection />
            <AboutSection />
            <NewsSection />
        </PublicLayout>
    );
};

export default LandingPage;