import type { ReactNode } from 'react';

interface Props {
    children: ReactNode;
    className?: string;
}

const Card = ({ children, className = '' }: Props) => (
    <div className={`bg-white rounded-2xl border border-gray-200 shadow-sm ${className}`}>{children}</div>
);

export default Card;
