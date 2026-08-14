import apiClient from '../lib/apiClient';
import type { AuthUser } from '../types/auth';

export interface AuthResponse {
  token: string;
  user: AuthUser;
}

export interface RegisterPayload {
  email: string;
  name: string;
  password: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export async function register(payload: RegisterPayload): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/api/auth/register', payload);
  return data;
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/api/auth/login', payload);
  return data;
}
