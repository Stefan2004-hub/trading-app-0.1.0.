import { useEffect, useMemo, useState } from 'react';
import type {
  TradeFormPayload,
  UpdateTransactionNetAmountPayload,
  UpdateTransactionPayload
} from '../types/trading';
import type { AssetOption, ExchangeOption, TransactionItem } from '../types/trading';
import { formatDateTime, formatNumber, formatUsd } from '../utils/format';
import { useAssetSpotPrices } from '../hooks/useAssetSpotPrices';
import { ConfirmDialog } from './ConfirmDialog';
import { EditTransactionModal } from './EditTransactionModal';
import { EditTransactionQuantityModal } from './EditTransactionQuantityModal';
import { SellTransactionModal } from './SellTransactionModal';

interface TransactionHistoryTableProps {
  transactions: TransactionItem[];
  assets: AssetOption[];
  exchanges: ExchangeOption[];
  onEditTransactionQuantity: (
    transactionId: string,
    payload: UpdateTransactionNetAmountPayload
  ) => Promise<boolean>;
  onEditTransaction: (transactionId: string, payload: UpdateTransactionPayload) => Promise<boolean>;
  onDeleteTransaction: (transactionId: string) => Promise<boolean>;
  onSellFromTransaction: (payload: TradeFormPayload) => Promise<boolean>;
}

function labelAsset(assetId: string, assets: AssetOption[]): string {
  return assets.find((item) => item.id === assetId)?.symbol ?? assetId;
}

function labelExchange(exchangeId: string, exchanges: ExchangeOption[]): string {
  return exchanges.find((item) => item.id === exchangeId)?.name ?? exchangeId;
}

export function TransactionHistoryTable({
  transactions,
  assets,
  exchanges,
  onEditTransactionQuantity,
  onEditTransaction,
  onDeleteTransaction,
  onSellFromTransaction
}: TransactionHistoryTableProps): JSX.Element {
  const [showHistory, setShowHistory] = useState(false);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [editQuantityTargetId, setEditQuantityTargetId] = useState<string | null>(null);
  const [editTargetId, setEditTargetId] = useState<string | null>(null);
  const [sellTargetId, setSellTargetId] = useState<string | null>(null);
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!openMenuId) {
      return;
    }

    function handleDocumentPointerDown(event: MouseEvent): void {
      const target = event.target;
      if (!(target instanceof Element)) {
        setOpenMenuId(null);
        return;
      }
      const rowMenuContainer = target.closest(`[data-actions-row-id="${openMenuId}"]`);
      if (rowMenuContainer) {
        return;
      }
      setOpenMenuId(null);
    }

    document.addEventListener('mousedown', handleDocumentPointerDown);
    return () => {
      document.removeEventListener('mousedown', handleDocumentPointerDown);
    };
  }, [openMenuId]);

  const editQuantityTransaction = useMemo(
    () => transactions.find((item) => item.id === editQuantityTargetId) ?? null,
    [editQuantityTargetId, transactions]
  );
  const editTransaction = useMemo(
    () => transactions.find((item) => item.id === editTargetId) ?? null,
    [editTargetId, transactions]
  );
  const sellTransaction = useMemo(
    () => transactions.find((item) => item.id === sellTargetId) ?? null,
    [sellTargetId, transactions]
  );
  const deleteTransaction = useMemo(
    () => transactions.find((item) => item.id === deleteTargetId) ?? null,
    [deleteTargetId, transactions]
  );
  const assetSymbolById = useMemo(() => {
    const map = new Map<string, string>();
    for (const asset of assets) {
      map.set(asset.id, asset.symbol);
    }
    return map;
  }, [assets]);
  const symbolsToPrice = useMemo(
    () =>
      Array.from(
        new Set(
          transactions
            .map((tx) => assetSymbolById.get(tx.assetId))
            .filter((symbol): symbol is string => Boolean(symbol))
        )
      ),
    [assetSymbolById, transactions]
  );
  const pricesBySymbol = useAssetSpotPrices(symbolsToPrice);
  const transactionsById = useMemo(() => {
    const map = new Map<string, TransactionItem>();
    for (const tx of transactions) {
      map.set(tx.id, tx);
    }
    return map;
  }, [transactions]);
  const openBuyTransactions = useMemo(
    () => transactions.filter((tx) => tx.transactionType === 'BUY' && !tx.matched),
    [transactions]
  );
  const rowsToRender = useMemo(() => {
    if (!showHistory) {
      return openBuyTransactions.map((tx) => ({ tx, groupClassName: '' }));
    }

    const visited = new Set<string>();
    const groupedRows: Array<{ tx: TransactionItem; groupClassName: string }> = [];

    for (const tx of transactions) {
      if (visited.has(tx.id)) {
        continue;
      }

      const matchId = tx.matchedTransactionId;
      const matchTx = matchId ? transactionsById.get(matchId) : null;
      if (tx.matched && matchTx) {
        const buyTx = tx.transactionType === 'BUY' ? tx : matchTx;
        const sellTx = tx.transactionType === 'SELL' ? tx : matchTx;
        groupedRows.push({ tx: buyTx, groupClassName: 'matched-group-start' });
        groupedRows.push({ tx: sellTx, groupClassName: 'matched-group-end' });
        visited.add(buyTx.id);
        visited.add(sellTx.id);
      }
    }

    return groupedRows;
  }, [openBuyTransactions, showHistory, transactions, transactionsById]);

  async function confirmDelete(): Promise<void> {
    if (!deleteTargetId) {
      return;
    }
    setDeleting(true);
    const ok = await onDeleteTransaction(deleteTargetId);
    setDeleting(false);
    if (ok) {
      setDeleteTargetId(null);
    }
  }

  return (
    <>
      <section className="history-panel history-panel-prominent">
        <div className="transaction-table-header">
          <h3>Transactions</h3>
          <label className="checkbox-row transaction-history-toggle" htmlFor="show-transaction-history">
            <input
              id="show-transaction-history"
              type="checkbox"
              checked={showHistory}
              onChange={(event) => setShowHistory(event.target.checked)}
            />
            Show History
          </label>
        </div>
        {rowsToRender.length === 0 ? (
          <p>{showHistory ? 'No matched buy/sell history found.' : 'No open buy transactions.'}</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Asset</th>
                  <th>Exchange</th>
                  <th>Amount</th>
                  <th>Fee Amount</th>
                  <th>Fee %</th>
                  <th>Price</th>
                  <th>Current Price</th>
                  <th>Unrealized P&L</th>
                  <th>USD Invested</th>
                  <th>Remaining Dollars</th>
                  <th>Realized PnL</th>
                  <th>Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rowsToRender.map(({ tx, groupClassName }) => {
                  const symbol = assetSymbolById.get(tx.assetId);
                  const priceState = symbol ? pricesBySymbol[symbol.toUpperCase()] : undefined;
                  const currentPriceValue =
                    priceState && priceState.status === 'success' && priceState.priceUsd
                      ? Number(priceState.priceUsd)
                      : null;
                  const purchasePriceValue = Number(tx.unitPriceUsd);
                  const quantityValue = Number(tx.netAmount);
                  const isOpenBuy = tx.transactionType === 'BUY' && !tx.matched;
                  const unrealizedPnlValue =
                    isOpenBuy &&
                    currentPriceValue !== null &&
                    Number.isFinite(currentPriceValue) &&
                    Number.isFinite(purchasePriceValue) &&
                    Number.isFinite(quantityValue)
                      ? (currentPriceValue - purchasePriceValue) * quantityValue
                      : null;
                  const remainingDollarsValue =
                    isOpenBuy &&
                    currentPriceValue !== null &&
                    Number.isFinite(currentPriceValue) &&
                    Number.isFinite(quantityValue)
                      ? currentPriceValue * quantityValue
                      : null;
                  const usdInvestedValue = Number(tx.totalSpentUsd);
                  const remainingClassName =
                    remainingDollarsValue === null || !Number.isFinite(usdInvestedValue)
                      ? ''
                      : remainingDollarsValue >= usdInvestedValue
                        ? 'pnl-positive'
                        : 'pnl-negative';

                  return (
                    <tr key={tx.id} className={groupClassName}>
                      <td>{tx.transactionType}</td>
                      <td>{labelAsset(tx.assetId, assets)}</td>
                      <td>{labelExchange(tx.exchangeId, exchanges)}</td>
                      <td>{formatNumber(tx.netAmount)}</td>
                      <td>
                        {formatNumber(tx.feeAmount)}
                        {tx.feeCurrency ? ` ${tx.feeCurrency}` : ''}
                      </td>
                      <td>{tx.feePercentage ? `${(Number(tx.feePercentage) * 100).toFixed(4)}%` : '-'}</td>
                      <td>{formatUsd(tx.unitPriceUsd)}</td>
                      <td>
                        {!symbol || !priceState || priceState.status === 'error'
                          ? '---'
                          : priceState.status === 'loading'
                            ? <span className="table-price-loading" aria-label="Loading current price" />
                            : formatUsd(priceState.priceUsd)}
                      </td>
                      <td>
                        {unrealizedPnlValue === null ? (
                          '---'
                        ) : (
                          <span className={unrealizedPnlValue >= 0 ? 'pnl-positive' : 'pnl-negative'}>
                            {formatUsd(String(unrealizedPnlValue))}
                          </span>
                        )}
                      </td>
                      <td>{formatUsd(tx.totalSpentUsd)}</td>
                      <td className={remainingClassName}>
                        {remainingDollarsValue === null ? '---' : formatUsd(String(remainingDollarsValue))}
                      </td>
                      <td>{formatUsd(tx.realizedPnl)}</td>
                      <td>{formatDateTime(tx.transactionDate)}</td>
                      <td>
                        <div className="transaction-actions" data-actions-row-id={tx.id}>
                          <button
                            type="button"
                            className="row-action-button row-action-menu-trigger"
                            aria-haspopup="menu"
                            aria-expanded={openMenuId === tx.id}
                            aria-label="Open actions menu"
                            onClick={(event) => {
                              event.stopPropagation();
                              setOpenMenuId((current) => (current === tx.id ? null : tx.id));
                            }}
                          >
                            <svg viewBox="0 0 4 16" aria-hidden="true" focusable="false">
                              <circle cx="2" cy="2" r="1.25" />
                              <circle cx="2" cy="8" r="1.25" />
                              <circle cx="2" cy="14" r="1.25" />
                            </svg>
                          </button>
                          {openMenuId === tx.id ? (
                            <div className="transaction-actions-menu" role="menu" aria-label="Transaction actions">
                              <button
                                type="button"
                                className="transaction-actions-menu-item"
                                role="menuitem"
                                onClick={() => {
                                  setEditQuantityTargetId(tx.id);
                                  setOpenMenuId(null);
                                }}
                              >
                                Edit Quantity
                              </button>
                              <button
                                type="button"
                                className="transaction-actions-menu-item"
                                role="menuitem"
                                onClick={() => {
                                  setEditTargetId(tx.id);
                                  setOpenMenuId(null);
                                }}
                              >
                                Edit Full Transaction
                              </button>
                              <button
                                type="button"
                                className="transaction-actions-menu-item transaction-actions-menu-item-danger"
                                role="menuitem"
                                onClick={() => {
                                  setDeleteTargetId(tx.id);
                                  setOpenMenuId(null);
                                }}
                              >
                                Delete
                              </button>
                              <button
                                type="button"
                                className="transaction-actions-menu-item transaction-actions-menu-item-success"
                                role="menuitem"
                                disabled={!(tx.transactionType === 'BUY' && !tx.matched)}
                                onClick={() => {
                                  setSellTargetId(tx.id);
                                  setOpenMenuId(null);
                                }}
                              >
                                Sell
                              </button>
                            </div>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <EditTransactionQuantityModal
        open={editQuantityTransaction !== null}
        transaction={editQuantityTransaction}
        assetLabel={editQuantityTransaction ? labelAsset(editQuantityTransaction.assetId, assets) : ''}
        onClose={() => setEditQuantityTargetId(null)}
        onSubmit={onEditTransactionQuantity}
      />

      <EditTransactionModal
        open={editTransaction !== null}
        transaction={editTransaction}
        assetLabel={editTransaction ? labelAsset(editTransaction.assetId, assets) : ''}
        exchangeLabel={editTransaction ? labelExchange(editTransaction.exchangeId, exchanges) : ''}
        onClose={() => setEditTargetId(null)}
        onSubmit={onEditTransaction}
      />

      <SellTransactionModal
        open={sellTransaction !== null}
        transaction={sellTransaction}
        assetLabel={sellTransaction ? labelAsset(sellTransaction.assetId, assets) : ''}
        exchangeLabel={sellTransaction ? labelExchange(sellTransaction.exchangeId, exchanges) : ''}
        onClose={() => setSellTargetId(null)}
        onSubmit={onSellFromTransaction}
      />

      <ConfirmDialog
        open={deleteTransaction !== null}
        title="Confirm Delete"
        message="Delete this transaction? This action cannot be undone."
        confirmText="Delete"
        loading={deleting}
        onCancel={() => setDeleteTargetId(null)}
        onConfirm={() => {
          void confirmDelete();
        }}
      />
    </>
  );
}
