import type { AssetOption, ExchangeOption, TransactionItem } from '../types/trading';
import { formatDateTime, formatNumber, formatUsd } from '../utils/format';

interface TransactionHistoryTableProps {
  transactions: TransactionItem[];
  assets: AssetOption[];
  exchanges: ExchangeOption[];
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
  exchanges
}: TransactionHistoryTableProps): JSX.Element {
  return (
    <section className="history-panel">
      <h3>Transaction History</h3>
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
                <th>Total</th>
                <th>Realized PnL</th>
                <th>Date</th>
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
                  <td>{formatUsd(tx.totalSpentUsd)}</td>
                  <td>{formatUsd(tx.realizedPnl)}</td>
                  <td>{formatDateTime(tx.transactionDate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
