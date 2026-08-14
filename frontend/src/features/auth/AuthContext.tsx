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

const STORAGE_KEY = 'aicryptoadvisor.auth';

interface StoredAuth {
  token: string;
  user: AuthUser;
}

/**
 * A stored token isn't re-validated here — an expired/invalid one simply fails the first
 * authenticated request, and the apiClient's 401 interceptor calls logout() to clear it.
 */
function loadStoredAuth(): StoredAuth | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredAuth) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  // Lazy initializers run once, synchronously, before the first render — so apiClient already
  // has the token attached before any query (e.g. RequirePreferences' GET /api/preferences)
  // can fire on mount.
  const [token, setToken] = useState<string | null>(() => {
    const stored = loadStoredAuth();
    setAuthToken(stored?.token ?? null);
    return stored?.token ?? null;
  });
  const [user, setUser] = useState<AuthUser | null>(() => loadStoredAuth()?.user ?? null);

  const login = (nextToken: string, nextUser: AuthUser) => {
    setAuthToken(nextToken);
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: nextToken, user: nextUser }));
    setToken(nextToken);
    setUser(nextUser);
  };

  const logout = () => {
    setAuthToken(null);
    localStorage.removeItem(STORAGE_KEY);
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
