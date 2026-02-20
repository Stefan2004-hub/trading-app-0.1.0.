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
