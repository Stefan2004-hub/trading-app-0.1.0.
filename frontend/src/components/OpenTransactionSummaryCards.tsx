import { useMemo } from 'react';
import { useAssetSpotPrices } from '../hooks/useAssetSpotPrices';
import type { AssetOption, TransactionItem } from '../types/trading';
import { formatUsd } from '../utils/format';

interface OpenTransactionSummaryCardsProps {
  transactions: TransactionItem[];
  assets: AssetOption[];
}

export function OpenTransactionSummaryCards({ transactions, assets }: OpenTransactionSummaryCardsProps): JSX.Element {
  const assetSymbolById = useMemo(() => {
    const map = new Map<string, string>();
    for (const asset of assets) {
      map.set(asset.id, asset.symbol.toUpperCase());
    }
    return map;
  }, [assets]);

  const openBuys = useMemo(
    () => transactions.filter((tx) => tx.transactionType === 'BUY' && !tx.matched),
    [transactions]
  );

  const symbols = useMemo(
    () =>
      Array.from(
        new Set(
          openBuys
            .map((tx) => assetSymbolById.get(tx.assetId))
            .filter((symbol): symbol is string => Boolean(symbol))
        )
      ),
    [assetSymbolById, openBuys]
  );
  const pricesBySymbol = useAssetSpotPrices(symbols);

  const totalInvested = useMemo(
    () => openBuys.reduce((sum, tx) => sum + (Number.isFinite(Number(tx.totalSpentUsd)) ? Number(tx.totalSpentUsd) : 0), 0),
    [openBuys]
  );

  const totalCurrentValue = useMemo(() => {
    let sum = 0;
    for (const tx of openBuys) {
      const symbol = assetSymbolById.get(tx.assetId);
      if (!symbol) {
        return null;
      }
      const priceState = pricesBySymbol[symbol];
      if (!priceState || priceState.status !== 'success' || !priceState.priceUsd) {
        return null;
      }
      const price = Number(priceState.priceUsd);
      const amount = Number(tx.netAmount);
      if (!Number.isFinite(price) || !Number.isFinite(amount)) {
        return null;
      }
      sum += price * amount;
    }
    return sum;
  }, [assetSymbolById, openBuys, pricesBySymbol]);

  const valueClassName =
    totalCurrentValue === null ? '' : totalCurrentValue >= totalInvested ? 'pnl-positive' : 'pnl-negative';

  return (
    <section className="cards-grid">
      <article className="metric-card">
        <h3>Total Invested</h3>
        <p>{formatUsd(String(totalInvested))}</p>
      </article>
      <article className="metric-card">
        <h3>Current Value</h3>
        <p className={valueClassName}>{totalCurrentValue === null ? '---' : formatUsd(String(totalCurrentValue))}</p>
      </article>
    </section>
  );
}
