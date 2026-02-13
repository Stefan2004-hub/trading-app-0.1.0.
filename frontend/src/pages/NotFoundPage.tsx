import { Link } from 'react-router-dom';

export function NotFoundPage(): JSX.Element {
  return (
    <main className="dashboard-shell">
      <section className="dashboard-panel">
        <h1>Page not found</h1>
        <p>
          Return to <Link to="/dashboard">dashboard</Link>.
        </p>
      </section>
    </main>
  );
}
