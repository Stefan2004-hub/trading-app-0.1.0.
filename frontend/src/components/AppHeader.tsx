import { Link, useLocation, useNavigate } from 'react-router-dom';
import { logout } from '../store/authSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

export function AppHeader(): JSX.Element {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAppSelector((state) => state.auth.user);

  return (
    <header className="app-header">
      <div className="brand">Trading App</div>
      <nav>
        <Link className={location.pathname === '/dashboard' ? 'active' : ''} to="/dashboard">
          Dashboard
        </Link>
        <Link className={location.pathname === '/transactions' ? 'active' : ''} to="/transactions">
          Transactions
        </Link>
        <Link className={location.pathname === '/strategies' ? 'active' : ''} to="/strategies">
          Strategies
        </Link>
      </nav>
      <div className="header-right">
        <span>{user?.username}</span>
        <button
          className="secondary"
          type="button"
          onClick={() => {
            dispatch(logout());
            navigate('/login', { replace: true });
          }}
        >
          Sign out
        </button>
      </div>
    </header>
  );
}
