import type { CoinOption } from '../../../types/preferences';

interface AssetsStepProps {
  coins: CoinOption[];
  selected: string[];
  onChange: (interests: string[]) => void;
}

export function AssetsStep({ coins, selected, onChange }: AssetsStepProps) {
  const toggle = (coinId: string) => {
    if (selected.includes(coinId)) {
      onChange(selected.filter((id) => id !== coinId));
    } else if (selected.length < 10) {
      onChange([...selected, coinId]);
    }
  };

  return (
    <div>
      <h2 className="mb-1 text-lg font-medium text-slate-900">Which crypto assets interest you?</h2>
      <p className="mb-4 text-sm text-slate-500">Pick 1–10 coins. Selected: {selected.length}</p>
      <div className="grid max-h-80 grid-cols-2 gap-2 overflow-y-auto sm:grid-cols-3">
        {coins.map((coin) => {
          const isSelected = selected.includes(coin.id);
          return (
            <button
              key={coin.id}
              type="button"
              onClick={() => toggle(coin.id)}
              className={`rounded border px-3 py-2 text-left text-sm ${
                isSelected
                  ? 'border-slate-900 bg-slate-900 text-white'
                  : 'border-slate-300 bg-white text-slate-700'
              }`}
            >
              <span className="font-medium">{coin.symbol}</span>
              <span className="ml-1.5 text-xs opacity-70">{coin.name}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
