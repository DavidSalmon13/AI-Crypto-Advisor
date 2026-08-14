import { useMutation, useQueryClient } from '@tanstack/react-query';
import { castVote, retractVote } from '../../api/feedbackApi';
import type { Dashboard, ItemType, Vote } from '../../types/dashboard';

const DASHBOARD_KEY = ['dashboard'];

export interface VoteInput {
  itemType: ItemType;
  itemRef: string;
  /** null retracts an existing vote. */
  vote: Vote;
}

/** Returns a copy of the dashboard with one item's userVote replaced. */
function applyVote(dashboard: Dashboard, { itemType, itemRef, vote }: VoteInput): Dashboard {
  switch (itemType) {
    case 'NEWS':
      return {
        ...dashboard,
        marketNews: dashboard.marketNews.map((item) =>
          item.id === itemRef ? { ...item, userVote: vote } : item,
        ),
      };
    case 'AI_INSIGHT':
      return dashboard.aiInsight.id === itemRef
        ? { ...dashboard, aiInsight: { ...dashboard.aiInsight, userVote: vote } }
        : dashboard;
    case 'MEME':
      return dashboard.meme.id === itemRef
        ? { ...dashboard, meme: { ...dashboard.meme, userVote: vote } }
        : dashboard;
    default:
      return dashboard;
  }
}

/**
 * Optimistic voting against the cached dashboard.
 *
 * There is deliberately no invalidation on success: refetching the dashboard would re-run
 * the whole aggregation, including a live CoinGecko call, to learn one field we already
 * know. A failed vote rolls the cache back instead.
 */
export function useVote() {
  const queryClient = useQueryClient();

  return useMutation({
    // Returns void either way: the optimistic cache patch already holds the new state, so
    // the server's echo of it is not needed.
    mutationFn: async ({ itemType, itemRef, vote }: VoteInput): Promise<void> => {
      if (vote === null) {
        await retractVote(itemType, itemRef);
        return;
      }
      await castVote(itemType, itemRef, vote);
    },

    onMutate: async (input: VoteInput) => {
      await queryClient.cancelQueries({ queryKey: DASHBOARD_KEY });
      const previous = queryClient.getQueryData<Dashboard>(DASHBOARD_KEY);

      if (previous) {
        queryClient.setQueryData<Dashboard>(DASHBOARD_KEY, applyVote(previous, input));
      }

      return { previous };
    },

    onError: (_error, _input, context) => {
      if (context?.previous) {
        queryClient.setQueryData<Dashboard>(DASHBOARD_KEY, context.previous);
      }
    },
  });
}
