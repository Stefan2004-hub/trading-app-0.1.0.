import { request } from './http';
import type {
  AssetOption,
  ExchangeOption,
  PortfolioAssetPerformance,
  PortfolioSummary,
  TradeFormPayload,
  TransactionItem
} from '../types/trading';

interface AuthorizedRequestOptions {
  accessToken: string;
}

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
    return request<AssetOption[]>('/api/assets');
  },

  listExchanges(): Promise<ExchangeOption[]> {
    return request<ExchangeOption[]>('/api/exchanges');
  },

  listTransactions({ accessToken }: AuthorizedRequestOptions): Promise<TransactionItem[]> {
    return request<TransactionItem[]>('/api/transactions', { accessToken });
  },

  getPortfolioSummary({ accessToken }: AuthorizedRequestOptions): Promise<PortfolioSummary> {
    return request<PortfolioSummary>('/api/portfolio/summary', { accessToken });
  },

  getPortfolioPerformance({ accessToken }: AuthorizedRequestOptions): Promise<PortfolioAssetPerformance[]> {
    return request<PortfolioAssetPerformance[]>('/api/portfolio/performance', { accessToken });
  },

  buy(payload: TradeFormPayload, { accessToken }: AuthorizedRequestOptions): Promise<TransactionItem> {
    return request<TransactionItem>('/api/transactions/buy', {
      method: 'POST',
      body: toTradeRequest(payload),
      accessToken
    });
  },

  sell(payload: TradeFormPayload, { accessToken }: AuthorizedRequestOptions): Promise<TransactionItem> {
    return request<TransactionItem>('/api/transactions/sell', {
      method: 'POST',
      body: toTradeRequest(payload),
      accessToken
    });
  }
};
