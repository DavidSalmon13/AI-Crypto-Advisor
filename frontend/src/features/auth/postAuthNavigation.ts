import axios from 'axios';
import type { NavigateFunction } from 'react-router-dom';
import apiClient from '../../lib/apiClient';

/**
 * specs.md §3.2: after auth, GET /api/preferences — 404 means onboarding hasn't
 * been completed yet, so route there instead of the dashboard.
 */
export async function navigateAfterAuth(navigate: NavigateFunction) {
  try {
    await apiClient.get('/api/preferences');
    navigate('/dashboard', { replace: true });
  } catch (err) {
    if (axios.isAxiosError(err) && err.response?.status === 404) {
      navigate('/onboarding', { replace: true });
    } else {
      navigate('/dashboard', { replace: true });
    }
  }
}
