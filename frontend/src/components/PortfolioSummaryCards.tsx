import type { PortfolioSummary } from '../types/trading';
import { formatPercent, formatUsd } from '../utils/format';

interface PortfolioSummaryCardsProps {
  summary: PortfolioSummary | null;
}

export function PortfolioSummaryCards({ summary }: PortfolioSummaryCardsProps): JSX.Element {
  if (!summary) {
    return (
      <section className="cards-grid">
        <article className="metric-card">
          <h3>Portfolio Summary</h3>
          <p>Summary unavailable.</p>
        </article>
      </section>
    );
  }

  return (
    <section className="cards-grid">
      <article className="metric-card">
        <h3>Total Invested</h3>
        <p>{formatUsd(summary.totalInvestedUsd)}</p>
      </article>
      <article className="metric-card">
        <h3>Current Value</h3>
        <p>{formatUsd(summary.totalCurrentValueUsd)}</p>
      </article>
      <article className="metric-card">
        <h3>Total PnL</h3>
        <p>{formatUsd(summary.totalPnlUsd)}</p>
      </article>
      <article className="metric-card">
        <h3>Unrealized</h3>
        <p>
          {formatUsd(summary.totalUnrealizedPnlUsd)} ({formatPercent(summary.totalUnrealizedPnlPercent)})
        </p>
      </article>
    </section>
  );
}
