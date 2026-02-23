import { useEffect, useState } from 'react';
import { tradingApi } from '../api/tradingApi';
import { AppHeader } from '../components/AppHeader';
import { ConfirmDialog } from '../components/ConfirmDialog';
import type { AssetOption, HistoricalDataRow, HistoricalDataSyncResult, HistoricalMissingAsset } from '../types/trading';

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
  const defaultPageSize = 20;
  const [rows, setRows] = useState<HistoricalDataRow[]>([]);
  const [assetOptions, setAssetOptions] = useState<AssetOption[]>([]);
  const [missingAssets, setMissingAssets] = useState<HistoricalMissingAsset[]>([]);
  const [selectedRefreshAssetId, setSelectedRefreshAssetId] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(defaultPageSize);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [skipped, setSkipped] = useState<HistoricalDataSyncResult['skippedAssets']>([]);

  useEffect(() => {
    void loadAssets();
    void loadMissingAssets();
  }, []);

  useEffect(() => {
    void loadRows(currentPage, pageSize);
  }, [currentPage, pageSize]);

  useEffect(() => {
    if (totalPages > 0 && currentPage >= totalPages) {
      setCurrentPage(totalPages - 1);
    }
  }, [currentPage, totalPages]);

  async function loadRows(page: number, size: number): Promise<void> {
    setLoading(true);
    setError(null);
    try {
      const response = await tradingApi.listHistoricalData({ page, size });
      setRows(response.content ?? []);
      setTotalPages(response.totalPages ?? 0);
      setTotalElements(response.totalElements ?? 0);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load historical data.');
    } finally {
      setLoading(false);
    }
  }

  async function loadAssets(): Promise<void> {
    try {
      setAssetOptions(await tradingApi.listAssets());
    } catch {
      setAssetOptions([]);
    }
  }

  async function loadMissingAssets(): Promise<void> {
    try {
      setMissingAssets(await tradingApi.listHistoricalAssetsMissingToday());
    } catch {
      setMissingAssets([]);
    }
  }

  async function handleRefresh(assetIdOverride?: string): Promise<void> {
    setRefreshing(true);
    setError(null);
    setInfo(null);
    setSkipped([]);
    try {
      const resolvedAssetId = assetIdOverride ?? selectedRefreshAssetId;
      if (!resolvedAssetId) {
        const response = await tradingApi.startHistoricalDataRefreshAll();
        setInfo(`${response.message} (${response.startedAt})`);
      } else {
        const response = await tradingApi.refreshHistoricalData(resolvedAssetId);
        setInfo(
          `Sync complete: ${response.rowsInserted} rows added across ${response.assetsProcessed} assets (${response.assetsSkipped} skipped).`
        );
        setSkipped(response.skippedAssets ?? []);
        if (currentPage === 0) {
          await loadRows(0, pageSize);
        } else {
          setCurrentPage(0);
        }
        await loadMissingAssets();
      }
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
      if (currentPage === 0) {
        await loadRows(0, pageSize);
      } else {
        setCurrentPage(0);
      }
      await loadMissingAssets();
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
        <p>Manage stored daily High, Low, and Close prices for tracked assets.</p>

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
          <label htmlFor="historical-refresh-scope" className="historical-refresh-scope">
            <span>Refresh scope</span>
            <select
              id="historical-refresh-scope"
              value={selectedRefreshAssetId}
              onChange={(event) => setSelectedRefreshAssetId(event.target.value)}
              disabled={loading || refreshing || resetting}
            >
              <option value="">All assets</option>
              {assetOptions.map((asset) => (
                <option key={asset.id} value={asset.id}>
                  {asset.symbol} ({asset.name})
                </option>
              ))}
            </select>
          </label>
          <button type="button" className="secondary" onClick={() => void handleRefresh()} disabled={loading || refreshing || resetting}>
            {refreshing ? 'Refreshing...' : 'Refresh'}
          </button>
          <button type="button" className="clean-history-button" onClick={() => setConfirmOpen(true)} disabled={loading || refreshing || resetting}>
            Clean &amp; Reset
          </button>
        </div>

        <section className="history-panel history-panel-prominent">
          <h2>Assets Missing Today&apos;s History (UTC)</h2>
          {missingAssets.length === 0 ? <p>All assets have historical data for today.</p> : null}
          {missingAssets.length > 0 ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Asset</th>
                    <th>Missing Date</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {missingAssets.map((asset) => (
                    <tr key={asset.assetId}>
                      <td>
                        {asset.assetSymbol} ({asset.assetName})
                      </td>
                      <td>{asset.missingDate}</td>
                      <td>
                        <button
                          type="button"
                          className="row-action-button row-action-edit"
                          disabled={loading || refreshing || resetting}
                          onClick={() => {
                            setSelectedRefreshAssetId(asset.assetId);
                            void handleRefresh(asset.assetId);
                          }}
                        >
                          Refresh Asset
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>

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
          <div className="transactions-pagination-footer">
            <label htmlFor="historical-page-size">Rows per page</label>
            <select
              id="historical-page-size"
              className="transactions-page-size-select"
              value={pageSize}
              disabled={loading || refreshing || resetting}
              onChange={(event) => {
                setPageSize(Number(event.target.value));
                setCurrentPage(0);
              }}
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
            <span className="transactions-pagination-label">
              Page {totalPages === 0 ? 0 : currentPage + 1} of {totalPages} ({totalElements} rows)
            </span>
            <div className="transactions-pagination-buttons">
              <button
                type="button"
                className="secondary transactions-page-button"
                onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
                disabled={loading || refreshing || resetting || currentPage <= 0}
              >
                Previous
              </button>
              <button
                type="button"
                className="secondary transactions-page-button"
                onClick={() => setCurrentPage((prev) => prev + 1)}
                disabled={loading || refreshing || resetting || totalPages === 0 || currentPage >= totalPages - 1}
              >
                Next
              </button>
            </div>
          </div>
        </section>
      </section>

      <ConfirmDialog
        open={confirmOpen}
        title="Clean and Reset Historical Data?"
        message="This will delete all stored historical data and re-sync only the last 30 days for all assets."
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
