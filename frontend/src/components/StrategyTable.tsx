import type { ConfiguredStrategyItem } from '../types/strategy';
import { formatDateTime, formatNumber, formatUsd } from '../utils/format';

interface StrategyTableProps {
  strategies: ConfiguredStrategyItem[];
  submitting: boolean;
  onDeleteSell: (strategyId: string) => Promise<boolean>;
  onDeleteBuy: (strategyId: string) => Promise<boolean>;
  className?: string;
  tableWrapClassName?: string;
}

export function StrategyTable({
  strategies,
  submitting,
  onDeleteSell,
  onDeleteBuy,
  className,
  tableWrapClassName
}: StrategyTableProps): JSX.Element {
  return (
    <section className={`history-panel${className ? ` ${className}` : ''}`}>
      <h3>Configured Strategies</h3>
      <div className={`table-wrap${tableWrapClassName ? ` ${tableWrapClassName}` : ''}`}>
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
            {strategies.length === 0 ? (
              <tr>
                <td colSpan={7}>No configured strategies match the current filters.</td>
              </tr>
            ) : null}
            {strategies.map((item) => (
              <tr key={`${item.strategyType.toLowerCase()}-${item.id}`}>
                <td>{item.strategyType}</td>
                <td>{item.assetSymbol}</td>
                <td>{formatNumber(item.thresholdPercent)}</td>
                <td>{item.buyAmountUsd ? formatUsd(item.buyAmountUsd) : '-'}</td>
                <td>{item.active ? 'Yes' : 'No'}</td>
                <td>{formatDateTime(item.updatedAt)}</td>
                <td>
                  <button
                    type="button"
                    className="row-action-button row-action-delete"
                    disabled={submitting}
                    onClick={() => {
                      if (item.strategyType === 'SELL') {
                        void onDeleteSell(item.id);
                        return;
                      }
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
