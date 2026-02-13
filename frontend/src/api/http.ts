import { sessionStorageUtil } from '../utils/storage';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const AUTH_EXPIRED_EVENT = 'trading:auth-expired';

interface RequestOptions {
  method?: string;
  body?: unknown;
  auth?: boolean;
  skipRefreshRetry?: boolean;
}

interface ApiErrorPayload {
  message?: string;
  code?: string;
}

interface RefreshResponse {
  accessToken: string;
  refreshToken: string;
}

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

let refreshInFlight: Promise<string | null> | null = null;

function notifyAuthExpired(): void {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
  }
}

async function parseError(response: Response): Promise<string> {
  let message = `Request failed with status ${response.status}`;
  try {
    const errorPayload = (await response.json()) as ApiErrorPayload;
    message = errorPayload.message ?? errorPayload.code ?? message;
  } catch {
    // Ignore parse failures and keep default message.
  }
  return message;
}

async function refreshAccessToken(): Promise<string | null> {
  if (refreshInFlight) {
    return refreshInFlight;
  }

  refreshInFlight = (async () => {
    const session = sessionStorageUtil.read();
    if (!session) {
      return null;
    }

    const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${session.accessToken}`
      },
      body: JSON.stringify({ refreshToken: session.refreshToken })
    });

    if (!response.ok) {
      sessionStorageUtil.clear();
      notifyAuthExpired();
      return null;
    }

    const refresh = (await response.json()) as RefreshResponse;
    sessionStorageUtil.write({
      accessToken: refresh.accessToken,
      refreshToken: refresh.refreshToken,
      user: session.user
    });

    return refresh.accessToken;
  })();

  try {
    return await refreshInFlight;
  } finally {
    refreshInFlight = null;
  }
}

function buildHeaders(auth: boolean, overrideToken?: string): HeadersInit {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  };

  if (!auth) {
    return headers;
  }

  const session = sessionStorageUtil.read();
  const token = overrideToken ?? session?.accessToken;
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = true, skipRefreshRetry = false } = options;

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: buildHeaders(auth),
    body: body === undefined ? undefined : JSON.stringify(body)
  });

  if (response.status === 401 && auth && !skipRefreshRetry) {
    const refreshedAccessToken = await refreshAccessToken();
    if (refreshedAccessToken) {
      return request<T>(path, {
        method,
        body,
        auth,
        skipRefreshRetry: true
      });
    }
  }

  if (!response.ok) {
    throw new ApiError(response.status, await parseError(response));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export { AUTH_EXPIRED_EVENT };
