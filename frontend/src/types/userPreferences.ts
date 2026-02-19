import type { BuyInputMode } from './trading';

export interface UserPreferences {
  userId: string;
  defaultBuyInputMode: BuyInputMode;
}
