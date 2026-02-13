import type { AssetOption } from '../types/trading';
import type { StrategyAlertItem } from '../types/strategy';
import { formatDateTime, formatUsd } from '../utils/format';

interface StrategyAlertListProps {
  alerts: StrategyAlertItem[];
  assets: AssetOption[];
  submitting: boolean;
  onAcknowledge: (alertId: string) => Promise<boolean>;
}

function assetLabel(assetId: string, assets: AssetOption[]): string {
  return assets.find((asset) => asset.id === assetId)?.symbol ?? assetId;
}

export function StrategyAlertList({ alerts, assets, submitting, onAcknowledge }: StrategyAlertListProps): JSX.Element {
  const pendingAlerts = alerts.filter((alert) => alert.status === 'PENDING');

  return (
    <section className="history-panel">
      <h3>Pending Alerts</h3>
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
              <button
                type="button"
                disabled={submitting}
                onClick={async () => {
                  await onAcknowledge(alert.id);
                }}
              >
                {submitting ? 'Working...' : 'Acknowledge'}
              </button>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
