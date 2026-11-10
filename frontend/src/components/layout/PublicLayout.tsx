import { ReactNode } from 'react';
import Navbar from './Navbar';
import Footer from './Footer';

interface Props {
    children: ReactNode;
    showFooter?: boolean;
}

const PublicLayout = ({ children, showFooter = true }: Props) => {
    return (
        <div className="min-h-screen flex flex-col">
            <Navbar />
            <main className="flex-1">{children}</main>
            {showFooter && <Footer />}
        </div>
    );
};

export default PublicLayout;