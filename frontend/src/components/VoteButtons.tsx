import { useVote } from '../features/dashboard/useVote';
import type { ItemType, Vote } from '../types/dashboard';

interface VoteButtonsProps {
  itemType: ItemType;
  itemRef: string;
  userVote: Vote;
}

const baseButton =
  'rounded border px-2 py-1 text-sm leading-none transition-colors disabled:opacity-50';

export function VoteButtons({ itemType, itemRef, userVote }: VoteButtonsProps) {
  const { mutate, isPending } = useVote();

  // Clicking the vote you already cast retracts it, which is what the DELETE route is for.
  const handleVote = (value: 1 | -1) =>
    mutate({ itemType, itemRef, vote: userVote === value ? null : value });

  return (
    <div className="flex items-center gap-1">
      <button
        type="button"
        onClick={() => handleVote(1)}
        disabled={isPending}
        aria-pressed={userVote === 1}
        aria-label={userVote === 1 ? 'Remove thumbs up' : 'Thumbs up'}
        className={`${baseButton} ${
          userVote === 1
            ? 'border-emerald-300 bg-emerald-50 text-emerald-700'
            : 'border-slate-200 text-slate-500 hover:bg-slate-50'
        }`}
      >
        👍
      </button>
      <button
        type="button"
        onClick={() => handleVote(-1)}
        disabled={isPending}
        aria-pressed={userVote === -1}
        aria-label={userVote === -1 ? 'Remove thumbs down' : 'Thumbs down'}
        className={`${baseButton} ${
          userVote === -1
            ? 'border-rose-300 bg-rose-50 text-rose-700'
            : 'border-slate-200 text-slate-500 hover:bg-slate-50'
        }`}
      >
        👎
      </button>
    </div>
  );
}
