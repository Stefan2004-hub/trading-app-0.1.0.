export type StrategyType = 'BUY' | 'SELL';
export type StrategyAlertStatus = 'PENDING' | 'ACKNOWLEDGED' | 'EXECUTED' | 'DISMISSED';

export interface SellStrategyItem {
  id: string;
  userId: string;
  assetId: string;
  thresholdPercent: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BuyStrategyItem {
  id: string;
  userId: string;
  assetId: string;
  dipThresholdPercent: string;
  buyAmountUsd: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StrategyAlertItem {
  id: string;
  userId: string;
  assetId: string;
  strategyType: StrategyType;
  triggerPrice: string;
  thresholdPercent: string;
  referencePrice: string;
  alertMessage: string;
  status: StrategyAlertStatus;
  createdAt: string;
  acknowledgedAt: string | null;
  executedAt: string | null;
}

export interface UpsertSellStrategyPayload {
  assetId: string;
  thresholdPercent: string;
  active: boolean;
}

export interface UpsertBuyStrategyPayload {
  assetId: string;
  dipThresholdPercent: string;
  buyAmountUsd: string;
  active: boolean;
}
