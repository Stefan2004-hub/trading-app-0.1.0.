import type { AssetOption } from '../types/trading';
import type { BuyStrategyItem, SellStrategyItem } from '../types/strategy';
import { formatDateTime, formatNumber, formatUsd } from '../utils/format';

interface StrategyTableProps {
  assets: AssetOption[];
  sellStrategies: SellStrategyItem[];
  buyStrategies: BuyStrategyItem[];
}

function assetLabel(assetId: string, assets: AssetOption[]): string {
  return assets.find((asset) => asset.id === assetId)?.symbol ?? assetId;
}

export function StrategyTable({ assets, sellStrategies, buyStrategies }: StrategyTableProps): JSX.Element {
  return (
    <section className="history-panel">
      <h3>Configured Strategies</h3>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Type</th>
              <th>Asset</th>
              <th>Threshold %</th>
              <th>Buy Amount USD</th>
              <th>Active</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            {sellStrategies.map((item) => (
              <tr key={`sell-${item.id}`}>
                <td>SELL</td>
                <td>{assetLabel(item.assetId, assets)}</td>
                <td>{formatNumber(item.thresholdPercent)}</td>
                <td>-</td>
                <td>{item.active ? 'Yes' : 'No'}</td>
                <td>{formatDateTime(item.updatedAt)}</td>
              </tr>
            ))}
            {buyStrategies.map((item) => (
              <tr key={`buy-${item.id}`}>
                <td>BUY</td>
                <td>{assetLabel(item.assetId, assets)}</td>
                <td>{formatNumber(item.dipThresholdPercent)}</td>
                <td>{formatUsd(item.buyAmountUsd)}</td>
                <td>{item.active ? 'Yes' : 'No'}</td>
                <td>{formatDateTime(item.updatedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
