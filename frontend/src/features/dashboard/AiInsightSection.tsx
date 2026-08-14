import type { AiInsight } from '../../types/dashboard';

export function AiInsightSection({ aiInsight }: { aiInsight: AiInsight }) {
  return (
    <section className="rounded-lg bg-white p-6 shadow-sm">
      <div className="mb-4 flex items-center gap-3">
        <h2 className="text-lg font-semibold text-slate-900">AI Insight of the Day</h2>
        {aiInsight.fallback && (
          <span
            className="rounded bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-700"
            title="The AI service was unavailable, so a standard message is shown instead."
          >
            Unavailable
          </span>
        )}
      </div>

      <p className="whitespace-pre-line leading-relaxed text-slate-700">{aiInsight.text}</p>
    </section>
  );
}
