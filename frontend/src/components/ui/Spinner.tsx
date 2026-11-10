interface SpinnerProps {
    fullScreen?: boolean;
}

const Spinner = ({ fullScreen = false }: SpinnerProps) => {
    const container = fullScreen
        ? 'fixed inset-0 flex items-center justify-center bg-white/80 z-50'
        : 'flex items-center justify-center p-4';

    return (
        <div className={container}>
            <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
        </div>
    );
};

export default Spinner;