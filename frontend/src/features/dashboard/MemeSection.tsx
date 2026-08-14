import { VoteButtons } from '../../components/VoteButtons';
import type { Meme } from '../../types/dashboard';

export function MemeSection({ meme }: { meme: Meme }) {
  return (
    <section className="rounded-lg bg-white p-6 shadow-sm">
      <figure>
        <img
          src={meme.imageUrl}
          alt={meme.caption}
          loading="lazy"
          className="w-full rounded-md border border-slate-200 object-cover"
        />
        <figcaption className="mt-3 flex items-center justify-between gap-3 text-sm text-slate-600">
          <span>{meme.caption}</span>
          <VoteButtons itemType="MEME" itemRef={meme.id} userVote={meme.userVote} />
        </figcaption>
      </figure>
    </section>
  );
}
