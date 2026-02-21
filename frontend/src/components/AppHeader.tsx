import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { logout } from '../store/authSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

export function AppHeader(): JSX.Element {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAppSelector((state) => state.auth.user);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const menuItems = [
    { label: 'Dashboard', to: '/dashboard' },
    { label: 'Transactions', to: '/transactions' },
    { label: 'Assets', to: '/assets' },
    { label: 'Exchanges', to: '/exchanges' },
    { label: 'Strategies', to: '/strategies' },
  ];

  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  return (
    <header className="app-header">
      <div className="brand">Trading App</div>
      <nav className="desktop-nav">
        {menuItems.map((item) => (
          <Link className={location.pathname === item.to ? 'active' : ''} key={item.to} to={item.to}>
            {item.label}
          </Link>
        ))}
      </nav>
      <div className="header-right">
        <span>{user?.username}</span>
        <button
          aria-controls="mobile-nav"
          aria-expanded={mobileMenuOpen}
          aria-label={mobileMenuOpen ? 'Close menu' : 'Open menu'}
          className="menu-toggle"
          onClick={() => setMobileMenuOpen((isOpen) => !isOpen)}
          type="button"
        >
          {mobileMenuOpen ? (
            <svg aria-hidden="true" viewBox="0 0 24 24">
              <path d="M6 6L18 18M6 18L18 6" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
            </svg>
          ) : (
            <svg aria-hidden="true" viewBox="0 0 24 24">
              <path d="M4 7H20M4 12H20M4 17H20" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
            </svg>
          )}
        </button>
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
      <nav className={`mobile-nav ${mobileMenuOpen ? 'open' : ''}`} id="mobile-nav">
        {menuItems.map((item) => (
          <Link className={location.pathname === item.to ? 'active' : ''} key={`mobile-${item.to}`} to={item.to}>
            {item.label}
          </Link>
        ))}
      </nav>
    </header>
  );
}
