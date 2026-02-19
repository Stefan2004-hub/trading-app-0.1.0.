import { useEffect } from 'react';
import { AppHeader } from '../components/AppHeader';
import { PortfolioSummaryCards } from '../components/PortfolioSummaryCards';
import { TradeForm } from '../components/TradeForm';
import { TransactionHistoryTable } from '../components/TransactionHistoryTable';
import {
  clearTradingError,
  loadTradingBootstrap,
  submitBuyTrade,
  submitSellTrade,
  updateDefaultBuyInputMode
} from '../store/tradingSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

export function TransactionsPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const { assets, exchanges, transactions, summary, loading, bootstrapAttempted, submitting, error, userPreferences } =
    useAppSelector((state) => state.trading);

  useEffect(() => {
    if (loading || bootstrapAttempted) {
      return;
    }
    void dispatch(loadTradingBootstrap());
  }, [bootstrapAttempted, dispatch, loading]);

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
            tradeType="BUY"
            assets={assets}
            exchanges={exchanges}
            submitting={submitting}
            defaultBuyInputMode={userPreferences?.defaultBuyInputMode}
            onBuyInputModeChange={(mode) => {
              void dispatch(updateDefaultBuyInputMode(mode));
            }}
            onSubmit={async (payload) => {
              dispatch(clearTradingError());
              const action = await dispatch(submitBuyTrade(payload));
              return submitBuyTrade.fulfilled.match(action);
            }}
          />

          <TradeForm
            title="Sell"
            tradeType="SELL"
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
