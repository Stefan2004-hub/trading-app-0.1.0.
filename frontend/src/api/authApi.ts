import { request } from './http';
import type { AuthResponse, LoginPayload, RegisterPayload, UserProfile } from '../types/auth';

interface RegisterResponse extends UserProfile {}

interface GoogleStartResponse {
  authorizationUrl: string;
}

const DEFAULT_API_BASE_URL = 'http://localhost:8080';
const DEFAULT_GOOGLE_AUTH_START_URL = `${DEFAULT_API_BASE_URL}/api/auth/oauth2/google`;

function envOrDefault(value: string | undefined, fallback: string): string {
  const trimmed = value?.trim();
  return trimmed && trimmed.length > 0 ? trimmed : fallback;
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
    return `${envOrDefault(import.meta.env.VITE_API_BASE_URL, DEFAULT_API_BASE_URL)}${response.authorizationUrl}`;
  },

  startGoogleAuth(): void {
    window.location.assign(envOrDefault(import.meta.env.VITE_GOOGLE_AUTH_START_URL, DEFAULT_GOOGLE_AUTH_START_URL));
  }
};
