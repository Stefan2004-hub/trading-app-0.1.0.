import { request } from './http';
import { env } from '../config/env';
import type { AuthResponse, LoginPayload, RegisterPayload, UserProfile } from '../types/auth';

interface RegisterResponse extends UserProfile {}

interface GoogleStartResponse {
  authorizationUrl: string;
}

export const authApi = {
  login(payload: LoginPayload): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/login', { method: 'POST', body: payload, auth: false });
  },

  register(payload: RegisterPayload): Promise<RegisterResponse> {
    return request<RegisterResponse>('/api/auth/register', { method: 'POST', body: payload, auth: false });
  },

  me(): Promise<UserProfile> {
    return request<UserProfile>('/api/auth/me');
  },

  async resolveGoogleAuthorizationUrl(): Promise<string> {
    const response = await request<GoogleStartResponse>('/api/auth/oauth2/google', { auth: false });
    if (response.authorizationUrl.startsWith('http')) {
      return response.authorizationUrl;
    }
    return `${env.apiBaseUrl}${response.authorizationUrl}`;
  },

  startGoogleAuth(): void {
    window.location.assign(env.googleAuthStartUrl);
  }
};
