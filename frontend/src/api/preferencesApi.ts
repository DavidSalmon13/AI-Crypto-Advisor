import apiClient from '../lib/apiClient';
import type { Preferences, PreferencesOptions } from '../types/preferences';

export async function getOptions(): Promise<PreferencesOptions> {
  const { data } = await apiClient.get<PreferencesOptions>('/api/preferences/options');
  return data;
}

export async function getPreferences(): Promise<Preferences> {
  const { data } = await apiClient.get<Preferences>('/api/preferences');
  return data;
}

export async function savePreferences(preferences: Preferences): Promise<Preferences> {
  const { data } = await apiClient.put<Preferences>('/api/preferences', preferences);
  return data;
}
