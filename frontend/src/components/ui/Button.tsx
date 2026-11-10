import { ButtonHTMLAttributes, ReactNode } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: 'primary' | 'secondary' | 'ghost';
    children: ReactNode;
    isLoading?: boolean;
}

const Button = ({
                    variant = 'primary',
                    isLoading = false,
                    children,
                    className = '',
                    disabled,
                    ...rest
                }: ButtonProps) => {
    const base = 'px-6 py-2 rounded-md font-medium transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed';
    const variants = {
        primary: 'bg-primary text-white hover:bg-primary-dark',
        secondary: 'bg-white text-primary border border-primary hover:bg-blue-50',
        ghost: 'bg-transparent text-primary hover:bg-blue-50',
    };

    return (
        <button
            className={`${base} ${variants[variant]} ${className}`}
            disabled={disabled || isLoading}
            {...rest}
        >
            {isLoading ? 'Cargando...' : children}
        </button>
    );
};

export default Button;