import { Link } from 'react-router-dom';
import type { AssetOption } from '../types/trading';
import type { StrategyAlertItem } from '../types/strategy';
import { formatDateTime, formatUsd } from '../utils/format';

interface StrategyAlertListProps {
  alerts: StrategyAlertItem[];
  assets: AssetOption[];
  submitting: boolean;
  onAcknowledge: (alertId: string) => Promise<boolean>;
  onDelete: (alertId: string) => Promise<boolean>;
}

function assetLabel(assetId: string, assets: AssetOption[]): string {
  return assets.find((asset) => asset.id === assetId)?.symbol ?? assetId;
}

export function StrategyAlertList({
  alerts,
  assets,
  submitting,
  onAcknowledge,
  onDelete
}: StrategyAlertListProps): JSX.Element {
  const pendingAlerts = alerts.filter((alert) => alert.status === 'PENDING');

  return (
    <section className="history-panel">
      <div className="section-header-inline">
        <h3>Pending Alerts</h3>
        <Link className="inline-link-button" to="/alerts">
          View all alerts
        </Link>
      </div>
      {pendingAlerts.length === 0 ? (
        <p>No pending alerts.</p>
      ) : (
        <div className="alert-list">
          {pendingAlerts.map((alert) => (
            <article className="alert-card" key={alert.id}>
              <div>
                <strong>{alert.strategyType}</strong> {assetLabel(alert.assetId, assets)}
              </div>
              <div>{alert.alertMessage}</div>
              <div>Trigger: {formatUsd(alert.triggerPrice)}</div>
              <div>Created: {formatDateTime(alert.createdAt)}</div>
              <div className="transaction-actions">
                <button
                  type="button"
                  className="row-action-button row-action-edit"
                  disabled={submitting}
                  onClick={async () => {
                    await onAcknowledge(alert.id);
                  }}
                >
                  {submitting ? 'Working...' : 'Acknowledge'}
                </button>
                <button
                  type="button"
                  className="row-action-button row-action-delete"
                  disabled={submitting}
                  onClick={async () => {
                    await onDelete(alert.id);
                  }}
                >
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
