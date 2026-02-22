import { useEffect } from 'react';
import { AppHeader } from '../components/AppHeader';
import { OpenPortfolioSection } from '../components/OpenPortfolioSection';
import { loadTradingBootstrap } from '../store/tradingSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

export function DashboardPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const authUserId = useAppSelector((state) => state.auth.user?.userId);
  const { loading, bootstrapAttempted, error, assetSummary } = useAppSelector((state) => state.trading);

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
        <OpenPortfolioSection assetSummary={assetSummary} />
      </section>
    </main>
  );
}
