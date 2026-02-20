import { useMemo, useState } from 'react';
import type { TradeFormPayload, UpdateTransactionPayload } from '../types/trading';
import type { AssetOption, ExchangeOption, TransactionItem } from '../types/trading';
import { formatDateTime, formatNumber, formatUsd } from '../utils/format';
import { useAssetSpotPrices } from '../hooks/useAssetSpotPrices';
import { ConfirmDialog } from './ConfirmDialog';
import { EditTransactionModal } from './EditTransactionModal';
import { SellTransactionModal } from './SellTransactionModal';

interface TransactionHistoryTableProps {
  transactions: TransactionItem[];
  assets: AssetOption[];
  exchanges: ExchangeOption[];
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
  onEditTransaction,
  onDeleteTransaction,
  onSellFromTransaction
}: TransactionHistoryTableProps): JSX.Element {
  const [editTargetId, setEditTargetId] = useState<string | null>(null);
  const [sellTargetId, setSellTargetId] = useState<string | null>(null);
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

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
        <h3>Transactions</h3>
        {transactions.length === 0 ? (
          <p>No transactions yet.</p>
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
                  <th>Total</th>
                  <th>Realized PnL</th>
                  <th>Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx) => (
                  <tr key={tx.id}>
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
                      {(() => {
                        const symbol = assetSymbolById.get(tx.assetId);
                        if (!symbol) {
                          return '---';
                        }
                        const priceState = pricesBySymbol[symbol.toUpperCase()];
                        if (!priceState || priceState.status === 'loading') {
                          return <span className="table-price-loading" aria-label="Loading current price" />;
                        }
                        if (priceState.status === 'error' || !priceState.priceUsd) {
                          return '---';
                        }
                        return formatUsd(priceState.priceUsd);
                      })()}
                    </td>
                    <td>{formatUsd(tx.totalSpentUsd)}</td>
                    <td>{formatUsd(tx.realizedPnl)}</td>
                    <td>{formatDateTime(tx.transactionDate)}</td>
                    <td>
                      <div className="transaction-actions">
                        <button
                          type="button"
                          className="row-action-button row-action-edit"
                          onClick={() => setEditTargetId(tx.id)}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="row-action-button row-action-delete"
                          onClick={() => setDeleteTargetId(tx.id)}
                        >
                          Delete
                        </button>
                        {tx.transactionType === 'BUY' ? (
                          <button
                            type="button"
                            className="row-action-button row-action-sell"
                            onClick={() => setSellTargetId(tx.id)}
                          >
                            Sell
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

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
