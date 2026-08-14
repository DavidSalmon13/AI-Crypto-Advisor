import { VoteButtons } from '../../components/VoteButtons';
import type { NewsItem } from '../../types/dashboard';

const dateFormatter = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

export function MarketNewsSection({ marketNews }: { marketNews: NewsItem[] }) {
  return (
    <section className="rounded-lg bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold text-slate-900">Market News</h2>

      {marketNews.length === 0 ? (
        <p className="text-sm text-slate-500">No headlines available right now.</p>
      ) : (
        <ul className="space-y-4">
          {marketNews.map((item) => (
            <li key={item.id} className="flex items-start justify-between gap-3">
              <div>
                <a
                  href={item.url}
                  target="_blank"
                  rel="noreferrer noopener"
                  className="font-medium text-slate-900 hover:text-sky-700 hover:underline"
                >
                  {item.title}
                </a>
                <p className="mt-1 text-xs text-slate-500">
                  {item.source} · {dateFormatter.format(new Date(item.publishedAt))}
                </p>
              </div>
              <VoteButtons itemType="NEWS" itemRef={item.id} userVote={item.userVote} />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
