import type { InvestorType } from '../../../types/preferences';

const LABELS: Record<InvestorType, string> = {
  HODLER: 'HODLer — long-term holder',
  DAY_TRADER: 'Day trader — in and out daily',
  NFT_COLLECTOR: 'NFT collector — digital collectibles first',
};

interface InvestorTypeStepProps {
  options: InvestorType[];
  selected: InvestorType | null;
  onChange: (investorType: InvestorType) => void;
}

export function InvestorTypeStep({ options, selected, onChange }: InvestorTypeStepProps) {
  return (
    <div>
      <h2 className="mb-1 text-lg font-medium text-slate-900">What kind of investor are you?</h2>
      <p className="mb-4 text-sm text-slate-500">Pick one.</p>
      <div className="flex flex-col gap-2">
        {options.map((option) => (
          <button
            key={option}
            type="button"
            onClick={() => onChange(option)}
            className={`rounded border px-4 py-3 text-left text-sm ${
              selected === option
                ? 'border-slate-900 bg-slate-900 text-white'
                : 'border-slate-300 bg-white text-slate-700'
            }`}
          >
            {LABELS[option]}
          </button>
        ))}
      </div>
    </div>
  );
}
