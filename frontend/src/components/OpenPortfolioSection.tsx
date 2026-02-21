import { useMemo } from 'react';
import { useAssetSpotPrices } from '../hooks/useAssetSpotPrices';
import type { AssetSummary } from '../types/trading';
import { formatNumber, formatUsd } from '../utils/format';

interface OpenPortfolioSectionProps {
  assetSummary: AssetSummary[];
}

export function OpenPortfolioSection({ assetSummary }: OpenPortfolioSectionProps): JSX.Element {
  const openRows = useMemo(() => {
    return assetSummary
      .map((row) => ({
        assetName: row.assetName,
        symbol: row.assetSymbol.toUpperCase(),
        netAmount: Number.isFinite(Number(row.netQuantity)) ? Number(row.netQuantity) : 0,
        usdInvested: Number.isFinite(Number(row.totalInvested)) ? Number(row.totalInvested) : 0,
        realizedProfit: Number.isFinite(Number(row.totalRealizedProfit)) ? Number(row.totalRealizedProfit) : 0
      }))
      .sort((a, b) => a.symbol.localeCompare(b.symbol));
  }, [assetSummary]);

  const symbols = useMemo(() => openRows.map((row) => row.symbol), [openRows]);
  const pricesBySymbol = useAssetSpotPrices(symbols);

  const valuationRows = useMemo(
    () =>
      openRows.map((row) => {
        const priceState = pricesBySymbol[row.symbol];
        const currentPrice =
          priceState?.status === 'success' && priceState.priceUsd && Number.isFinite(Number(priceState.priceUsd))
            ? Number(priceState.priceUsd)
            : null;
        const currentValue = row.netAmount === 0 ? 0 : currentPrice === null ? null : row.netAmount * currentPrice;
        return {
          ...row,
          priceState,
          currentPrice,
          currentValue
        };
      }),
    [openRows, pricesBySymbol]
  );

  const summary = useMemo(() => {
    const totalInvested = valuationRows.reduce((sum, row) => sum + row.usdInvested, 0);
    const allValuesKnown = valuationRows.every((row) => row.currentValue !== null);
    const totalMarketValue = allValuesKnown
      ? valuationRows.reduce((sum, row) => sum + (row.currentValue ?? 0), 0)
      : null;

    return { totalInvested, totalMarketValue };
  }, [valuationRows]);

  const summaryClassName =
    summary.totalMarketValue === null
      ? ''
      : summary.totalMarketValue >= summary.totalInvested
        ? 'pnl-positive'
        : 'pnl-negative';

  return (
    <section className="history-panel history-panel-prominent">
      <h3>Portfolio Summary</h3>
      <div className="portfolio-summary-inline">
        <div className="metric-card">
          <h3>Total Invested</h3>
          <p>{formatUsd(String(summary.totalInvested))}</p>
        </div>
        <div className="metric-card">
          <h3>Current Market Value</h3>
          <p className={summaryClassName}>
            {summary.totalMarketValue === null ? '---' : formatUsd(String(summary.totalMarketValue))}
          </p>
        </div>
      </div>

      {valuationRows.length === 0 ? (
        <p>No portfolio history yet.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Asset</th>
                <th>Net Amount</th>
                <th>USD Invested</th>
                <th>Current Price</th>
                <th>Current Value</th>
                <th>Realized Profit</th>
              </tr>
            </thead>
            <tbody>
              {valuationRows.map((row) => {
                const diff =
                  row.currentValue === null || !Number.isFinite(row.currentValue)
                    ? null
                    : row.currentValue - row.usdInvested;
                const valueClassName = diff === null ? '' : diff >= 0 ? 'pnl-positive' : 'pnl-negative';
                const realizedClassName =
                  row.realizedProfit > 0 ? 'pnl-positive' : row.realizedProfit < 0 ? 'pnl-negative' : '';

                return (
                  <tr key={row.symbol}>
                    <td title={row.assetName}>{row.symbol}</td>
                    <td>{formatNumber(String(row.netAmount))}</td>
                    <td>{formatUsd(String(row.usdInvested))}</td>
                    <td>
                      {row.netAmount === 0
                        ? '---'
                        : !row.priceState || row.priceState.status === 'loading'
                          ? <span className="table-price-loading" aria-label="Loading current price" />
                          : row.priceState.status === 'error' || row.currentPrice === null
                            ? '---'
                            : formatUsd(String(row.currentPrice))}
                    </td>
                    <td className={valueClassName}>
                      {row.currentValue === null ? '---' : formatUsd(String(row.currentValue))}
                    </td>
                    <td className={realizedClassName}>{formatUsd(String(row.realizedProfit))}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
