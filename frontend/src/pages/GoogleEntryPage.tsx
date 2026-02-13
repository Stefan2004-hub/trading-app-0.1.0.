import { useEffect, useState } from 'react';
import { authApi } from '../api/authApi';

export function GoogleEntryPage(): JSX.Element {
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    authApi
      .resolveGoogleAuthorizationUrl()
      .then((url) => {
        window.location.assign(url);
      })
      .catch((e: unknown) => {
        if (!mounted) {
          return;
        }
        const message = e instanceof Error ? e.message : 'Failed to start Google auth';
        setError(message);
      });

    return () => {
      mounted = false;
    };
  }, []);

  return (
    <main className="auth-shell">
      <section className="auth-panel">
        <h1>Redirecting to Google...</h1>
        {error ? <p className="auth-error">{error}</p> : null}
      </section>
    </main>
  );
}
