import { request } from './http';
import { decimalToFractionalPercent } from '../utils/decimal';
import type {
  AccumulationTradeItem,
  AccumulationTradeStatus,
  AssetOption,
  ExchangeOption,
  PaginatedResponse,
  PortfolioAssetPerformance,
  PortfolioSummary,
  TradeFormPayload,
  TransactionItem,
  UpdateTransactionNetAmountPayload,
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

  listTransactions(params?: { page?: number; size?: number; search?: string }): Promise<PaginatedResponse<TransactionItem>> {
    const queryParams = new URLSearchParams();
    queryParams.set('page', String(params?.page ?? 0));
    queryParams.set('size', String(params?.size ?? 20));

    const trimmedSearch = params?.search?.trim();
    if (trimmedSearch) {
      queryParams.set('search', trimmedSearch);
    }

    return request<PaginatedResponse<TransactionItem>>(`/api/transactions?${queryParams.toString()}`);
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

  updateTransactionNetAmount(id: string, payload: UpdateTransactionNetAmountPayload): Promise<TransactionItem> {
    return request<TransactionItem>(`/api/transactions/${id}/net-amount`, {
      method: 'PATCH',
      body: {
        netAmount: payload.netAmount
      }
    });
  },

  deleteTransaction(id: string): Promise<void> {
    return request<void>(`/api/transactions/${id}`, {
      method: 'DELETE'
    });
  },

  listAccumulationTrades(params?: {
    status?: AccumulationTradeStatus;
    userId?: string;
  }): Promise<AccumulationTradeItem[]> {
    const queryParams = new URLSearchParams();
    if (params?.status) {
      queryParams.set('status', params.status);
    }
    if (params?.userId) {
      queryParams.set('userId', params.userId);
    }
    const query = queryParams.toString();
    return request<AccumulationTradeItem[]>(`/api/accumulation-trades${query ? `?${query}` : ''}`);
  },

  openAccumulationTrade(payload: {
    exitTransactionId: string;
    predictionNotes?: string;
    userId?: string;
  }): Promise<AccumulationTradeItem> {
    const query = payload.userId ? `?userId=${encodeURIComponent(payload.userId)}` : '';
    return request<AccumulationTradeItem>(`/api/accumulation-trades/open${query}`, {
      method: 'POST',
      body: {
        exitTransactionId: payload.exitTransactionId,
        predictionNotes: payload.predictionNotes ?? null
      }
    });
  },

  closeAccumulationTrade(payload: {
    accumulationTradeId: string;
    reentryTransactionId: string;
    predictionNotes?: string;
    userId?: string;
  }): Promise<AccumulationTradeItem> {
    const query = payload.userId ? `?userId=${encodeURIComponent(payload.userId)}` : '';
    return request<AccumulationTradeItem>(`/api/accumulation-trades/close${query}`, {
      method: 'POST',
      body: {
        accumulationTradeId: payload.accumulationTradeId,
        reentryTransactionId: payload.reentryTransactionId,
        predictionNotes: payload.predictionNotes ?? null
      }
    });
  }
};
