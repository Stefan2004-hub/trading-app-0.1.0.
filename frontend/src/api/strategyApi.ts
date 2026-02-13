import { request } from './http';
import type {
  BuyStrategyItem,
  SellStrategyItem,
  StrategyAlertItem,
  UpsertBuyStrategyPayload,
  UpsertSellStrategyPayload
} from '../types/strategy';
import type { AssetOption } from '../types/trading';

interface AuthorizedRequestOptions {
  accessToken: string;
}

export const strategyApi = {
  listAssets(): Promise<AssetOption[]> {
    return request<AssetOption[]>('/api/assets');
  },

  listSellStrategies({ accessToken }: AuthorizedRequestOptions): Promise<SellStrategyItem[]> {
    return request<SellStrategyItem[]>('/api/strategies/sell', { accessToken });
  },

  upsertSellStrategy(
    payload: UpsertSellStrategyPayload,
    { accessToken }: AuthorizedRequestOptions
  ): Promise<SellStrategyItem> {
    return request<SellStrategyItem>('/api/strategies/sell', {
      method: 'POST',
      body: payload,
      accessToken
    });
  },

  listBuyStrategies({ accessToken }: AuthorizedRequestOptions): Promise<BuyStrategyItem[]> {
    return request<BuyStrategyItem[]>('/api/strategies/buy', { accessToken });
  },

  upsertBuyStrategy(
    payload: UpsertBuyStrategyPayload,
    { accessToken }: AuthorizedRequestOptions
  ): Promise<BuyStrategyItem> {
    return request<BuyStrategyItem>('/api/strategies/buy', {
      method: 'POST',
      body: payload,
      accessToken
    });
  },

  listAlerts({ accessToken }: AuthorizedRequestOptions): Promise<StrategyAlertItem[]> {
    return request<StrategyAlertItem[]>('/api/strategies/alerts', { accessToken });
  },

  acknowledgeAlert(alertId: string, { accessToken }: AuthorizedRequestOptions): Promise<StrategyAlertItem> {
    return request<StrategyAlertItem>(`/api/strategies/alerts/${alertId}/acknowledge`, {
      method: 'POST',
      accessToken
    });
  }
};
