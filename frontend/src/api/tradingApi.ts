import { request } from './http';
import { decimalToFractionalPercent } from '../utils/decimal';
import type {
  AssetOption,
  ExchangeOption,
  PortfolioAssetPerformance,
  PortfolioSummary,
  TradeFormPayload,
  TransactionItem,
  UpdateTransactionPayload
} from '../types/trading';

function toBaseTradeRequest(payload: TradeFormPayload): Record<string, unknown> {
  const feePercentage = payload.feePercentage ? decimalToFractionalPercent(payload.feePercentage) : null;
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
  listAssets(search?: string): Promise<AssetOption[]> {
    const trimmedSearch = search?.trim();
    const query = trimmedSearch ? `?search=${encodeURIComponent(trimmedSearch)}` : '';
    return request<AssetOption[]>(`/api/assets${query}`);
  },

  createAsset(payload: { symbol: string; name: string }): Promise<AssetOption> {
    return request<AssetOption>('/api/assets', {
      method: 'POST',
      body: payload
    });
  },

  updateAsset(id: string, payload: { symbol: string; name: string }): Promise<AssetOption> {
    return request<AssetOption>(`/api/assets/${id}`, {
      method: 'PUT',
      body: payload
    });
  },

  deleteAsset(id: string): Promise<void> {
    return request<void>(`/api/assets/${id}`, {
      method: 'DELETE'
    });
  },

  listExchanges(search?: string): Promise<ExchangeOption[]> {
    const trimmedSearch = search?.trim();
    const query = trimmedSearch ? `?search=${encodeURIComponent(trimmedSearch)}` : '';
    return request<ExchangeOption[]>(`/api/exchanges${query}`);
  },

  createExchange(payload: { symbol: string; name: string }): Promise<ExchangeOption> {
    return request<ExchangeOption>('/api/exchanges', {
      method: 'POST',
      body: payload
    });
  },

  updateExchange(id: string, payload: { symbol: string; name: string }): Promise<ExchangeOption> {
    return request<ExchangeOption>(`/api/exchanges/${id}`, {
      method: 'PUT',
      body: payload
    });
  },

  deleteExchange(id: string): Promise<void> {
    return request<void>(`/api/exchanges/${id}`, {
      method: 'DELETE'
    });
  },

  listTransactions(search?: string): Promise<TransactionItem[]> {
    const trimmedSearch = search?.trim();
    const query = trimmedSearch ? `?search=${encodeURIComponent(trimmedSearch)}` : '';
    return request<TransactionItem[]>(`/api/transactions${query}`);
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
  },

  updateTransaction(id: string, payload: UpdateTransactionPayload): Promise<TransactionItem> {
    const feePercentage = payload.feePercentage ? decimalToFractionalPercent(payload.feePercentage) : null;
    const feeAmount = payload.feeAmount?.trim() ? payload.feeAmount.trim() : null;

    return request<TransactionItem>(`/api/transactions/${id}`, {
      method: 'PUT',
      body: {
        grossAmount: payload.grossAmount,
        feeAmount,
        feePercentage,
        unitPriceUsd: payload.unitPriceUsd
      }
    });
  },

  deleteTransaction(id: string): Promise<void> {
    return request<void>(`/api/transactions/${id}`, {
      method: 'DELETE'
    });
  }
};
