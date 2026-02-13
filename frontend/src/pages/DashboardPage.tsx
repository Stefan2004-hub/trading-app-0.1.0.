import { logout } from '../store/authSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

export function DashboardPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const user = useAppSelector((state) => state.auth.user);

  return (
    <main className="dashboard-shell">
      <section className="dashboard-panel">
        <h1>Dashboard</h1>
        <p>
          Signed in as <strong>{user?.username}</strong> ({user?.email})
        </p>
        <button
          className="secondary"
          type="button"
          onClick={() => {
            dispatch(logout());
          }}
        >
          Sign out
        </button>
      </section>
    </main>
  );
}
