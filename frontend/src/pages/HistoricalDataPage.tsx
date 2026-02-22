import { useEffect, useState } from 'react';
import { tradingApi } from '../api/tradingApi';
import { AppHeader } from '../components/AppHeader';
import { ConfirmDialog } from '../components/ConfirmDialog';
import type { HistoricalDataRow, HistoricalDataSyncResult } from '../types/trading';

function formatUsd(value: string): string {
  const parsed = Number(value);
  if (Number.isNaN(parsed)) {
    return value;
  }
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 8
  }).format(parsed);
}

export function HistoricalDataPage(): JSX.Element {
  const [rows, setRows] = useState<HistoricalDataRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [skipped, setSkipped] = useState<HistoricalDataSyncResult['skippedAssets']>([]);

  useEffect(() => {
    void loadRows();
  }, []);

  async function loadRows(): Promise<void> {
    setLoading(true);
    setError(null);
    try {
      setRows(await tradingApi.listHistoricalData());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load historical data.');
    } finally {
      setLoading(false);
    }
  }

  async function handleRefresh(): Promise<void> {
    setRefreshing(true);
    setError(null);
    setInfo(null);
    setSkipped([]);
    try {
      const response = await tradingApi.refreshHistoricalData();
      setInfo(
        `Sync complete: ${response.rowsInserted} rows added across ${response.assetsProcessed} assets (${response.assetsSkipped} skipped).`
      );
      setSkipped(response.skippedAssets ?? []);
      await loadRows();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to refresh historical data.');
    } finally {
      setRefreshing(false);
    }
  }

  async function handleCleanReset(): Promise<void> {
    setResetting(true);
    setError(null);
    setInfo(null);
    setSkipped([]);
    try {
      const response = await tradingApi.cleanAndResetHistoricalData();
      setInfo(
        `Reset complete: ${response.rowsInserted} rows restored for ${response.assetsProcessed} assets (${response.assetsSkipped} skipped).`
      );
      setSkipped(response.skippedAssets ?? []);
      setConfirmOpen(false);
      await loadRows();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to clean and reset historical data.');
    } finally {
      setResetting(false);
    }
  }

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Historical Data</h1>
        <p>Manage stored daily High, Low, and Close prices for assets in your portfolio.</p>

        {error ? <p className="auth-error">{error}</p> : null}
        {info ? <p className="field-help">{info}</p> : null}
        {skipped.length > 0 ? (
          <section className="history-panel">
            <h3>Skipped Assets</h3>
            <ul>
              {skipped.map((item) => (
                <li key={`${item.assetId}-${item.assetSymbol}`}>
                  <strong>{item.assetSymbol}</strong>: {item.reason}
                </li>
              ))}
            </ul>
          </section>
        ) : null}

        <div className="transactions-title-actions historical-data-actions">
          <button type="button" className="secondary" onClick={() => void handleRefresh()} disabled={loading || refreshing || resetting}>
            {refreshing ? 'Refreshing...' : 'Refresh'}
          </button>
          <button type="button" className="clean-history-button" onClick={() => setConfirmOpen(true)} disabled={loading || refreshing || resetting}>
            Clean &amp; Reset
          </button>
        </div>

        <section className="history-panel history-panel-prominent">
          <h2>Stored Prices</h2>
          {loading ? <p>Loading...</p> : null}
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Asset</th>
                  <th>High</th>
                  <th>Low</th>
                  <th>Close</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.dayDate}</td>
                    <td>
                      {row.assetSymbol} ({row.assetName})
                    </td>
                    <td>{formatUsd(row.highPrice)}</td>
                    <td>{formatUsd(row.lowPrice)}</td>
                    <td>{formatUsd(row.closingPrice)}</td>
                  </tr>
                ))}
                {!loading && rows.length === 0 ? (
                  <tr>
                    <td colSpan={5}>No historical data available. Click Refresh to sync.</td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>
      </section>

      <ConfirmDialog
        open={confirmOpen}
        title="Clean and Reset Historical Data?"
        message="This will delete all stored historical data and re-sync only the last 30 days for your invested assets."
        confirmText="Clean & Reset"
        loadingText="Resetting..."
        loading={resetting}
        onConfirm={() => {
          void handleCleanReset();
        }}
        onCancel={() => setConfirmOpen(false)}
      />
    </main>
  );
}
