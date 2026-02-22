export type MarketAlertType = 'BUY' | 'SELL';
export type MarketAlertStrategyType = 'FIXED_14D' | 'DYNAMIC';

export interface MarketAlertItem {
  id: string;
  assetId: string;
  assetSymbol: string;
  assetName: string;
  alertType: MarketAlertType;
  strategyType: MarketAlertStrategyType;
  rsiValue: string;
  stochValue: string;
  intervalDays: number;
  triggerDate: string;
  isViewed: boolean;
  createdAt: string;
}

export interface MarketScanResult {
  assetsProcessed: number;
  alertsCreated: number;
  fixedAlertsCreated: number;
  dynamicAlertsCreated: number;
}
