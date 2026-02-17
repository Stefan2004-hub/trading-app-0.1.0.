interface FrontendEnv {
  apiBaseUrl: string;
  googleAuthStartUrl: string;
}

const DEFAULT_API_BASE_URL = 'http://localhost:8080';

function readOptionalEnvVar(name: 'VITE_API_BASE_URL' | 'VITE_GOOGLE_AUTH_START_URL'): string | undefined {
  const value = import.meta.env[name];
  const trimmed = value?.trim();
  return trimmed && trimmed.length > 0 ? trimmed : undefined;
}

function trimTrailingSlashes(url: string): string {
  return url.replace(/\/+$/, '');
}

function readApiBaseUrl(): string {
  const apiBaseUrl = readOptionalEnvVar('VITE_API_BASE_URL') ?? DEFAULT_API_BASE_URL;
  return trimTrailingSlashes(apiBaseUrl);
}

const apiBaseUrl = readApiBaseUrl();
const googleAuthStartUrl =
  readOptionalEnvVar('VITE_GOOGLE_AUTH_START_URL') ?? `${apiBaseUrl}/api/auth/oauth2/google`;

export const env: FrontendEnv = {
  apiBaseUrl,
  googleAuthStartUrl
};
