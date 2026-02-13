export type AuthProvider = 'LOCAL' | 'GOOGLE';

export interface UserProfile {
  userId: string;
  email: string;
  username: string;
  authProvider: AuthProvider;
}

export interface AuthResponse extends UserProfile {
  accessToken: string;
  refreshToken: string;
}

export interface LoginPayload {
  identifier: string;
  password: string;
}

export interface RegisterPayload {
  email: string;
  username: string;
  password: string;
}
