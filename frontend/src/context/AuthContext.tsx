import { createContext, useState, useEffect, type ReactNode } from 'react';
import { authApi } from '@/api/authApi';
import { tokenStorage } from '@/utils/tokenStorage';
import type { User, LoginRequest, RegisterRequest } from '@/types/auth.types';

interface AuthContextType {
    user: User | null;
    loading: boolean;
    isAuthenticated: boolean;
    login: (credentials: LoginRequest) => Promise<User>;
    register: (data: RegisterRequest) => Promise<User>;
    logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
    const [user, setUser] = useState<User | null>(null);
    // El estado inicial ya depende de si hay token: si no hay, no hay nada que cargar.
    const [loading, setLoading] = useState(() => tokenStorage.get() !== null);

    useEffect(() => {
        const token = tokenStorage.get();
        if (!token) {
            return;
        }
        authApi.getMe()
            .then(setUser)
            .catch(() => {
                tokenStorage.remove();
                setUser(null);
            })
            .finally(() => setLoading(false));
    }, []);

    const register = async (data: RegisterRequest): Promise<User> => {
        await authApi.registrarPaciente({
            nombre: data.nombreCompleto,
            email: data.email,
            telefono: data.telefono,
            documento: data.dni,
            direccion: data.direccion,
            contrasenia: data.password,
        });
        return login({ correo: data.email, contrasenia: data.password });
    };

    const login = async (credentials: LoginRequest): Promise<User> => {
        const { token } = await authApi.login(credentials);
        tokenStorage.set(token);
        const usuario = await authApi.getMe();
        setUser(usuario);
        return usuario;
    };

    const logout = async () => {
        try {
            await authApi.logout();
        } catch {
            /* ignore */
        } finally {
            tokenStorage.remove();
            setUser(null);
        }
    };

    return (
        <AuthContext.Provider value={{ user, loading, isAuthenticated: !!user, login, register, logout }}>
            {children}
        </AuthContext.Provider>
    );
};
