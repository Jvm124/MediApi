import { createContext } from 'react';
import type { User, LoginRequest, RegisterRequest } from '@/types/auth.types';

// El contexto de la sesión: lo llena AuthProvider y se lee con useAuth().
// Va en su propio archivo porque Fast Refresh, que al guardar recarga los componentes sin perder su estado,
// solo funciona si un archivo .tsx exporta únicamente componentes (regla react-refresh/only-export-components).
interface AuthContextType {
    user: User | null;
    loading: boolean;
    isAuthenticated: boolean;
    login: (credentials: LoginRequest) => Promise<User>;
    register: (data: RegisterRequest) => Promise<User>;
    logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
