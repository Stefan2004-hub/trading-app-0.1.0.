import { useCallback, useEffect, useState } from 'react';
import { AppHeader } from '../components/AppHeader';
import { BuyTransactionModal } from '../components/BuyTransactionModal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { OpenTransactionSummaryCards } from '../components/OpenTransactionSummaryCards';
import { TransactionHistoryTable } from '../components/TransactionHistoryTable';
import { ToastContainer, type ToastItem, type ToastVariant } from '../components/ui/toast';
import { tradingApi } from '../api/tradingApi';
import { multiplyDecimal } from '../utils/decimal';
import {
  clearTradingError,
  deleteTransaction,
  loadTransactions,
  loadTradingBootstrap,
  openAccumulationTrade,
  submitBuyAndCloseAccumulation,
  submitBuyTrade,
  submitSellTrade,
  updateTransaction,
  updateTransactionNetAmount,
  updateDefaultBuyInputMode
} from '../store/tradingSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import type {
  SortDirection,
  TradeFormPayload,
  TransactionColumnKey,
  TransactionSortBy,
  TransactionView,
  UpdateTransactionNetAmountPayload,
  UpdateTransactionPayload
} from '../types/trading';

const DEFAULT_VISIBLE_COLUMNS: TransactionColumnKey[] = [
  'type',
  'asset',
  'exchange',
  'amount',
  'price',
  'currentPrice',
  'unrealizedPnl',
  'usdInvested',
  'date',
  'actions'
];

const ALL_TRANSACTION_COLUMNS: TransactionColumnKey[] = [
  'type',
  'asset',
  'exchange',
  'amount',
  'feeAmount',
  'feePercent',
  'price',
  'currentPrice',
  'unrealizedPnl',
  'usdInvested',
  'remainingDollars',
  'realizedPnl',
  'date',
  'actions'
];

const STORAGE_KEY = 'trading.transactions.visibleColumns';

function sanitizeVisibleColumns(parsed: unknown): TransactionColumnKey[] {
  if (!Array.isArray(parsed)) {
    return [...DEFAULT_VISIBLE_COLUMNS];
  }

  const validColumns = new Set(ALL_TRANSACTION_COLUMNS);
  const selectedColumns = new Set<TransactionColumnKey>();
  for (const column of parsed) {
    if (typeof column !== 'string' || !validColumns.has(column as TransactionColumnKey)) {
      return [...DEFAULT_VISIBLE_COLUMNS];
    }
    selectedColumns.add(column as TransactionColumnKey);
  }
  selectedColumns.add('actions');
  return ALL_TRANSACTION_COLUMNS.filter((column) => selectedColumns.has(column));
}

function loadVisibleColumns(): TransactionColumnKey[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return [...DEFAULT_VISIBLE_COLUMNS];
    return sanitizeVisibleColumns(JSON.parse(stored));
  } catch {
    return [...DEFAULT_VISIBLE_COLUMNS];
  }
}

function saveVisibleColumns(columns: TransactionColumnKey[]): void {
  const normalizedColumns = sanitizeVisibleColumns(columns);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalizedColumns));
}

function nextLocalDate(date: string): string {
  const [year, month, day] = date.split('-').map(Number);
  const nextDate = new Date(year, month - 1, day + 1);
  const nextYear = nextDate.getFullYear();
  const nextMonth = String(nextDate.getMonth() + 1).padStart(2, '0');
  const nextDay = String(nextDate.getDate()).padStart(2, '0');
  return `${nextYear}-${nextMonth}-${nextDay}`;
}

export function TransactionsPage(): JSX.Element {
  const defaultPageSize = 20;
  const dispatch = useAppDispatch();
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const [isBuyModalOpen, setIsBuyModalOpen] = useState(false);
  const [buyModalContext, setBuyModalContext] = useState<{
    accumulationTradeId: string | null;
    exitTransactionId?: string;
    initialAssetId?: string;
    initialExchangeId?: string;
    initialUsdAmount?: string;
  }>({
    accumulationTradeId: null
  });
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(defaultPageSize);
  const [transactionView, setTransactionView] = useState<TransactionView>('OPEN');
  const [isCleanHistoryConfirmOpen, setIsCleanHistoryConfirmOpen] = useState(false);
  const [cleaningHistory, setCleaningHistory] = useState(false);
  const [sortBy, setSortBy] = useState<TransactionSortBy>('date');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [visibleColumns, setVisibleColumns] = useState<TransactionColumnKey[]>(() => loadVisibleColumns());
  const {
    assets,
    exchanges,
    transactions,
    accumulationTrades,
    loading,
    bootstrapAttempted,
    submitting,
    error,
    userPreferences,
    transactionTotalPages,
    transactionTotalElements
  } = useAppSelector((state) => state.trading);
  const authUserId = useAppSelector((state) => state.auth.user?.userId);
  const dateFromInclusive = dateFrom ? new Date(`${dateFrom}T00:00:00`).toISOString() : undefined;
  const dateToExclusive = dateTo ? new Date(`${nextLocalDate(dateTo)}T00:00:00`).toISOString() : undefined;

  useEffect(() => {
    if (loading || bootstrapAttempted) {
      return;
    }
    void dispatch(loadTradingBootstrap(authUserId));
  }, [authUserId, bootstrapAttempted, dispatch, loading]);

  useEffect(() => {
    if (!bootstrapAttempted) {
      return;
    }
    const timeoutId = window.setTimeout(() => {
      void dispatch(
        loadTransactions({
          page: currentPage,
          size: pageSize,
          search: searchTerm.trim() ? searchTerm : undefined,
          view: transactionView,
          groupSize: transactionView === 'MATCHED' ? pageSize : undefined,
          sortBy,
          sortDirection,
          dateFromInclusive,
          dateToExclusive
        })
      );
    }, 250);
    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [bootstrapAttempted, currentPage, dispatch, pageSize, searchTerm, transactionView, sortBy, sortDirection, dateFromInclusive, dateToExclusive]);

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
        search: searchTerm.trim() ? searchTerm : undefined,
        view: transactionView,
        groupSize: transactionView === 'MATCHED' ? pageSize : undefined,
        sortBy,
        sortDirection,
        dateFromInclusive,
        dateToExclusive
      })
    ).unwrap();
  }, [currentPage, dispatch, pageSize, searchTerm, transactionView, sortBy, sortDirection, dateFromInclusive, dateToExclusive]);

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
      const action = await dispatch(updateTransaction({ id: transactionId, payload, userId: authUserId }));
      const ok = updateTransaction.fulfilled.match(action);
      if (ok) {
        showToast('Transaction updated successfully.', 'success');
        await refreshTransactionsForSearch();
      } else {
        showToast('Failed to update transaction. Please try again.', 'error');
      }
      return ok;
    },
    [authUserId, dispatch, refreshTransactionsForSearch, showToast]
  );

  const submitEditTransactionQuantity = useCallback(
    async (transactionId: string, payload: UpdateTransactionNetAmountPayload): Promise<boolean> => {
      dispatch(clearTradingError());
      const action = await dispatch(updateTransactionNetAmount({ id: transactionId, payload, userId: authUserId }));
      const ok = updateTransactionNetAmount.fulfilled.match(action);
      if (ok) {
        showToast('Transaction quantity updated successfully.', 'success');
        await refreshTransactionsForSearch();
      } else {
        showToast('Failed to update transaction quantity. Please try again.', 'error');
      }
      return ok;
    },
    [authUserId, dispatch, refreshTransactionsForSearch, showToast]
  );

  const submitDeleteTransaction = useCallback(
    async (transactionId: string): Promise<boolean> => {
      dispatch(clearTradingError());
      const action = await dispatch(deleteTransaction({ id: transactionId, userId: authUserId }));
      const ok = deleteTransaction.fulfilled.match(action);
      if (ok) {
        showToast('Transaction deleted successfully.', 'success');
        await refreshTransactionsForSearch();
      } else {
        showToast('Failed to delete transaction. Please try again.', 'error');
      }
      return ok;
    },
    [authUserId, dispatch, refreshTransactionsForSearch, showToast]
  );

  const submitSellFromTransaction = useCallback(
    async (payload: TradeFormPayload): Promise<boolean> => {
      dispatch(clearTradingError());
      const action = await dispatch(submitSellTrade({ payload, userId: authUserId }));
      const ok = submitSellTrade.fulfilled.match(action);
      if (ok) {
        showToast('Sell transaction created successfully.', 'success');
        await refreshTransactionsForSearch();
      } else {
        showToast('Failed to create sell transaction. Please try again.', 'error');
      }
      return ok;
    },
    [authUserId, dispatch, refreshTransactionsForSearch, showToast]
  );

  const submitOpenAccumulationTrade = useCallback(
    async (exitTransactionId: string): Promise<boolean> => {
      dispatch(clearTradingError());
      const action = await dispatch(openAccumulationTrade({ exitTransactionId, userId: authUserId }));
      const ok = openAccumulationTrade.fulfilled.match(action);
      if (ok) {
        showToast('Accumulation trade opened successfully.', 'success');
        await refreshTransactionsForSearch();
      } else {
        showToast('Failed to open accumulation trade. Please try again.', 'error');
      }
      return ok;
    },
    [authUserId, dispatch, refreshTransactionsForSearch, showToast]
  );

  const handleSortChange = useCallback((newSortBy: TransactionSortBy, newSortDirection: SortDirection): void => {
    setSortBy(newSortBy);
    setSortDirection(newSortDirection);
    setCurrentPage(0);
  }, []);

  const handleVisibleColumnsChange = useCallback((columns: TransactionColumnKey[]): void => {
    setVisibleColumns(columns);
    saveVisibleColumns(columns);
  }, []);

  const clearDateFilter = useCallback((): void => {
    setDateFrom('');
    setDateTo('');
    setCurrentPage(0);
  }, []);

  const submitCleanHistory = useCallback(async (): Promise<void> => {
    setCleaningHistory(true);
    dispatch(clearTradingError());
    try {
      const { blob, fileName } = await tradingApi.cleanHistory();
      const objectUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(objectUrl);

      await dispatch(loadTradingBootstrap(authUserId)).unwrap();
      await refreshTransactionsForSearch();
      showToast('History purged. Check your device for the backup file.', 'success');
      setIsCleanHistoryConfirmOpen(false);
    } catch {
      showToast('Failed to clean history. Please try again.', 'error');
    } finally {
      setCleaningHistory(false);
    }
  }, [authUserId, dispatch, refreshTransactionsForSearch, showToast]);

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel transactions-workspace-panel">
        <h1>Trading</h1>
        {error ? <p className="auth-error">{error}</p> : null}

        {transactionView === 'OPEN' ? <OpenTransactionSummaryCards transactions={transactions} assets={assets} /> : null}

        <section className="transactions-title-row">
          <h2>Transaction List</h2>
          <div className="transactions-title-actions">
            <button
              type="button"
              className="clean-history-button"
              onClick={() => setIsCleanHistoryConfirmOpen(true)}
              disabled={cleaningHistory}
            >
              Clean History
            </button>
            <button
              type="button"
              className="open-buy-modal-button"
              onClick={() => {
                setBuyModalContext({ accumulationTradeId: null });
                setIsBuyModalOpen(true);
              }}
            >
              Buy
            </button>
          </div>
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

        <div className="date-filter-row">
          <div className="date-filter-field">
            <label htmlFor="date-from">From</label>
            <input
              id="date-from"
              type="date"
              className="search-input date-input"
              value={dateFrom}
              onChange={(e) => {
                setDateFrom(e.target.value);
                setCurrentPage(0);
              }}
            />
          </div>
          <div className="date-filter-field">
            <label htmlFor="date-to">To</label>
            <input
              id="date-to"
              type="date"
              className="search-input date-input"
              value={dateTo}
              onChange={(e) => {
                setDateTo(e.target.value);
                setCurrentPage(0);
              }}
            />
          </div>
          {(dateFrom || dateTo) ? (
            <button type="button" className="date-filter-clear" onClick={clearDateFilter}>
              Clear
            </button>
          ) : null}
        </div>

        <TransactionHistoryTable
          transactions={transactions}
          transactionView={transactionView}
          onTransactionViewChange={(view) => {
            setTransactionView(view);
            setCurrentPage(0);
          }}
          accumulationTrades={accumulationTrades}
          assets={assets}
          exchanges={exchanges}
          onEditTransactionQuantity={submitEditTransactionQuantity}
          onEditTransaction={submitEditTransaction}
          onDeleteTransaction={submitDeleteTransaction}
          onSellFromTransaction={submitSellFromTransaction}
          onOpenAccumulationTrade={submitOpenAccumulationTrade}
          visibleColumns={visibleColumns}
          onVisibleColumnsChange={handleVisibleColumnsChange}
          sortBy={sortBy}
          sortDirection={sortDirection}
          onSortChange={handleSortChange}
          onCompleteAccumulationTrade={({ accumulationTradeId, exitTransactionId, assetId, exchangeId }) => {
            const exitTransaction = transactions.find((tx) => tx.id === exitTransactionId) ?? null;
            const inheritedUsdAmount =
              exitTransaction ? multiplyDecimal(exitTransaction.grossAmount, exitTransaction.unitPriceUsd) ?? exitTransaction.totalSpentUsd : undefined;
            setBuyModalContext({
              accumulationTradeId,
              exitTransactionId,
              initialAssetId: assetId,
              initialExchangeId: exchangeId,
              initialUsdAmount: inheritedUsdAmount
            });
            void dispatch(updateDefaultBuyInputMode('USD_AMOUNT'));
            setIsBuyModalOpen(true);
          }}
        />

        <div className="transactions-pagination-footer">
          <label htmlFor="transactions-page-size">
            {transactionView === 'MATCHED' ? 'Pairs per page' : 'Rows per page'}
          </label>
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
            {' '}(
            {transactionView === 'MATCHED' ? Math.floor(transactionTotalElements / 2) : transactionTotalElements}
            {' '}
            {transactionView === 'MATCHED' ? 'pairs' : 'total'})
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
          title={buyModalContext.accumulationTradeId ? 'Complete Accumulation' : 'Buy Transaction'}
          submitLabel={buyModalContext.accumulationTradeId ? 'Buy and Complete' : 'Buy'}
          assets={assets}
          exchanges={exchanges}
          submitting={submitting}
          defaultBuyInputMode={userPreferences?.defaultBuyInputMode}
          forcedBuyInputMode={buyModalContext.accumulationTradeId ? 'USD_AMOUNT' : undefined}
          initialAssetId={buyModalContext.initialAssetId}
          initialExchangeId={buyModalContext.initialExchangeId}
          initialUsdAmount={buyModalContext.initialUsdAmount}
          onClose={() => {
            setIsBuyModalOpen(false);
            setBuyModalContext({ accumulationTradeId: null });
          }}
          onBuyInputModeChange={(mode) => {
            void dispatch(updateDefaultBuyInputMode(mode));
          }}
          onSubmit={async (payload) => {
            dispatch(clearTradingError());
            if (!buyModalContext.accumulationTradeId) {
              const action = await dispatch(submitBuyTrade({ payload, userId: authUserId }));
              const ok = submitBuyTrade.fulfilled.match(action);
              if (ok) {
                showToast('Buy transaction created successfully.', 'success');
                await refreshTransactionsForSearch();
              } else {
                showToast('Failed to create buy transaction. Please try again.', 'error');
              }
              return ok;
            }

            const action = await dispatch(
              submitBuyAndCloseAccumulation({
                payload,
                accumulationTradeId: buyModalContext.accumulationTradeId,
                userId: authUserId
              })
            );
            const ok = submitBuyAndCloseAccumulation.fulfilled.match(action);
            if (ok) {
              showToast('Accumulation trade completed successfully.', 'success');
              await refreshTransactionsForSearch();
              return true;
            }

            showToast('Failed to complete accumulation trade. Please try again.', 'error');
            await refreshTransactionsForSearch();
            return false;
          }}
        />
        <ConfirmDialog
          open={isCleanHistoryConfirmOpen}
          title="Clean History"
          message="This will export your matched BUY/SELL history to Excel and permanently remove those closed transactions and linked accumulation trades. Open BUY positions are kept."
          confirmText="Clean History"
          loadingText="Generating Report & Cleaning..."
          loading={cleaningHistory}
          onCancel={() => {
            if (!cleaningHistory) {
              setIsCleanHistoryConfirmOpen(false);
            }
          }}
          onConfirm={() => {
            void submitCleanHistory();
          }}
        />
      </section>
      <ToastContainer items={toasts} />
    </main>
  );
}
