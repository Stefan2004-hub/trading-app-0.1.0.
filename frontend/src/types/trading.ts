export interface AssetOption {
  id: string;
  symbol: string;
  name: string;
}

export interface ExchangeOption {
  id: string;
  name: string;
}

export type TransactionType = 'BUY' | 'SELL';
export type BuyInputMode = 'COIN_AMOUNT' | 'USD_AMOUNT';

export interface TransactionItem {
  id: string;
  userId: string;
  assetId: string;
  exchangeId: string;
  transactionType: TransactionType;
  grossAmount: string;
  feeAmount: string | null;
  feePercentage: string | null;
  feeCurrency: string | null;
  netAmount: string;
  unitPriceUsd: string;
  totalSpentUsd: string;
  realizedPnl: string | null;
  transactionDate: string;
  matched: boolean;
  matchedTransactionId: string | null;
}

export interface PortfolioAssetPerformance {
  symbol: string;
  exchange: string;
  currentBalance: string;
  totalInvestedUsd: string;
  avgBuyPrice: string;
  currentUnitPriceUsd: string;
  currentValueUsd: string;
  unrealizedPnlUsd: string;
  unrealizedPnlPercent: string;
  realizedPnlUsd: string;
  totalPnlUsd: string;
}

export interface PortfolioSummary {
  totalInvestedUsd: string;
  totalCurrentValueUsd: string;
  totalUnrealizedPnlUsd: string;
  totalUnrealizedPnlPercent: string;
  totalRealizedPnlUsd: string;
  totalPnlUsd: string;
  assets: PortfolioAssetPerformance[];
}

export interface TradeFormPayload {
  assetId: string;
  exchangeId: string;
  grossAmount?: string;
  usdAmount?: string;
  inputMode?: BuyInputMode;
  feeAmount?: string;
  feePercentage?: string;
  feeCurrency?: string;
  unitPriceUsd: string;
  transactionDate?: string;
}

export interface UpdateTransactionPayload {
  grossAmount: string;
  feeAmount?: string;
  feePercentage?: string;
  unitPriceUsd: string;
}
