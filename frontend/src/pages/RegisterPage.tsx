import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthLayout } from '../components/AuthLayout';
import { clearAuthError, register } from '../store/authSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

export function RegisterPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { status, error } = useAppSelector((state) => state.auth);

  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  async function onSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    dispatch(clearAuthError());

    const action = await dispatch(
      register({
        email: email.trim(),
        username: username.trim(),
        password
      })
    );

    if (register.fulfilled.match(action)) {
      navigate('/login', { replace: true });
    }
  }

  return (
    <AuthLayout title="Create account" subtitle="Start tracking your trades in one place.">
      <form onSubmit={onSubmit} className="auth-form">
        <label htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
          autoComplete="email"
        />

        <label htmlFor="username">Username</label>
        <input
          id="username"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          required
          autoComplete="username"
          minLength={3}
          maxLength={50}
        />

        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
          autoComplete="new-password"
          minLength={8}
          maxLength={100}
        />

        {error ? <p className="auth-error">{error}</p> : null}

        <button type="submit" disabled={status === 'loading'}>
          {status === 'loading' ? 'Creating...' : 'Create account'}
        </button>
      </form>

      <p className="auth-note">
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </AuthLayout>
  );
}
