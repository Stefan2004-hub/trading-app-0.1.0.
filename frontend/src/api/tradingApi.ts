import { request } from './http';
import type {
  AssetOption,
  ExchangeOption,
  PortfolioAssetPerformance,
  PortfolioSummary,
  TradeFormPayload,
  TransactionItem
} from '../types/trading';

function toBaseTradeRequest(payload: TradeFormPayload): Record<string, unknown> {
  const feePercentage =
    payload.feePercentage && Number.isFinite(Number(payload.feePercentage))
      ? (Number(payload.feePercentage) / 100).toString()
      : null;
  const feeAmount = payload.feeAmount?.trim() ? payload.feeAmount.trim() : null;

  return {
    assetId: payload.assetId,
    exchangeId: payload.exchangeId,
    feeAmount,
    feePercentage,
    feeCurrency: payload.feeCurrency ? payload.feeCurrency : null,
    unitPriceUsd: payload.unitPriceUsd,
    transactionDate: payload.transactionDate ? payload.transactionDate : null
  };
}

export const tradingApi = {
  listAssets(): Promise<AssetOption[]> {
    return request<AssetOption[]>('/api/assets');
  },

  listExchanges(): Promise<ExchangeOption[]> {
    return request<ExchangeOption[]>('/api/exchanges');
  },

  listTransactions(): Promise<TransactionItem[]> {
    return request<TransactionItem[]>('/api/transactions');
  },

  getPortfolioSummary(): Promise<PortfolioSummary> {
    return request<PortfolioSummary>('/api/portfolio/summary');
  },

  getPortfolioPerformance(): Promise<PortfolioAssetPerformance[]> {
    return request<PortfolioAssetPerformance[]>('/api/portfolio/performance');
  },

  buy(payload: TradeFormPayload): Promise<TransactionItem> {
    const base = toBaseTradeRequest(payload);
    return request<TransactionItem>('/api/transactions/buy', {
      method: 'POST',
      body: {
        ...base,
        grossAmount: payload.grossAmount ? payload.grossAmount : null,
        usdAmount: payload.usdAmount ? payload.usdAmount : null,
        inputMode: payload.inputMode ? payload.inputMode : 'COIN_AMOUNT'
      }
    });
  },

  sell(payload: TradeFormPayload): Promise<TransactionItem> {
    const base = toBaseTradeRequest(payload);
    return request<TransactionItem>('/api/transactions/sell', {
      method: 'POST',
      body: {
        ...base,
        grossAmount: payload.grossAmount
      }
    });
  }
};
