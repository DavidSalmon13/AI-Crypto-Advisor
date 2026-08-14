import type { CoinPrice } from '../../types/dashboard';

const priceFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 2,
});

function ChangeBadge({ pct }: { pct: number }) {
  const isUp = pct >= 0;
  return (
    <span
      className={`rounded px-2 py-0.5 text-sm font-medium ${
        isUp ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'
      }`}
    >
      {isUp ? '▲' : '▼'} {Math.abs(pct).toFixed(2)}%
    </span>
  );
}

export function CoinPricesSection({ coinPrices }: { coinPrices: CoinPrice[] }) {
  return (
    <section className="rounded-lg bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold text-slate-900">Coin Prices</h2>

      {coinPrices.length === 0 ? (
        <p className="text-sm text-slate-500">
          Live prices are unavailable right now. Please try again shortly.
        </p>
      ) : (
        <ul className="divide-y divide-slate-100">
          {coinPrices.map((coin) => (
            <li key={coin.id} className="flex items-center justify-between py-3">
              <div>
                <p className="font-medium text-slate-900">{coin.name}</p>
                <p className="text-sm text-slate-500">{coin.symbol}</p>
              </div>
              <div className="flex items-center gap-3">
                <span className="font-medium text-slate-900">{priceFormatter.format(coin.priceUsd)}</span>
                <ChangeBadge pct={coin.change24hPct} />
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
