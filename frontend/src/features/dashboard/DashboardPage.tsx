import { useNavigate } from 'react-router-dom';
import { Spinner } from '../../components/Spinner';
import { useAuth } from '../auth/useAuth';
import { AiInsightSection } from './AiInsightSection';
import { CoinPricesSection } from './CoinPricesSection';
import { MarketNewsSection } from './MarketNewsSection';
import { MemeSection } from './MemeSection';
import { useDashboard } from './useDashboard';

const dateFormatter = new Intl.DateTimeFormat('en-US', {
  weekday: 'long',
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  timeZone: 'UTC',
});

export function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { data, isLoading, error, refetch, isFetching } = useDashboard();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="min-h-screen bg-slate-100 p-4 sm:p-8">
      <div className="mx-auto max-w-5xl">
        <header className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">Daily Dashboard</h1>
            {data && (
              <p className="text-sm text-slate-500">{dateFormatter.format(new Date(data.date))}</p>
            )}
            {user && <p className="text-sm text-slate-500">Signed in as {user.email}</p>}
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => navigate('/onboarding')}
              className="rounded border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
            >
              Edit preferences
            </button>
            <button
              type="button"
              onClick={handleLogout}
              className="rounded border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
            >
              Log out
            </button>
          </div>
        </header>

        {isLoading && <Spinner />}

        {error && !isLoading && (
          <div className="rounded-lg bg-white p-6 shadow-sm">
            <p className="text-sm text-slate-700">We couldn&apos;t load your dashboard.</p>
            <button
              type="button"
              onClick={() => refetch()}
              disabled={isFetching}
              className="mt-3 rounded bg-slate-900 px-3 py-1.5 text-sm text-white disabled:opacity-60"
            >
              {isFetching ? 'Retrying…' : 'Try again'}
            </button>
          </div>
        )}

        {data && (
          // Two independent columns rather than one four-cell grid: grid cells stretch to
          // match the tallest sibling in their row, which left Coin Prices as a tall empty
          // card beside the ten-item news list. The split also balances height — the three
          // compact sections together come out close to the news list on their own.
          <div className="grid items-start gap-6 lg:grid-cols-2">
            <div className="flex flex-col gap-6">
              <CoinPricesSection coinPrices={data.coinPrices} />
              <AiInsightSection aiInsight={data.aiInsight} />
              <MemeSection meme={data.meme} />
            </div>
            <div className="flex flex-col gap-6">
              <MarketNewsSection marketNews={data.marketNews} />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
