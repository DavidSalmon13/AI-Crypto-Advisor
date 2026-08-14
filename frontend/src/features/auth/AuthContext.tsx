import { createContext, useEffect, useState, type ReactNode } from 'react';
import { setAuthToken, setUnauthorizedHandler } from '../../lib/apiClient';
import { queryClient } from '../../lib/queryClient';
import type { AuthUser } from '../../types/auth';

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (token: string, user: AuthUser) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(null);

  const login = (nextToken: string, nextUser: AuthUser) => {
    setAuthToken(nextToken);
    setToken(nextToken);
    setUser(nextUser);
  };

  const logout = () => {
    setAuthToken(null);
    setToken(null);
    setUser(null);
    queryClient.clear();
  };

  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated: token !== null, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
