import { useCallback, useEffect, useState } from 'react';
import { AppHeader } from '../components/AppHeader';
import { PortfolioSummaryCards } from '../components/PortfolioSummaryCards';
import { TradeForm } from '../components/TradeForm';
import { TransactionHistoryTable } from '../components/TransactionHistoryTable';
import { ToastContainer, type ToastItem, type ToastVariant } from '../components/ui/toast';
import {
  clearTradingError,
  deleteTransaction,
  loadTradingBootstrap,
  submitBuyTrade,
  submitSellTrade,
  updateTransaction,
  updateDefaultBuyInputMode
} from '../store/tradingSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import type { TradeFormPayload, UpdateTransactionPayload } from '../types/trading';

export function TransactionsPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const { assets, exchanges, transactions, summary, loading, bootstrapAttempted, submitting, error, userPreferences } =
    useAppSelector((state) => state.trading);

  useEffect(() => {
    if (loading || bootstrapAttempted) {
      return;
    }
    void dispatch(loadTradingBootstrap());
  }, [bootstrapAttempted, dispatch, loading]);

  const showToast = useCallback((message: string, variant: ToastVariant): void => {
    const id = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    setToasts((current) => [...current, { id, message, variant }]);
    window.setTimeout(() => {
      setToasts((current) => current.filter((item) => item.id !== id));
    }, 3000);
  }, []);

  const submitEditTransaction = useCallback(
    async (transactionId: string, payload: UpdateTransactionPayload): Promise<boolean> => {
      dispatch(clearTradingError());
      const action = await dispatch(updateTransaction({ id: transactionId, payload }));
      const ok = updateTransaction.fulfilled.match(action);
      if (ok) {
        showToast('Transaction updated successfully.', 'success');
      } else {
        showToast('Failed to update transaction. Please try again.', 'error');
      }
      return ok;
    },
    [dispatch, showToast]
  );

  const submitDeleteTransaction = useCallback(
    async (transactionId: string): Promise<boolean> => {
      dispatch(clearTradingError());
      const action = await dispatch(deleteTransaction(transactionId));
      const ok = deleteTransaction.fulfilled.match(action);
      if (ok) {
        showToast('Transaction deleted successfully.', 'success');
      } else {
        showToast('Failed to delete transaction. Please try again.', 'error');
      }
      return ok;
    },
    [dispatch, showToast]
  );

  const submitSellFromTransaction = useCallback(
    async (payload: TradeFormPayload): Promise<boolean> => {
      dispatch(clearTradingError());
      const action = await dispatch(submitSellTrade(payload));
      const ok = submitSellTrade.fulfilled.match(action);
      if (ok) {
        showToast('Sell transaction created successfully.', 'success');
      } else {
        showToast('Failed to create sell transaction. Please try again.', 'error');
      }
      return ok;
    },
    [dispatch, showToast]
  );

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

        <TransactionHistoryTable
          transactions={transactions}
          assets={assets}
          exchanges={exchanges}
          onEditTransaction={submitEditTransaction}
          onDeleteTransaction={submitDeleteTransaction}
          onSellFromTransaction={submitSellFromTransaction}
        />
      </section>
      <ToastContainer items={toasts} />
    </main>
  );
}
