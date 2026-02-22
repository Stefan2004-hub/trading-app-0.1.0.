import { useEffect, useMemo, useState } from 'react';
import { AppHeader } from '../components/AppHeader';
import type { StrategyAlertStatus } from '../types/strategy';
import { formatDateTime, formatPercent, formatUsd } from '../utils/format';
import {
  acknowledgeStrategyAlert,
  clearStrategyError,
  deleteStrategyAlert,
  loadStrategyData
} from '../store/strategySlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

type AlertStatusFilter = StrategyAlertStatus | 'ALL';

function statusRank(status: StrategyAlertStatus): number {
  switch (status) {
    case 'PENDING':
      return 0;
    case 'ACKNOWLEDGED':
      return 1;
    case 'EXECUTED':
      return 2;
    case 'DISMISSED':
      return 3;
    default:
      return 10;
  }
}

export function AlertsPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const { alerts, assets, loading, dataAttempted, submitting, error } = useAppSelector((state) => state.strategy);
  const [statusFilter, setStatusFilter] = useState<AlertStatusFilter>('ALL');
  const [search, setSearch] = useState('');

  useEffect(() => {
    if (loading || dataAttempted) {
      return;
    }
    void dispatch(loadStrategyData());
  }, [dataAttempted, dispatch, loading]);

  const assetLabelById = useMemo(() => {
    const map = new Map<string, string>();
    for (const asset of assets) {
      map.set(asset.id, `${asset.symbol} - ${asset.name}`);
    }
    return map;
  }, [assets]);

  const filteredAlerts = useMemo(() => {
    const term = search.trim().toLowerCase();
    return alerts
      .filter((alert) => (statusFilter === 'ALL' ? true : alert.status === statusFilter))
      .filter((alert) => {
        if (!term) {
          return true;
        }
        const assetLabel = assetLabelById.get(alert.assetId) ?? alert.assetId;
        return (
          assetLabel.toLowerCase().includes(term) ||
          alert.alertMessage.toLowerCase().includes(term) ||
          alert.strategyType.toLowerCase().includes(term)
        );
      })
      .sort((left, right) => {
        const byCreatedDesc = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
        if (byCreatedDesc !== 0) {
          return byCreatedDesc;
        }
        const byStatus = statusRank(left.status) - statusRank(right.status);
        if (byStatus !== 0) {
          return byStatus;
        }
        return left.id.localeCompare(right.id);
      });
  }, [alerts, assetLabelById, search, statusFilter]);

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Alerts</h1>
        {error ? <p className="auth-error">{error}</p> : null}

        <div className="search-controls">
          <label htmlFor="alerts-status-filter">Status</label>
          <select
            id="alerts-status-filter"
            className="search-input"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as AlertStatusFilter)}
          >
            <option value="ALL">All</option>
            <option value="PENDING">Pending</option>
            <option value="ACKNOWLEDGED">Acknowledged</option>
            <option value="EXECUTED">Executed</option>
            <option value="DISMISSED">Dismissed</option>
          </select>

          <label htmlFor="alerts-search">Search</label>
          <input
            id="alerts-search"
            className="search-input"
            type="search"
            placeholder="Search by asset, type, or message"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        <section className="history-panel">
          <h3>All Strategy Alerts</h3>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Created</th>
                  <th>Type</th>
                  <th>Asset</th>
                  <th>Status</th>
                  <th>Reference</th>
                  <th>Trigger</th>
                  <th>Threshold</th>
                  <th>Message</th>
                  <th>Acknowledged</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredAlerts.map((alert) => (
                  <tr key={alert.id}>
                    <td>{formatDateTime(alert.createdAt)}</td>
                    <td>{alert.strategyType}</td>
                    <td>{assetLabelById.get(alert.assetId) ?? alert.assetId}</td>
                    <td>{alert.status}</td>
                    <td>{formatUsd(alert.referencePrice)}</td>
                    <td>{formatUsd(alert.triggerPrice)}</td>
                    <td>{formatPercent(alert.thresholdPercent)}</td>
                    <td>{alert.alertMessage}</td>
                    <td>{formatDateTime(alert.acknowledgedAt)}</td>
                    <td>
                      <div className="transaction-actions">
                        {alert.status === 'PENDING' ? (
                          <button
                            type="button"
                            className="row-action-button row-action-edit"
                            disabled={submitting}
                            onClick={async () => {
                              dispatch(clearStrategyError());
                              await dispatch(acknowledgeStrategyAlert(alert.id));
                            }}
                          >
                            Acknowledge
                          </button>
                        ) : null}
                        <button
                          type="button"
                          className="row-action-button row-action-delete"
                          disabled={submitting}
                          onClick={async () => {
                            dispatch(clearStrategyError());
                            await dispatch(deleteStrategyAlert(alert.id));
                          }}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!loading && filteredAlerts.length === 0 ? (
                  <tr>
                    <td colSpan={10}>No alerts found for current filters.</td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>
      </section>
    </main>
  );
}
