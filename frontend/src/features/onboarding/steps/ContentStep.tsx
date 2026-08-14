import type { ContentType } from '../../../types/preferences';

const LABELS: Record<ContentType, string> = {
  MARKET_NEWS: 'Market news',
  CHARTS: 'Charts & analysis',
  SOCIAL: 'Social & community',
  FUN: 'Fun & memes',
};

interface ContentStepProps {
  options: ContentType[];
  selected: ContentType[];
  onChange: (contentTypes: ContentType[]) => void;
}

export function ContentStep({ options, selected, onChange }: ContentStepProps) {
  const toggle = (contentType: ContentType) => {
    if (selected.includes(contentType)) {
      onChange(selected.filter((c) => c !== contentType));
    } else {
      onChange([...selected, contentType]);
    }
  };

  return (
    <div>
      <h2 className="mb-1 text-lg font-medium text-slate-900">What content would you like to see?</h2>
      <p className="mb-4 text-sm text-slate-500">Pick at least one.</p>
      <div className="flex flex-col gap-2">
        {options.map((option) => {
          const isSelected = selected.includes(option);
          return (
            <button
              key={option}
              type="button"
              onClick={() => toggle(option)}
              className={`rounded border px-4 py-3 text-left text-sm ${
                isSelected
                  ? 'border-slate-900 bg-slate-900 text-white'
                  : 'border-slate-300 bg-white text-slate-700'
              }`}
            >
              {LABELS[option]}
            </button>
          );
        })}
      </div>
    </div>
  );
}
