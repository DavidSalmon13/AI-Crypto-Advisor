import { useQuery } from '@tanstack/react-query';
import { getDashboard } from '../../api/dashboardApi';

/**
 * The whole dashboard comes from one aggregating call (specs.md §4.3), so one query key
 * backs all four sections — and Phase 6's optimistic vote updates will patch this cache.
 */
export function useDashboard() {
  return useQuery({
    queryKey: ['dashboard'],
    queryFn: getDashboard,
  });
}
