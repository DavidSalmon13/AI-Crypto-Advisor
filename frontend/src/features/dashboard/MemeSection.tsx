import type { Meme } from '../../types/dashboard';

export function MemeSection({ meme }: { meme: Meme }) {
  return (
    <section className="rounded-lg bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold text-slate-900">Fun Crypto Meme</h2>

      <figure>
        <img
          src={meme.imageUrl}
          alt={meme.caption}
          loading="lazy"
          className="w-full rounded-md border border-slate-200 object-cover"
        />
        <figcaption className="mt-3 text-sm text-slate-600">{meme.caption}</figcaption>
      </figure>
    </section>
  );
}
