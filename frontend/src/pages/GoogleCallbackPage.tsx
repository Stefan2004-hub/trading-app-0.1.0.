import { useEffect, useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { type AuthResponse } from '../types/auth';
import { completeOAuthLogin } from '../store/authSlice';
import { useAppDispatch } from '../store/hooks';

function readParam(params: URLSearchParams, key: string): string | null {
  const value = params.get(key);
  if (!value) {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

export function GoogleCallbackPage(): JSX.Element {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const params = useMemo(() => new URLSearchParams(location.search), [location.search]);

  useEffect(() => {
    const oauthError = readParam(params, 'oauthError');
    if (oauthError) {
      navigate(`/login?oauthError=${encodeURIComponent(oauthError)}`, { replace: true });
      return;
    }

    const authProvider = readParam(params, 'authProvider');
    const userId = readParam(params, 'userId');
    const email = readParam(params, 'email');
    const username = readParam(params, 'username');
    const accessToken = readParam(params, 'accessToken');
    const refreshToken = readParam(params, 'refreshToken');

    if (!authProvider || !userId || !email || !username || !accessToken || !refreshToken) {
      navigate('/login?oauthError=Google%20sign-in%20response%20was%20incomplete', { replace: true });
      return;
    }

    const payload: AuthResponse = {
      authProvider: authProvider === 'GOOGLE' ? 'GOOGLE' : 'LOCAL',
      userId,
      email,
      username,
      accessToken,
      refreshToken
    };

    dispatch(completeOAuthLogin(payload));
    navigate('/dashboard', { replace: true });
  }, [dispatch, navigate, params]);

  return (
    <main className="auth-shell">
      <section className="auth-panel">
        <h1>Completing sign in...</h1>
      </section>
    </main>
  );
}
