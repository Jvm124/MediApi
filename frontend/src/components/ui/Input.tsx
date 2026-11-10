import { InputHTMLAttributes, forwardRef } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
    label?: string;
    error?: string;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
    ({ label, error, className = '', ...rest }, ref) => {
        return (
            <div className="w-full">
                {label && <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>}
                <input
                    ref={ref}
                    className={`input-field ${error ? 'border-red-500' : ''} ${className}`}
                    {...rest}
                />
                {error && <p className="text-red-500 text-xs mt-1">{error}</p>}
            </div>
        );
    }
);
Input.displayName = 'Input';
export default Input;