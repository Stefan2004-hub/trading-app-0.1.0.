import { request } from './http';
import type { MarketAlertItem, MarketScanResult } from '../types/marketAlert';

export const marketAlertApi = {
  list(): Promise<MarketAlertItem[]> {
    return request<MarketAlertItem[]>('/api/market-alerts');
  },

  scan(): Promise<MarketScanResult> {
    return request<MarketScanResult>('/api/market-alerts/scan', {
      method: 'POST'
    });
  },

  markViewed(alertId: string): Promise<MarketAlertItem> {
    return request<MarketAlertItem>(`/api/market-alerts/${alertId}/view`, {
      method: 'POST'
    });
  },

  clearHistory(): Promise<void> {
    return request<void>('/api/market-alerts', {
      method: 'DELETE'
    });
  }
};
