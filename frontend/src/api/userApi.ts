import { request } from './http';
import type { BuyInputMode } from '../types/trading';
import type { UserPreferences } from '../types/userPreferences';

export const userApi = {
  getPreferences(): Promise<UserPreferences> {
    return request<UserPreferences>('/api/user-preferences');
  },

  updateDefaultBuyInputMode(defaultBuyInputMode: BuyInputMode): Promise<UserPreferences> {
    return request<UserPreferences>('/api/user-preferences', {
      method: 'PUT',
      body: { defaultBuyInputMode }
    });
  }
};
