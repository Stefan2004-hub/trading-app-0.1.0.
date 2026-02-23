import { useEffect, useState } from 'react';
import { AppHeader } from '../components/AppHeader';
import { OpenPortfolioSection } from '../components/OpenPortfolioSection';
import { loadTradingBootstrap } from '../store/tradingSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import type { DashboardGroupingMode } from '../types/trading';

export function DashboardPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const authUserId = useAppSelector((state) => state.auth.user?.userId);
  const { loading, bootstrapAttempted, error, assetSummary, performance } = useAppSelector((state) => state.trading);
  const [groupingMode, setGroupingMode] = useState<DashboardGroupingMode>('ASSET');

  useEffect(() => {
    if (loading || bootstrapAttempted) {
      return;
    }
    void dispatch(loadTradingBootstrap(authUserId));
  }, [authUserId, bootstrapAttempted, dispatch, loading]);

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Portfolio Overview</h1>
        {error ? <p className="auth-error">{error}</p> : null}
        <div className="dashboard-grouping-controls">
          <p className="transaction-history-toggle">Grouping</p>
          <div className="transactions-view-tabs" role="tablist" aria-label="Dashboard grouping mode">
            <button
              type="button"
              className={`transactions-view-tab ${groupingMode === 'ASSET' ? 'active' : ''}`.trim()}
              onClick={() => setGroupingMode('ASSET')}
              role="tab"
              aria-selected={groupingMode === 'ASSET'}
            >
              Asset View
            </button>
            <button
              type="button"
              className={`transactions-view-tab ${groupingMode === 'EXCHANGE' ? 'active' : ''}`.trim()}
              onClick={() => setGroupingMode('EXCHANGE')}
              role="tab"
              aria-selected={groupingMode === 'EXCHANGE'}
            >
              Exchange View
            </button>
          </div>
        </div>
        <OpenPortfolioSection assetSummary={assetSummary} performance={performance} groupingMode={groupingMode} />
      </section>
    </main>
  );
}
