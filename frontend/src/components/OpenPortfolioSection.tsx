import { useMemo } from 'react';
import { useAssetSpotPrices } from '../hooks/useAssetSpotPrices';
import type { AssetSummary, DashboardGroupingMode, PortfolioAssetPerformance } from '../types/trading';
import { formatNumber, formatUsd } from '../utils/format';

interface OpenPortfolioSectionProps {
  assetSummary: AssetSummary[];
  performance: PortfolioAssetPerformance[];
  groupingMode: DashboardGroupingMode;
}

function formatPercent(value: number): string {
  return `${formatNumber(value.toFixed(2))}%`;
}

export function OpenPortfolioSection({ assetSummary, performance, groupingMode }: OpenPortfolioSectionProps): JSX.Element {
  const assetRows = useMemo(() => {
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

  const symbols = useMemo(() => assetRows.map((row) => row.symbol), [assetRows]);
  const pricesBySymbol = useAssetSpotPrices(symbols);

  const assetValuationRows = useMemo(
    () =>
      assetRows.map((row) => {
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
    [assetRows, pricesBySymbol]
  );

  const assetSummaryTotals = useMemo(() => {
    const totalInvested = assetValuationRows.reduce((sum, row) => sum + row.usdInvested, 0);
    const allValuesKnown = assetValuationRows.every((row) => row.currentValue !== null);
    const totalMarketValue = allValuesKnown
      ? assetValuationRows.reduce((sum, row) => sum + (row.currentValue ?? 0), 0)
      : null;

    return { totalInvested, totalMarketValue };
  }, [assetValuationRows]);

  const exchangeData = useMemo(() => {
    const openRows = performance
      .map((row) => {
        const currentBalance = Number(row.currentBalance);
        const currentValueUsd = Number(row.currentValueUsd);

        return {
          exchangeName: row.exchange?.trim() || 'Unknown Exchange',
          currentBalance: Number.isFinite(currentBalance) ? currentBalance : 0,
          currentValueUsd: Number.isFinite(currentValueUsd) ? currentValueUsd : 0
        };
      })
      .filter((row) => row.currentBalance > 0);

    const groupedByExchange = new Map<string, number>();
    for (const row of openRows) {
      groupedByExchange.set(row.exchangeName, (groupedByExchange.get(row.exchangeName) ?? 0) + row.currentValueUsd);
    }

    const globalTotalValue = Array.from(groupedByExchange.values()).reduce((sum, value) => sum + value, 0);
    const exchangeRows = Array.from(groupedByExchange.entries())
      .map(([exchangeName, totalValueUsd]) => ({
        exchangeName,
        totalValueUsd,
        portfolioPercent: globalTotalValue <= 0 ? 0 : (totalValueUsd / globalTotalValue) * 100
      }))
      .sort((left, right) => right.totalValueUsd - left.totalValueUsd);

    return { exchangeRows, globalTotalValue };
  }, [performance]);

  const activeAssetCount = useMemo(
    () => assetValuationRows.filter((row) => Number.isFinite(row.netAmount) && row.netAmount > 0).length,
    [assetValuationRows]
  );
  const activeExchangeCount = exchangeData.exchangeRows.length;

  const displayedMarketValue =
    groupingMode === 'EXCHANGE' ? exchangeData.globalTotalValue : assetSummaryTotals.totalMarketValue;

  const summaryClassName =
    displayedMarketValue === null
      ? ''
      : displayedMarketValue >= assetSummaryTotals.totalInvested
        ? 'pnl-positive'
        : 'pnl-negative';

  return (
    <section className="history-panel history-panel-prominent">
      <h3>Portfolio Summary</h3>
      <div className="portfolio-summary-inline">
        <div className="metric-card">
          <h3>Total Invested</h3>
          <p>{formatUsd(String(assetSummaryTotals.totalInvested))}</p>
        </div>
        <div className="metric-card">
          <h3>Current Market Value</h3>
          <p className={summaryClassName}>
            {displayedMarketValue === null ? '---' : formatUsd(String(displayedMarketValue))}
          </p>
        </div>
        <div className="metric-card">
          <h3>{groupingMode === 'EXCHANGE' ? 'Active Exchanges' : 'Active Assets'}</h3>
          <p>{groupingMode === 'EXCHANGE' ? activeExchangeCount : activeAssetCount}</p>
        </div>
      </div>

      {groupingMode === 'ASSET' && assetValuationRows.length === 0 ? <p>No portfolio history yet.</p> : null}
      {groupingMode === 'EXCHANGE' && exchangeData.exchangeRows.length === 0 ? <p>No open positions by exchange yet.</p> : null}

      {groupingMode === 'ASSET' && assetValuationRows.length > 0 ? (
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
              {assetValuationRows.map((row) => {
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
      ) : null}

      {groupingMode === 'EXCHANGE' && exchangeData.exchangeRows.length > 0 ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Exchange Name</th>
                <th>Total Value (USD)</th>
                <th>Percentage of Portfolio</th>
              </tr>
            </thead>
            <tbody>
              {exchangeData.exchangeRows.map((row) => (
                <tr key={row.exchangeName}>
                  <td>{row.exchangeName}</td>
                  <td>{formatUsd(String(row.totalValueUsd))}</td>
                  <td>{formatPercent(row.portfolioPercent)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}
