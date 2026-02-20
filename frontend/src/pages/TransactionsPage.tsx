import { useCallback, useEffect, useState } from 'react';
import { AppHeader } from '../components/AppHeader';
import { BuyTransactionModal } from '../components/BuyTransactionModal';
import { OpenTransactionSummaryCards } from '../components/OpenTransactionSummaryCards';
import { TransactionHistoryTable } from '../components/TransactionHistoryTable';
import { ToastContainer, type ToastItem, type ToastVariant } from '../components/ui/toast';
import {
  clearTradingError,
  deleteTransaction,
  loadTransactions,
  loadTradingBootstrap,
  submitBuyTrade,
  submitSellTrade,
  updateTransaction,
  updateTransactionNetAmount,
  updateDefaultBuyInputMode
} from '../store/tradingSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import type {
  TradeFormPayload,
  UpdateTransactionNetAmountPayload,
  UpdateTransactionPayload
} from '../types/trading';

export function TransactionsPage(): JSX.Element {
  const defaultPageSize = 20;
  const dispatch = useAppDispatch();
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const [isBuyModalOpen, setIsBuyModalOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(defaultPageSize);
  const {
    assets,
    exchanges,
    transactions,
    loading,
    bootstrapAttempted,
    submitting,
    error,
    userPreferences,
    transactionTotalPages,
    transactionTotalElements
  } = useAppSelector((state) => state.trading);

  useEffect(() => {
    if (loading || bootstrapAttempted) {
      return;
    }
    void dispatch(loadTradingBootstrap());
  }, [bootstrapAttempted, dispatch, loading]);

  useEffect(() => {
    if (!bootstrapAttempted) {
      return;
    }
    const timeoutId = window.setTimeout(() => {
      void dispatch(
        loadTransactions({
          page: currentPage,
          size: pageSize,
          search: searchTerm.trim() ? searchTerm : undefined
        })
      );
    }, 250);
    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [bootstrapAttempted, currentPage, dispatch, pageSize, searchTerm]);

  useEffect(() => {
    if (transactionTotalPages > 0 && currentPage >= transactionTotalPages) {
      setCurrentPage(transactionTotalPages - 1);
    }
  }, [currentPage, transactionTotalPages]);

  const refreshTransactionsForSearch = useCallback(async (): Promise<void> => {
    await dispatch(
      loadTransactions({
        page: currentPage,
        size: pageSize,
        search: searchTerm.trim() ? searchTerm : undefined
      })
    ).unwrap();
  }, [currentPage, dispatch, pageSize, searchTerm]);

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
        await refreshTransactionsForSearch();
      } else {
        showToast('Failed to update transaction. Please try again.', 'error');
      }
      return ok;
    },
    [dispatch, refreshTransactionsForSearch, showToast]
  );

  const submitEditTransactionQuantity = useCallback(
    async (transactionId: string, payload: UpdateTransactionNetAmountPayload): Promise<boolean> => {
      dispatch(clearTradingError());
      const action = await dispatch(updateTransactionNetAmount({ id: transactionId, payload }));
      const ok = updateTransactionNetAmount.fulfilled.match(action);
      if (ok) {
        showToast('Transaction quantity updated successfully.', 'success');
        await refreshTransactionsForSearch();
      } else {
        showToast('Failed to update transaction quantity. Please try again.', 'error');
      }
      return ok;
    },
    [dispatch, refreshTransactionsForSearch, showToast]
  );

  const submitDeleteTransaction = useCallback(
    async (transactionId: string): Promise<boolean> => {
      dispatch(clearTradingError());
      const action = await dispatch(deleteTransaction(transactionId));
      const ok = deleteTransaction.fulfilled.match(action);
      if (ok) {
        showToast('Transaction deleted successfully.', 'success');
        await refreshTransactionsForSearch();
      } else {
        showToast('Failed to delete transaction. Please try again.', 'error');
      }
      return ok;
    },
    [dispatch, refreshTransactionsForSearch, showToast]
  );

  const submitSellFromTransaction = useCallback(
    async (payload: TradeFormPayload): Promise<boolean> => {
      dispatch(clearTradingError());
      const action = await dispatch(submitSellTrade(payload));
      const ok = submitSellTrade.fulfilled.match(action);
      if (ok) {
        showToast('Sell transaction created successfully.', 'success');
        await refreshTransactionsForSearch();
      } else {
        showToast('Failed to create sell transaction. Please try again.', 'error');
      }
      return ok;
    },
    [dispatch, refreshTransactionsForSearch, showToast]
  );

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel transactions-workspace-panel">
        <h1>Trading</h1>
        {error ? <p className="auth-error">{error}</p> : null}

        <OpenTransactionSummaryCards transactions={transactions} assets={assets} />

        <section className="transactions-title-row">
          <h2>Transaction List</h2>
          <button type="button" className="open-buy-modal-button" onClick={() => setIsBuyModalOpen(true)}>
            Buy
          </button>
        </section>

        <div className="search-controls">
          <label htmlFor="transaction-search">Search by Asset or Exchange</label>
          <input
            id="transaction-search"
            className="search-input"
            type="search"
            placeholder="e.g. BTC, AAPL, Binance, Nasdaq"
            value={searchTerm}
            onChange={(event) => {
              setSearchTerm(event.target.value);
              setCurrentPage(0);
            }}
          />
        </div>

        <TransactionHistoryTable
          transactions={transactions}
          assets={assets}
          exchanges={exchanges}
          onEditTransactionQuantity={submitEditTransactionQuantity}
          onEditTransaction={submitEditTransaction}
          onDeleteTransaction={submitDeleteTransaction}
          onSellFromTransaction={submitSellFromTransaction}
        />

        <div className="transactions-pagination-footer">
          <label htmlFor="transactions-page-size">Rows per page</label>
          <select
            id="transactions-page-size"
            className="transactions-page-size-select"
            value={pageSize}
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
            Page {transactionTotalPages === 0 ? 0 : currentPage + 1} of {transactionTotalPages}
            {' '}({transactionTotalElements} total)
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
              disabled={loading || currentPage + 1 >= transactionTotalPages}
            >
              Next
            </button>
          </div>
        </div>

        <BuyTransactionModal
          open={isBuyModalOpen}
          assets={assets}
          exchanges={exchanges}
          submitting={submitting}
          defaultBuyInputMode={userPreferences?.defaultBuyInputMode}
          onClose={() => setIsBuyModalOpen(false)}
          onBuyInputModeChange={(mode) => {
            void dispatch(updateDefaultBuyInputMode(mode));
          }}
          onSubmit={async (payload) => {
            dispatch(clearTradingError());
            const action = await dispatch(submitBuyTrade(payload));
            const ok = submitBuyTrade.fulfilled.match(action);
            if (ok) {
              showToast('Buy transaction created successfully.', 'success');
              await refreshTransactionsForSearch();
            } else {
              showToast('Failed to create buy transaction. Please try again.', 'error');
            }
            return ok;
          }}
        />
      </section>
      <ToastContainer items={toasts} />
    </main>
  );
}
