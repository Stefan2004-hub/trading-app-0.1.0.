import { useMemo } from 'react';
import { useAssetSpotPrices } from '../hooks/useAssetSpotPrices';
import type { AssetPriceState } from '../hooks/useAssetSpotPrices';
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

interface BasePerformanceRow {
  symbol: string;
  exchange: string;
  currentBalance: number;
  totalInvestedUsd: number;
  realizedPnlUsd: number;
}

interface AssetViewRow {
  assetName: string;
  symbol: string;
  netAmount: number;
  usdInvested: number;
  realizedProfit: number;
  currentPrice: number | null;
  currentValue: number | null;
  priceState?: AssetPriceState;
}

interface ExchangeViewRow {
  exchangeName: string;
  totalValueUsd: number | null;
  realizedProfitUsd: number;
  portfolioPercent: number | null;
}

export function OpenPortfolioSection({ assetSummary, performance, groupingMode }: OpenPortfolioSectionProps): JSX.Element {
  const assetNameBySymbol = useMemo(() => {
    const map = new Map<string, string>();
    for (const row of assetSummary) {
      map.set(row.assetSymbol.toUpperCase(), row.assetName);
    }
    return map;
  }, [assetSummary]);

  const baseRows = useMemo<BasePerformanceRow[]>(() => {
    return performance.map((row) => ({
      symbol: row.symbol.toUpperCase(),
      exchange: row.exchange?.trim() || 'Unknown Exchange',
      currentBalance: Number.isFinite(Number(row.currentBalance)) ? Number(row.currentBalance) : 0,
      totalInvestedUsd: Number.isFinite(Number(row.totalInvestedUsd)) ? Number(row.totalInvestedUsd) : 0,
      realizedPnlUsd: Number.isFinite(Number(row.realizedPnlUsd)) ? Number(row.realizedPnlUsd) : 0
    }));
  }, [performance]);

  const symbols = useMemo(
    () => Array.from(new Set(baseRows.filter((row) => row.currentBalance > 0).map((row) => row.symbol))),
    [baseRows]
  );
  const pricesBySymbol = useAssetSpotPrices(symbols);

  const rowValuations = useMemo(() => {
    return baseRows.map((row) => {
      if (row.currentBalance <= 0) {
        return {
          ...row,
          currentPrice: null,
          priceState: undefined,
          currentValue: 0 as number | null
        };
      }

      const priceState = pricesBySymbol[row.symbol];
      const currentPrice =
        priceState?.status === 'success' && priceState.priceUsd && Number.isFinite(Number(priceState.priceUsd))
          ? Number(priceState.priceUsd)
          : null;

      return {
        ...row,
        currentPrice,
        priceState,
        currentValue: currentPrice === null ? null : row.currentBalance * currentPrice
      };
    });
  }, [baseRows, pricesBySymbol]);

  const totalInvested = useMemo(
    () => rowValuations.reduce((sum, row) => sum + row.totalInvestedUsd, 0),
    [rowValuations]
  );

  const assetRows = useMemo<AssetViewRow[]>(() => {
    const grouped = new Map<
      string,
      {
        assetName: string;
        symbol: string;
        netAmount: number;
        usdInvested: number;
        realizedProfit: number;
        currentPrice: number | null;
        currentValueSum: number;
        hasUnknownOpenValue: boolean;
      }
    >();

    for (const row of rowValuations) {
      const existing = grouped.get(row.symbol);
      const next =
        existing ?? {
          assetName: assetNameBySymbol.get(row.symbol) ?? row.symbol,
          symbol: row.symbol,
          netAmount: 0,
          usdInvested: 0,
          realizedProfit: 0,
          currentPrice: null,
          currentValueSum: 0,
          hasUnknownOpenValue: false
        };

      next.netAmount += row.currentBalance;
      next.usdInvested += row.totalInvestedUsd;
      next.realizedProfit += row.realizedPnlUsd;
      if (row.currentBalance > 0) {
        if (row.currentValue === null) {
          next.hasUnknownOpenValue = true;
        } else {
          next.currentValueSum += row.currentValue;
        }
        if (row.currentPrice !== null) {
          next.currentPrice = row.currentPrice;
        }
      }
      grouped.set(row.symbol, next);
    }

    return Array.from(grouped.values())
      .map((row) => ({
        assetName: row.assetName,
        symbol: row.symbol,
        netAmount: row.netAmount,
        usdInvested: row.usdInvested,
        realizedProfit: row.realizedProfit,
        currentPrice: row.currentPrice,
        currentValue: row.hasUnknownOpenValue ? null : row.currentValueSum,
        priceState: pricesBySymbol[row.symbol]
      }))
      .sort((a, b) => a.symbol.localeCompare(b.symbol));
  }, [assetNameBySymbol, pricesBySymbol, rowValuations]);

  const exchangeRows = useMemo<ExchangeViewRow[]>(() => {
    const grouped = new Map<
      string,
      { exchangeName: string; totalValueSum: number; realizedProfitUsd: number; hasUnknownOpenValue: boolean; hasOpen: boolean }
    >();

    for (const row of rowValuations) {
      const existing = grouped.get(row.exchange);
      const next =
        existing ?? {
          exchangeName: row.exchange,
          totalValueSum: 0,
          realizedProfitUsd: 0,
          hasUnknownOpenValue: false,
          hasOpen: false
        };

      next.realizedProfitUsd += row.realizedPnlUsd;
      if (row.currentBalance > 0) {
        next.hasOpen = true;
        if (row.currentValue === null) {
          next.hasUnknownOpenValue = true;
        } else {
          next.totalValueSum += row.currentValue;
        }
      }
      grouped.set(row.exchange, next);
    }

    const activeRows = Array.from(grouped.values()).filter((row) => row.hasOpen);
    const hasUnknownAny = activeRows.some((row) => row.hasUnknownOpenValue);
    const globalValue = hasUnknownAny ? null : activeRows.reduce((sum, row) => sum + row.totalValueSum, 0);

    return activeRows
      .map((row) => {
        const totalValueUsd = row.hasUnknownOpenValue ? null : row.totalValueSum;
        const portfolioPercent =
          totalValueUsd === null || globalValue === null || globalValue <= 0 ? null : (totalValueUsd / globalValue) * 100;

        return {
          exchangeName: row.exchangeName,
          totalValueUsd,
          realizedProfitUsd: row.realizedProfitUsd,
          portfolioPercent
        };
      })
      .sort((left, right) => (right.totalValueUsd ?? -1) - (left.totalValueUsd ?? -1));
  }, [rowValuations]);

  const globalMarketValue = useMemo(() => {
    if (exchangeRows.some((row) => row.totalValueUsd === null)) {
      return null;
    }
    return exchangeRows.reduce((sum, row) => sum + (row.totalValueUsd ?? 0), 0);
  }, [exchangeRows]);

  const activeAssetCount = useMemo(() => assetRows.filter((row) => row.netAmount > 0).length, [assetRows]);
  const activeExchangeCount = exchangeRows.length;

  const totalAssetValueForDebug = useMemo(() => {
    const openAssetRows = assetRows.filter((row) => row.netAmount > 0);
    if (openAssetRows.some((row) => row.currentValue === null)) {
      return null;
    }
    return openAssetRows.reduce((sum, row) => sum + (row.currentValue ?? 0), 0);
  }, [assetRows]);
  const totalExchangeValueForDebug = useMemo(() => {
    if (exchangeRows.some((row) => row.totalValueUsd === null)) {
      return null;
    }
    return exchangeRows.reduce((sum, row) => sum + (row.totalValueUsd ?? 0), 0);
  }, [exchangeRows]);
  const hasCalculationDrift =
    totalAssetValueForDebug !== null &&
    totalExchangeValueForDebug !== null &&
    Math.abs(totalAssetValueForDebug - totalExchangeValueForDebug) > 0.01;

  const summaryClassName =
    globalMarketValue === null
      ? ''
      : globalMarketValue >= totalInvested
        ? 'pnl-positive'
        : 'pnl-negative';

  return (
    <section className="history-panel history-panel-prominent">
      <h3>Portfolio Summary</h3>
      <div className="cards-grid portfolio-summary-cards">
        <div className="metric-card">
          <h3>Total Invested</h3>
          <p>{formatUsd(String(totalInvested))}</p>
        </div>
        <div className="metric-card">
          <h3>Current Market Value</h3>
          <p className={summaryClassName}>
            {globalMarketValue === null ? '---' : formatUsd(String(globalMarketValue))}
          </p>
        </div>
        <div className="metric-card">
          <h3>Active Assets</h3>
          <p>{activeAssetCount}</p>
        </div>
        <div className="metric-card">
          <h3>Active Exchanges</h3>
          <p>{activeExchangeCount}</p>
        </div>
      </div>

      {groupingMode === 'ASSET' && assetRows.length === 0 ? <p>No portfolio history yet.</p> : null}
      {groupingMode === 'EXCHANGE' && exchangeRows.length === 0 ? <p>No open positions by exchange yet.</p> : null}

      {groupingMode === 'ASSET' && assetRows.length > 0 ? (
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
              {assetRows.map((row) => {
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

      {groupingMode === 'EXCHANGE' && exchangeRows.length > 0 ? (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Exchange Name</th>
                <th>Total Value (USD)</th>
                <th>Percentage of Portfolio</th>
                <th>Realized Profit</th>
              </tr>
            </thead>
            <tbody>
              {exchangeRows.map((row) => {
                const realizedClassName =
                  row.realizedProfitUsd > 0 ? 'pnl-positive' : row.realizedProfitUsd < 0 ? 'pnl-negative' : '';
                return (
                  <tr key={row.exchangeName}>
                    <td>{row.exchangeName}</td>
                    <td>{row.totalValueUsd === null ? '---' : formatUsd(String(row.totalValueUsd))}</td>
                    <td>{row.portfolioPercent === null ? '---' : formatPercent(row.portfolioPercent)}</td>
                    <td className={realizedClassName}>{formatUsd(String(row.realizedProfitUsd))}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : null}

      <div className={`dashboard-debug-footer${hasCalculationDrift ? ' mismatch' : ''}`}>
        <span>Total Asset Value: {totalAssetValueForDebug === null ? '---' : formatUsd(String(totalAssetValueForDebug))}</span>
        <span>Total Exchange Value: {totalExchangeValueForDebug === null ? '---' : formatUsd(String(totalExchangeValueForDebug))}</span>
      </div>
    </section>
  );
}
