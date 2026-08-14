import apiClient from '../lib/apiClient';
import type { Dashboard } from '../types/dashboard';

export async function getDashboard(): Promise<Dashboard> {
  const { data } = await apiClient.get<Dashboard>('/api/dashboard');
  return data;
}
