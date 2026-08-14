import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { Navigate, Outlet } from 'react-router-dom';
import { getPreferences } from '../api/preferencesApi';
import { Spinner } from '../components/Spinner';

/**
 * specs.md §3.2 routing gate: authenticated routes other than /onboarding require
 * completed preferences — GET /api/preferences 404 forces a redirect to /onboarding.
 */
export function RequirePreferences() {
  const { isLoading, error } = useQuery({
    queryKey: ['preferences'],
    queryFn: getPreferences,
    retry: false,
  });

  if (isLoading) {
    return <Spinner />;
  }

  if (axios.isAxiosError(error) && error.response?.status === 404) {
    return <Navigate to="/onboarding" replace />;
  }

  return <Outlet />;
}
