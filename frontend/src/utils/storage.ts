import type { UserProfile } from '../types/auth';

const ACCESS_TOKEN_KEY = 'trading.accessToken';
const REFRESH_TOKEN_KEY = 'trading.refreshToken';
const USER_KEY = 'trading.user';

export interface StoredSession {
  accessToken: string;
  refreshToken: string;
  user: UserProfile;
}

export const sessionStorageUtil = {
  read(): StoredSession | null {
    const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    const rawUser = localStorage.getItem(USER_KEY);

    if (!accessToken || !refreshToken || !rawUser) {
      return null;
    }

    try {
      const user = JSON.parse(rawUser) as UserProfile;
      return { accessToken, refreshToken, user };
    } catch {
      this.clear();
      return null;
    }
  },

  write(session: StoredSession): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, session.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(session.user));
  },

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
};
