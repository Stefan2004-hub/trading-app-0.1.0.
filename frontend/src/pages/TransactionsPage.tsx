import { useEffect } from 'react';
import { AppHeader } from '../components/AppHeader';
import { PortfolioSummaryCards } from '../components/PortfolioSummaryCards';
import { TradeForm } from '../components/TradeForm';
import { TransactionHistoryTable } from '../components/TransactionHistoryTable';
import { clearTradingError, loadTradingBootstrap, submitBuyTrade, submitSellTrade } from '../store/tradingSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

export function TransactionsPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const { assets, exchanges, transactions, summary, loading, submitting, error } = useAppSelector(
    (state) => state.trading
  );

  useEffect(() => {
    if (loading || summary) {
      return;
    }
    void dispatch(loadTradingBootstrap());
  }, [dispatch, loading, summary]);

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Trading</h1>
        {error ? <p className="auth-error">{error}</p> : null}

        <PortfolioSummaryCards summary={summary} />

        <section className="forms-grid">
          <TradeForm
            title="Buy"
            assets={assets}
            exchanges={exchanges}
            submitting={submitting}
            onSubmit={async (payload) => {
              dispatch(clearTradingError());
              const action = await dispatch(submitBuyTrade(payload));
              return submitBuyTrade.fulfilled.match(action);
            }}
          />

          <TradeForm
            title="Sell"
            assets={assets}
            exchanges={exchanges}
            submitting={submitting}
            onSubmit={async (payload) => {
              dispatch(clearTradingError());
              const action = await dispatch(submitSellTrade(payload));
              return submitSellTrade.fulfilled.match(action);
            }}
          />
        </section>

        <TransactionHistoryTable transactions={transactions} assets={assets} exchanges={exchanges} />
      </section>
    </main>
  );
}
