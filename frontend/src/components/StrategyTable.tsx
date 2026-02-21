import type { AssetOption } from '../types/trading';
import type { BuyStrategyItem, SellStrategyItem } from '../types/strategy';
import { formatDateTime, formatNumber, formatUsd } from '../utils/format';

interface StrategyTableProps {
  assets: AssetOption[];
  sellStrategies: SellStrategyItem[];
  buyStrategies: BuyStrategyItem[];
  submitting: boolean;
  onDeleteSell: (strategyId: string) => Promise<boolean>;
  onDeleteBuy: (strategyId: string) => Promise<boolean>;
}

function assetLabel(assetId: string, assets: AssetOption[]): string {
  return assets.find((asset) => asset.id === assetId)?.symbol ?? assetId;
}

export function StrategyTable({
  assets,
  sellStrategies,
  buyStrategies,
  submitting,
  onDeleteSell,
  onDeleteBuy
}: StrategyTableProps): JSX.Element {
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
              <th>Actions</th>
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
                <td>
                  <button
                    type="button"
                    className="row-action-button row-action-delete"
                    disabled={submitting}
                    onClick={() => {
                      void onDeleteSell(item.id);
                    }}
                  >
                    Delete
                  </button>
                </td>
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
                <td>
                  <button
                    type="button"
                    className="row-action-button row-action-delete"
                    disabled={submitting}
                    onClick={() => {
                      void onDeleteBuy(item.id);
                    }}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
