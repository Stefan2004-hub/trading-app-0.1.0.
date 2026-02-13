import { request } from './http';
import type {
  AssetOption,
  ExchangeOption,
  PortfolioAssetPerformance,
  PortfolioSummary,
  TradeFormPayload,
  TransactionItem
} from '../types/trading';

function toTradeRequest(payload: TradeFormPayload): Record<string, unknown> {
  return {
    assetId: payload.assetId,
    exchangeId: payload.exchangeId,
    grossAmount: payload.grossAmount,
    feeAmount: payload.feeAmount ? payload.feeAmount : null,
    feeCurrency: payload.feeCurrency ? payload.feeCurrency : null,
    unitPriceUsd: payload.unitPriceUsd,
    transactionDate: payload.transactionDate ? payload.transactionDate : null
  };
}

export const tradingApi = {
  listAssets(): Promise<AssetOption[]> {
    return request<AssetOption[]>('/api/assets', { auth: false });
  },

  listExchanges(): Promise<ExchangeOption[]> {
    return request<ExchangeOption[]>('/api/exchanges', { auth: false });
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
    return request<TransactionItem>('/api/transactions/buy', {
      method: 'POST',
      body: toTradeRequest(payload)
    });
  },

  sell(payload: TradeFormPayload): Promise<TransactionItem> {
    return request<TransactionItem>('/api/transactions/sell', {
      method: 'POST',
      body: toTradeRequest(payload)
    });
  }
};
