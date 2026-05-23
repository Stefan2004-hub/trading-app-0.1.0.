import { useEffect, useMemo, useState } from 'react';
import { tradingApi } from '../api/tradingApi';
import { AppHeader } from '../components/AppHeader';
import { loadTradingBootstrap } from '../store/tradingSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import type {
  AccumulationTradeAssetSummaryItem,
  AccumulationTradeItem,
  AccumulationTradeStatus,
  AssetOption
} from '../types/trading';
import { formatDateTime, formatNumber, formatUsd } from '../utils/format';

function labelAsset(assetId: string, assets: AssetOption[]): string {
  return assets.find((item) => item.id === assetId)?.symbol ?? assetId;
}

export function AccumulationStrategyPage(): JSX.Element {
  const defaultPageSize = 20;
  const dispatch = useAppDispatch();
  const { assets, bootstrapAttempted, loading: bootstrapLoading } = useAppSelector((state) => state.trading);
  const authUserId = useAppSelector((state) => state.auth.user?.userId);
  const [rows, setRows] = useState<AccumulationTradeItem[]>([]);
  const [summaries, setSummaries] = useState<AccumulationTradeAssetSummaryItem[]>([]);
  const [selectedAssetId, setSelectedAssetId] = useState('');
  const [selectedStatus, setSelectedStatus] = useState<AccumulationTradeStatus>('CLOSED');
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(defaultPageSize);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loadingRows, setLoadingRows] = useState(false);
  const [loadingSummaries, setLoadingSummaries] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loading = loadingRows || loadingSummaries || bootstrapLoading;
  const assetFilter = selectedAssetId || undefined;

  const sortedAssets = useMemo(
    () => [...assets].sort((left, right) => left.symbol.localeCompare(right.symbol)),
    [assets]
  );

  useEffect(() => {
    if (bootstrapLoading || bootstrapAttempted) {
      return;
    }
    void dispatch(loadTradingBootstrap(authUserId));
  }, [authUserId, bootstrapAttempted, bootstrapLoading, dispatch]);

  useEffect(() => {
    async function loadRows(): Promise<void> {
      setLoadingRows(true);
      setError(null);
      try {
        const response = await tradingApi.listAccumulationTrades({
          page: currentPage,
          size: pageSize,
          assetId: assetFilter,
          status: selectedStatus,
          userId: authUserId
        });
        setRows(response.content ?? []);
        setTotalPages(response.totalPages ?? 0);
        setTotalElements(response.totalElements ?? 0);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load accumulation trades.');
      } finally {
        setLoadingRows(false);
      }
    }

    void loadRows();
  }, [assetFilter, authUserId, currentPage, pageSize, selectedStatus]);

  useEffect(() => {
    async function loadSummaries(): Promise<void> {
      setLoadingSummaries(true);
      setError(null);
      try {
        setSummaries(
          await tradingApi.listAccumulationTradeAssetSummaries({
            assetId: assetFilter,
            status: selectedStatus,
            userId: authUserId
          })
        );
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load accumulation asset totals.');
      } finally {
        setLoadingSummaries(false);
      }
    }

    void loadSummaries();
  }, [assetFilter, authUserId, selectedStatus]);

  useEffect(() => {
    if (totalPages > 0 && currentPage >= totalPages) {
      setCurrentPage(totalPages - 1);
    }
  }, [currentPage, totalPages]);

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel transactions-workspace-panel">
        <h1>Accumulation Strategy</h1>
        {error ? <p className="auth-error">{error}</p> : null}

        <div className="search-controls">
          <label htmlFor="accumulation-asset-filter">Asset</label>
          <select
            id="accumulation-asset-filter"
            className="search-input"
            value={selectedAssetId}
            disabled={loading}
            onChange={(event) => {
              setSelectedAssetId(event.target.value);
              setCurrentPage(0);
            }}
          >
            <option value="">All assets</option>
            {sortedAssets.map((asset) => (
              <option key={asset.id} value={asset.id}>
                {asset.symbol} ({asset.name})
              </option>
            ))}
          </select>
        </div>

        <div className="search-controls">
          <label htmlFor="accumulation-status-filter">Status</label>
          <select
            id="accumulation-status-filter"
            className="search-input"
            value={selectedStatus}
            disabled={loading}
            onChange={(event) => {
              setSelectedStatus(event.target.value as AccumulationTradeStatus);
              setCurrentPage(0);
            }}
          >
            <option value="CLOSED">Closed</option>
            <option value="OPEN">Open</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>

        <section className="history-panel history-panel-prominent accumulation-strategy-panel">
          <h2>Trades</h2>
          {loadingRows ? <p>Loading...</p> : null}
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Asset</th>
                  <th>Exit Price</th>
                  <th>Reentry Price</th>
                  <th>Old Coin Amount</th>
                  <th>New Coin Amount</th>
                  <th>Accumulation Delta</th>
                  <th>Status</th>
                  <th>Created At</th>
                  <th>Closed At</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((trade) => {
                  const deltaValue = trade.accumulationDelta === null ? null : Number(trade.accumulationDelta);
                  const deltaClassName =
                    deltaValue !== null && Number.isFinite(deltaValue) && deltaValue > 0 ? 'pnl-positive' : '';
                  return (
                    <tr key={trade.id}>
                      <td>{labelAsset(trade.assetId, assets)}</td>
                      <td>{formatUsd(trade.exitPriceUsd)}</td>
                      <td>{formatUsd(trade.reentryPriceUsd)}</td>
                      <td>{formatNumber(trade.oldCoinAmount)}</td>
                      <td>{formatNumber(trade.newCoinAmount)}</td>
                      <td className={deltaClassName}>{formatNumber(trade.accumulationDelta)}</td>
                      <td>{trade.status}</td>
                      <td>{formatDateTime(trade.createdAt)}</td>
                      <td>{formatDateTime(trade.closedAt)}</td>
                    </tr>
                  );
                })}
                {!loadingRows && rows.length === 0 ? (
                  <tr>
                    <td colSpan={9}>No accumulation trades found.</td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>

          <div className="transactions-pagination-footer">
            <label htmlFor="accumulation-page-size">Rows per page</label>
            <select
              id="accumulation-page-size"
              className="transactions-page-size-select"
              value={pageSize}
              disabled={loading}
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
              Page {totalPages === 0 ? 0 : currentPage + 1} of {totalPages} ({totalElements} total)
            </span>
            <div className="transactions-pagination-buttons">
              <button
                type="button"
                className="secondary transactions-page-button"
                onClick={() => setCurrentPage((page) => Math.max(0, page - 1))}
                disabled={loading || currentPage === 0}
              >
                Previous
              </button>
              <button
                type="button"
                className="secondary transactions-page-button"
                onClick={() => setCurrentPage((page) => page + 1)}
                disabled={loading || totalPages === 0 || currentPage + 1 >= totalPages}
              >
                Next
              </button>
            </div>
          </div>
        </section>

        <section className="history-panel history-panel-prominent accumulation-strategy-panel">
          <h2>Asset Totals</h2>
          {loadingSummaries ? <p>Loading...</p> : null}
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Asset</th>
                  <th>Total Accumulation Delta</th>
                  <th>Trade Count</th>
                </tr>
              </thead>
              <tbody>
                {summaries.map((summary) => {
                  const deltaValue = Number(summary.totalAccumulationDelta);
                  const deltaClassName = Number.isFinite(deltaValue) && deltaValue > 0 ? 'pnl-positive' : '';
                  return (
                    <tr key={summary.assetId}>
                      <td>{labelAsset(summary.assetId, assets)}</td>
                      <td className={deltaClassName}>{formatNumber(summary.totalAccumulationDelta)}</td>
                      <td>{summary.tradeCount}</td>
                    </tr>
                  );
                })}
                {!loadingSummaries && summaries.length === 0 ? (
                  <tr>
                    <td colSpan={3}>No asset totals found.</td>
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
