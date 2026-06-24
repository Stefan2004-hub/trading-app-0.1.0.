import { request, requestBlob } from './http';
import { decimalToFractionalPercent } from '../utils/decimal';
import type {
  AccumulationTradeItem,
  AccumulationTradeAssetSummaryItem,
  AssetSummary,
  AccumulationTradeStatus,
  AssetOption,
  ExchangeOption,
  HistoricalDataRow,
  HistoricalMissingAsset,
  HistoricalSyncStartResult,
  HistoricalDataSyncResult,
  PricePeakItem,
  PaginatedResponse,
  PortfolioAssetPerformance,
  PortfolioSummary,
  SortDirection,
  TradeFormPayload,
  TransactionItem,
  TransactionSortBy,
  TransactionView,
  UpdateTransactionNetAmountPayload,
  UpdateTransactionPayload,
  UpdatePricePeakPayload,
  SpotPriceQuote
} from '../types/trading';

function extractLookupContent<T>(response: T[] | PaginatedResponse<T>): T[] {
  if (Array.isArray(response)) {
    return response;
  }
  return Array.isArray(response.content) ? response.content : [];
}

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
    const normalizedSearch = search?.trim() ?? '';
    return request<AssetOption[] | PaginatedResponse<AssetOption>>(
      `/api/assets?search=${encodeURIComponent(normalizedSearch)}`
    ).then(extractLookupContent);
  },

  createAsset(payload: { symbol: string; name: string; coinGeckoId?: string | null; gateIoSymbol?: string | null }): Promise<AssetOption> {
    return request<AssetOption>('/api/assets', {
      method: 'POST',
      body: payload
    });
  },

  updateAsset(id: string, payload: { symbol: string; name: string; coinGeckoId?: string | null; gateIoSymbol?: string | null }): Promise<AssetOption> {
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
    const normalizedSearch = search?.trim() ?? '';
    return request<ExchangeOption[] | PaginatedResponse<ExchangeOption>>(
      `/api/exchanges?search=${encodeURIComponent(normalizedSearch)}`
    ).then(extractLookupContent);
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

  listPricePeaks(search?: string): Promise<PricePeakItem[]> {
    const normalizedSearch = search?.trim() ?? '';
    return request<PricePeakItem[]>(`/api/price-peaks?search=${encodeURIComponent(normalizedSearch)}`);
  },

  updatePricePeak(id: string, payload: UpdatePricePeakPayload): Promise<PricePeakItem> {
    return request<PricePeakItem>(`/api/price-peaks/${id}`, {
      method: 'PUT',
      body: payload
    });
  },

  deletePricePeak(id: string): Promise<void> {
    return request<void>(`/api/price-peaks/${id}`, {
      method: 'DELETE'
    });
  },

  listTransactions(params?: {
    page?: number;
    size?: number;
    search?: string;
    view?: TransactionView;
    groupSize?: number;
    sortBy?: TransactionSortBy;
    sortDirection?: SortDirection;
    dateFromInclusive?: string;
    dateToExclusive?: string;
  }): Promise<PaginatedResponse<TransactionItem>> {
    const queryParams = new URLSearchParams();
    queryParams.set('page', String(params?.page ?? 0));
    queryParams.set('size', String(params?.size ?? 20));
    queryParams.set('view', params?.view ?? 'OPEN');

    const trimmedSearch = params?.search?.trim();
    if (trimmedSearch) {
      queryParams.set('search', trimmedSearch);
    }
    if (params?.groupSize && params.groupSize > 0) {
      queryParams.set('groupSize', String(params.groupSize));
    }
    if (params?.sortBy) {
      queryParams.set('sortBy', params.sortBy);
    }
    if (params?.sortDirection) {
      queryParams.set('sortDirection', params.sortDirection);
    }
    if (params?.dateFromInclusive) {
      queryParams.set('dateFromInclusive', params.dateFromInclusive);
    }
    if (params?.dateToExclusive) {
      queryParams.set('dateToExclusive', params.dateToExclusive);
    }

    return request<PaginatedResponse<TransactionItem>>(`/api/transactions?${queryParams.toString()}`);
  },

  getPortfolioSummary(): Promise<PortfolioSummary> {
    return request<PortfolioSummary>('/api/portfolio/summary');
  },

  getPortfolioPerformance(): Promise<PortfolioAssetPerformance[]> {
    return request<PortfolioAssetPerformance[]>('/api/portfolio/performance');
  },

  getAssetSummary(): Promise<AssetSummary[]> {
    return request<AssetSummary[]>('/api/portfolio/asset-summary');
  },

  getSpotPrice(symbol: string): Promise<SpotPriceQuote> {
    const normalized = symbol.trim().toUpperCase();
    return request<SpotPriceQuote>(`/api/prices/spot?symbol=${encodeURIComponent(normalized)}`);
  },

  listHistoricalData(params?: { page?: number; size?: number }): Promise<PaginatedResponse<HistoricalDataRow>> {
    const queryParams = new URLSearchParams();
    queryParams.set('page', String(params?.page ?? 0));
    queryParams.set('size', String(params?.size ?? 20));
    return request<PaginatedResponse<HistoricalDataRow>>(`/api/historical-data?${queryParams.toString()}`);
  },

  listHistoricalAssetsMissingToday(): Promise<HistoricalMissingAsset[]> {
    return request<HistoricalMissingAsset[]>('/api/historical-data/missing-today');
  },

  startHistoricalDataRefreshAll(): Promise<HistoricalSyncStartResult> {
    return request<HistoricalSyncStartResult>('/api/historical-data/refresh', {
      method: 'POST'
    });
  },

  refreshHistoricalData(assetId: string): Promise<HistoricalDataSyncResult> {
    const queryParams = new URLSearchParams();
    queryParams.set('assetId', assetId);
    const query = queryParams.toString();
    return request<HistoricalDataSyncResult>(`/api/historical-data/refresh${query ? `?${query}` : ''}`, {
      method: 'POST'
    });
  },

  cleanAndResetHistoricalData(): Promise<HistoricalDataSyncResult> {
    return request<HistoricalDataSyncResult>('/api/historical-data/clean-reset', {
      method: 'POST'
    });
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

  async cleanHistory(): Promise<{ blob: Blob; fileName: string }> {
    const response = await requestBlob('/api/transactions/clean-history', {
      method: 'POST'
    });
    const contentDisposition = response.headers.get('content-disposition') ?? '';
    const fileNameMatch = contentDisposition.match(/filename="([^"]+)"/i);
    const fileName = fileNameMatch?.[1] ?? 'trading-history-backup.xlsx';
    return { blob: response.blob, fileName };
  },

  async downloadSqlBackup(): Promise<{ blob: Blob; fileName: string }> {
    const response = await requestBlob('/api/system/sql-backup', {
      method: 'GET'
    });
    const contentDisposition = response.headers.get('content-disposition') ?? '';
    const fileNameMatch = contentDisposition.match(/filename=\"([^\"]+)\"/i);
    const fileName = fileNameMatch?.[1] ?? 'trading-sql-backup.sql';
    return { blob: response.blob, fileName };
  },

  getKeepAliveStatus(): Promise<{ keepAliveActive: boolean }> {
    return request<{ keepAliveActive: boolean }>('/api/admin/ping-status', {
      method: 'GET'
    });
  },

  toggleKeepAlive(): Promise<{ keepAliveActive: boolean }> {
    return request<{ keepAliveActive: boolean }>('/api/admin/toggle-ping', {
      method: 'POST'
    });
  },

  listAccumulationTrades(params?: {
    page?: number;
    size?: number;
    assetId?: string;
    status?: AccumulationTradeStatus;
    userId?: string;
  }): Promise<PaginatedResponse<AccumulationTradeItem>> {
    const queryParams = new URLSearchParams();
    queryParams.set('page', String(params?.page ?? 0));
    queryParams.set('size', String(params?.size ?? 20));
    if (params?.assetId) {
      queryParams.set('assetId', params.assetId);
    }
    if (params?.status) {
      queryParams.set('status', params.status);
    }
    if (params?.userId) {
      queryParams.set('userId', params.userId);
    }
    return request<PaginatedResponse<AccumulationTradeItem>>(`/api/accumulation-trades?${queryParams.toString()}`);
  },

  listAccumulationTradeAssetSummaries(params?: {
    assetId?: string;
    status?: AccumulationTradeStatus;
    userId?: string;
  }): Promise<AccumulationTradeAssetSummaryItem[]> {
    const queryParams = new URLSearchParams();
    if (params?.assetId) {
      queryParams.set('assetId', params.assetId);
    }
    if (params?.status) {
      queryParams.set('status', params.status);
    }
    if (params?.userId) {
      queryParams.set('userId', params.userId);
    }
    const query = queryParams.toString();
    return request<AccumulationTradeAssetSummaryItem[]>(
      `/api/accumulation-trades/grouped-by-asset${query ? `?${query}` : ''}`
    );
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
