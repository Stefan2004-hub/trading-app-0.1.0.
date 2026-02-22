export interface AssetOption {
  id: string;
  symbol: string;
  name: string;
}

export interface ExchangeOption {
  id: string;
  symbol: string;
  name: string;
}

export interface PricePeakItem {
  id: string;
  userId: string;
  assetId: string;
  assetSymbol: string;
  assetName: string;
  lastBuyTransactionId: string | null;
  peakPrice: string;
  peakTimestamp: string;
  active: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface UpdatePricePeakPayload {
  peakPrice: string;
  peakTimestamp: string;
  active: boolean;
}

export type TransactionType = 'BUY' | 'SELL';
export type TransactionView = 'OPEN' | 'MATCHED';
export type BuyInputMode = 'COIN_AMOUNT' | 'USD_AMOUNT';
export type AccumulationTradeStatus = 'OPEN' | 'CLOSED' | 'CANCELLED';
export type TransactionAccumulationRole = 'NONE' | 'ACCUMULATION_EXIT' | 'ACCUMULATION_REENTRY';

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
  accumulationLinked: boolean;
  accumulationRole: TransactionAccumulationRole;
}

export interface AccumulationTradeItem {
  id: string;
  userId: string;
  assetId: string;
  exitTransactionId: string;
  reentryTransactionId: string | null;
  oldCoinAmount: string;
  newCoinAmount: string | null;
  accumulationDelta: string | null;
  status: AccumulationTradeStatus;
  exitPriceUsd: string;
  reentryPriceUsd: string | null;
  createdAt: string;
  closedAt: string | null;
  predictionNotes: string | null;
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

export interface AssetSummary {
  assetName: string;
  assetSymbol: string;
  netQuantity: string;
  totalInvested: string;
  totalRealizedProfit: string;
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

export interface UpdateTransactionNetAmountPayload {
  netAmount: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
