import type { AccumulationTradeItem, AssetOption } from '../types/trading';
import { formatDateTime, formatNumber, formatUsd } from '../utils/format';

interface AccumulationStrategySectionProps {
  trades: AccumulationTradeItem[];
  assets: AssetOption[];
}

function labelAsset(assetId: string, assets: AssetOption[]): string {
  return assets.find((item) => item.id === assetId)?.symbol ?? assetId;
}

export function AccumulationStrategySection({ trades, assets }: AccumulationStrategySectionProps): JSX.Element {
  const closedTrades = trades.filter((trade) => trade.status === 'CLOSED');

  return (
    <section className="history-panel history-panel-prominent accumulation-strategy-panel">
      <h3>Accumulation Strategy</h3>
      {closedTrades.length === 0 ? (
        <p>No closed accumulation trades yet.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Asset</th>
                <th>Exit Price</th>
                <th>Reentry Price</th>
                <th>Accumulation Delta</th>
                <th>Closed At</th>
              </tr>
            </thead>
            <tbody>
              {closedTrades.map((trade) => {
                const deltaValue = trade.accumulationDelta === null ? null : Number(trade.accumulationDelta);
                const deltaClassName =
                  deltaValue !== null && Number.isFinite(deltaValue) && deltaValue > 0 ? 'pnl-positive' : '';

                return (
                  <tr key={trade.id}>
                    <td>{labelAsset(trade.assetId, assets)}</td>
                    <td>{formatUsd(trade.exitPriceUsd)}</td>
                    <td>{formatUsd(trade.reentryPriceUsd)}</td>
                    <td className={deltaClassName}>{formatNumber(trade.accumulationDelta)}</td>
                    <td>{formatDateTime(trade.closedAt)}</td>
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
