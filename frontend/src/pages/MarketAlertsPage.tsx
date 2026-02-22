import { useEffect, useMemo, useState } from 'react';
import { marketAlertApi } from '../api/marketAlertApi';
import { AppHeader } from '../components/AppHeader';
import { ConfirmDialog } from '../components/ConfirmDialog';
import type { MarketAlertItem } from '../types/marketAlert';
import { formatDateTime, formatNumber } from '../utils/format';

interface GroupedAlerts {
  assetId: string;
  assetLabel: string;
  alerts: MarketAlertItem[];
}

function strategyLabel(strategyType: MarketAlertItem['strategyType']): string {
  return strategyType === 'FIXED_14D' ? 'Fixed 14d' : 'Dynamic';
}

export function MarketAlertsPage(): JSX.Element {
  const [alerts, setAlerts] = useState<MarketAlertItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [scanning, setScanning] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [clearDialogOpen, setClearDialogOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    void loadAlerts();
  }, []);

  async function loadAlerts(): Promise<void> {
    setLoading(true);
    setError(null);
    try {
      setAlerts(await marketAlertApi.list());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load market alerts.');
    } finally {
      setLoading(false);
    }
  }

  async function handleScan(): Promise<void> {
    setScanning(true);
    setError(null);
    setInfo(null);
    try {
      const response = await marketAlertApi.scan();
      setInfo(
        `Scan complete: ${response.alertsCreated} alerts created (${response.fixedAlertsCreated} fixed, ${response.dynamicAlertsCreated} dynamic) across ${response.assetsProcessed} assets.`
      );
      await loadAlerts();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to run market scan.');
    } finally {
      setScanning(false);
    }
  }

  async function handleMarkViewed(alertId: string): Promise<void> {
    setUpdating(true);
    setError(null);
    try {
      await marketAlertApi.markViewed(alertId);
      await loadAlerts();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to mark alert as read.');
    } finally {
      setUpdating(false);
    }
  }

  async function handleClear(): Promise<void> {
    setUpdating(true);
    setError(null);
    setInfo(null);
    try {
      await marketAlertApi.clearHistory();
      setClearDialogOpen(false);
      setInfo('Alert history cleared.');
      await loadAlerts();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to clear alert history.');
    } finally {
      setUpdating(false);
    }
  }

  const groupedAlerts = useMemo<GroupedAlerts[]>(() => {
    const sorted = [...alerts].sort((left, right) => {
      const byDate = new Date(right.triggerDate).getTime() - new Date(left.triggerDate).getTime();
      if (byDate !== 0) {
        return byDate;
      }
      return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
    });

    const grouped = new Map<string, GroupedAlerts>();
    for (const alert of sorted) {
      const existing = grouped.get(alert.assetId);
      if (existing) {
        existing.alerts.push(alert);
      } else {
        grouped.set(alert.assetId, {
          assetId: alert.assetId,
          assetLabel: `${alert.assetSymbol} - ${alert.assetName}`,
          alerts: [alert]
        });
      }
    }

    return Array.from(grouped.values()).sort((left, right) => left.assetLabel.localeCompare(right.assetLabel));
  }, [alerts]);

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Market Alerts</h1>
        <p>RSI/Stochastic scan results from your stored historical data.</p>

        {error ? <p className="auth-error">{error}</p> : null}
        {info ? <p className="field-help">{info}</p> : null}

        <div className="transactions-title-actions historical-data-actions">
          <button type="button" className="secondary" onClick={() => void handleScan()} disabled={loading || scanning || updating}>
            {scanning ? 'Scanning...' : 'Refresh Scan'}
          </button>
          <button
            type="button"
            className="clean-history-button"
            onClick={() => setClearDialogOpen(true)}
            disabled={loading || scanning || updating || alerts.length === 0}
          >
            Clear History
          </button>
        </div>

        {loading ? <p>Loading...</p> : null}

        {groupedAlerts.map((group) => (
          <section className="history-panel history-panel-prominent" key={group.assetId}>
            <h2>{group.assetLabel}</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Trigger Date</th>
                    <th>Signal</th>
                    <th>RSI</th>
                    <th>Stoch</th>
                    <th>Strategy</th>
                    <th>Interval</th>
                    <th>Created</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {group.alerts.map((alert) => (
                    <tr key={alert.id}>
                      <td>{alert.triggerDate}</td>
                      <td>{alert.alertType}</td>
                      <td>{`RSI: ${formatNumber(alert.rsiValue)}`}</td>
                      <td>{`Stoch: ${formatNumber(alert.stochValue)}`}</td>
                      <td>{strategyLabel(alert.strategyType)}</td>
                      <td>{`${alert.intervalDays} days`}</td>
                      <td>{formatDateTime(alert.createdAt)}</td>
                      <td>{alert.isViewed ? 'Read' : 'New'}</td>
                      <td>
                        {!alert.isViewed ? (
                          <button
                            type="button"
                            className="row-action-button row-action-edit"
                            disabled={updating || scanning}
                            onClick={() => {
                              void handleMarkViewed(alert.id);
                            }}
                          >
                            Mark as Read
                          </button>
                        ) : (
                          '-'
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        ))}

        {!loading && groupedAlerts.length === 0 ? <p>No market alerts yet. Run Refresh Scan to evaluate latest data.</p> : null}
      </section>

      <ConfirmDialog
        open={clearDialogOpen}
        title="Clear Market Alert History?"
        message="This will permanently delete your market alert history for your account."
        confirmText="Clear History"
        loadingText="Clearing..."
        loading={updating}
        onConfirm={() => {
          void handleClear();
        }}
        onCancel={() => setClearDialogOpen(false)}
      />
    </main>
  );
}
