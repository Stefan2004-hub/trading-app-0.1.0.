import { request } from './http';
import type {
  BuyStrategyItem,
  SellStrategyItem,
  StrategyAlertItem,
  UpsertBuyStrategyPayload,
  UpsertSellStrategyPayload
} from '../types/strategy';
import type { AssetOption } from '../types/trading';

export const strategyApi = {
  listAssets(): Promise<AssetOption[]> {
    return request<AssetOption[]>('/api/assets', { auth: false });
  },

  listSellStrategies(): Promise<SellStrategyItem[]> {
    return request<SellStrategyItem[]>('/api/strategies/sell');
  },

  upsertSellStrategy(payload: UpsertSellStrategyPayload): Promise<SellStrategyItem> {
    return request<SellStrategyItem>('/api/strategies/sell', {
      method: 'POST',
      body: payload
    });
  },

  listBuyStrategies(): Promise<BuyStrategyItem[]> {
    return request<BuyStrategyItem[]>('/api/strategies/buy');
  },

  upsertBuyStrategy(payload: UpsertBuyStrategyPayload): Promise<BuyStrategyItem> {
    return request<BuyStrategyItem>('/api/strategies/buy', {
      method: 'POST',
      body: payload
    });
  },

  listAlerts(): Promise<StrategyAlertItem[]> {
    return request<StrategyAlertItem[]>('/api/strategies/alerts');
  },

  acknowledgeAlert(alertId: string): Promise<StrategyAlertItem> {
    return request<StrategyAlertItem>(`/api/strategies/alerts/${alertId}/acknowledge`, {
      method: 'POST'
    });
  }
};
