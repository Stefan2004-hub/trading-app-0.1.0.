import { useEffect } from 'react';
import { AppHeader } from '../components/AppHeader';
import { PortfolioSummaryCards } from '../components/PortfolioSummaryCards';
import { loadTradingBootstrap } from '../store/tradingSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

export function DashboardPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const { summary, loading, error } = useAppSelector((state) => state.trading);

  useEffect(() => {
    if (summary || loading) {
      return;
    }
    void dispatch(loadTradingBootstrap());
  }, [dispatch, loading, summary]);

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Portfolio Overview</h1>
        {error ? <p className="auth-error">{error}</p> : null}
        <PortfolioSummaryCards summary={summary} />
      </section>
    </main>
  );
}
