import { FormEvent, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { AuthLayout } from '../components/AuthLayout';
import { clearAuthError, login } from '../store/authSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

interface LocationState {
  from?: {
    pathname?: string;
  };
}

export function LoginPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const { status, error } = useAppSelector((state) => state.auth);

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');

  const state = location.state as LocationState | null;
  const redirectPath = state?.from?.pathname ?? '/dashboard';

  if (status === 'authenticated') {
    return <Navigate to={redirectPath} replace />;
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    dispatch(clearAuthError());

    const action = await dispatch(login({ identifier: identifier.trim(), password }));
    if (login.fulfilled.match(action)) {
      navigate(redirectPath, { replace: true });
    }
  }

  return (
    <AuthLayout title="Sign in" subtitle="Use your email or username to continue.">
      <form onSubmit={onSubmit} className="auth-form">
        <label htmlFor="identifier">Email or username</label>
        <input
          id="identifier"
          value={identifier}
          onChange={(event) => setIdentifier(event.target.value)}
          required
          autoComplete="username"
        />

        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
          autoComplete="current-password"
        />

        {error ? <p className="auth-error">{error}</p> : null}

        <button type="submit" disabled={status === 'loading'}>
          {status === 'loading' ? 'Signing in...' : 'Sign in'}
        </button>
      </form>

      <button
        className="secondary"
        type="button"
        onClick={() => {
          authApi.startGoogleAuth();
        }}
      >
        Continue with Google
      </button>

      <p className="auth-note">
        New here? <Link to="/register">Create an account</Link>
      </p>
    </AuthLayout>
  );
}
