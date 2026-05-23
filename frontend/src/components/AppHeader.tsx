import { type ReactNode, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { logout } from '../store/authSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

type IconComponent = (props: { className?: string }) => JSX.Element;

interface NavItem {
  label: string;
  to: string;
  icon: IconComponent;
}

interface NavGroupConfig {
  id: string;
  label: string;
  icon: IconComponent;
  items: NavItem[];
}

interface NavGroupProps {
  group: NavGroupConfig;
  isOpen: boolean;
  currentPath: string;
  onToggle: (groupId: string) => void;
  children?: ReactNode;
}

function IconBase({ className, children }: { className?: string; children: ReactNode }): JSX.Element {
  return (
    <svg aria-hidden="true" className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      {children}
    </svg>
  );
}

function MenuIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <path d="M4 7h16" />
      <path d="M4 12h16" />
      <path d="M4 17h16" />
    </IconBase>
  );
}

function CloseIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <path d="M6 6l12 12" />
      <path d="M18 6L6 18" />
    </IconBase>
  );
}

function ChevronIcon({ className, open }: { className?: string; open: boolean }): JSX.Element {
  return (
    <IconBase className={className}>
      {open ? <path d="M6 9l6 6 6-6" /> : <path d="M9 6l6 6-6 6" />}
    </IconBase>
  );
}

function BriefcaseIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <rect x="3" y="7" width="18" height="13" rx="2" />
      <path d="M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
      <path d="M3 12h18" />
    </IconBase>
  );
}

function ChartIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <path d="M4 19h16" />
      <path d="M6 15l4-4 3 3 5-6" />
      <circle cx="6" cy="15" r="1" />
      <circle cx="10" cy="11" r="1" />
      <circle cx="13" cy="14" r="1" />
      <circle cx="18" cy="8" r="1" />
    </IconBase>
  );
}

function DatabaseIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <ellipse cx="12" cy="6" rx="7" ry="3" />
      <path d="M5 6v6c0 1.7 3.1 3 7 3s7-1.3 7-3V6" />
      <path d="M5 12v6c0 1.7 3.1 3 7 3s7-1.3 7-3v-6" />
    </IconBase>
  );
}

function SettingsIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1 1 0 0 0 .2 1.1l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1 1 0 0 0-1.1-.2 1 1 0 0 0-.6.9V20a2 2 0 1 1-4 0v-.2a1 1 0 0 0-.6-.9 1 1 0 0 0-1.1.2l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1 1 0 0 0 .2-1.1 1 1 0 0 0-.9-.6H4a2 2 0 1 1 0-4h.2a1 1 0 0 0 .9-.6 1 1 0 0 0-.2-1.1l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1 1 0 0 0 1.1.2H9a1 1 0 0 0 .6-.9V4a2 2 0 1 1 4 0v.2a1 1 0 0 0 .6.9h.2a1 1 0 0 0 1.1-.2l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1 1 0 0 0-.2 1.1v.2a1 1 0 0 0 .9.6H20a2 2 0 1 1 0 4h-.2a1 1 0 0 0-.9.6z" />
    </IconBase>
  );
}

function DashboardIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <rect x="3" y="3" width="8" height="8" rx="1" />
      <rect x="13" y="3" width="8" height="5" rx="1" />
      <rect x="13" y="10" width="8" height="11" rx="1" />
      <rect x="3" y="13" width="8" height="8" rx="1" />
    </IconBase>
  );
}

function TransactionsIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <path d="M6 7h11" />
      <path d="M6 12h12" />
      <path d="M6 17h10" />
      <path d="M4 7l1-1 1 1" />
      <path d="M20 12l-1 1-1-1" />
    </IconBase>
  );
}

function StrategyIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <path d="M4 6h16" />
      <path d="M8 6v12" />
      <path d="M16 6v12" />
      <path d="M4 18h16" />
      <circle cx="8" cy="11" r="1" />
      <circle cx="16" cy="14" r="1" />
    </IconBase>
  );
}

function BellIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <path d="M6 16h12" />
      <path d="M8 16V10a4 4 0 1 1 8 0v6" />
      <path d="M10 19a2 2 0 0 0 4 0" />
    </IconBase>
  );
}

function HistoryIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <path d="M3 12a9 9 0 1 0 3-6.7" />
      <path d="M3 4v4h4" />
      <path d="M12 8v5l3 2" />
    </IconBase>
  );
}

function PeakIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <path d="M3 18l6-8 4 5 4-7 4 10" />
      <path d="M3 20h18" />
    </IconBase>
  );
}

function AssetIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <circle cx="12" cy="12" r="8" />
      <path d="M12 8v8" />
      <path d="M9 10h6" />
      <path d="M9 14h6" />
    </IconBase>
  );
}

function ExchangeIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <rect x="4" y="5" width="16" height="14" rx="2" />
      <path d="M8 9h8" />
      <path d="M8 13h6" />
    </IconBase>
  );
}

function AlertsIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <path d="M12 4l8 14H4l8-14z" />
      <path d="M12 10v4" />
      <circle cx="12" cy="16" r="1" fill="currentColor" stroke="none" />
    </IconBase>
  );
}

function MaintenanceIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <path d="M4 20l5-5" />
      <path d="M15 4l5 5" />
      <path d="M14 5l5 5" />
      <path d="M10 9l5 5" />
    </IconBase>
  );
}

function UserIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <circle cx="12" cy="8" r="3" />
      <path d="M5 20a7 7 0 0 1 14 0" />
    </IconBase>
  );
}

function LogoutIcon({ className }: { className?: string }): JSX.Element {
  return (
    <IconBase className={className}>
      <path d="M10 17l5-5-5-5" />
      <path d="M15 12H7" />
      <path d="M4 4h6" />
      <path d="M4 20h6" />
      <path d="M4 4v16" />
    </IconBase>
  );
}

const primaryGroups: NavGroupConfig[] = [
  {
    id: 'portfolio',
    label: 'Portfolio',
    icon: BriefcaseIcon,
    items: [
      { label: 'Dashboard', to: '/dashboard', icon: DashboardIcon },
      { label: 'Transactions', to: '/transactions', icon: TransactionsIcon },
      { label: 'Accumulation Strategy', to: '/accumulation-strategy', icon: StrategyIcon },
      { label: 'Strategies', to: '/strategies', icon: StrategyIcon }
    ]
  },
  {
    id: 'market-analysis',
    label: 'Market Analysis',
    icon: ChartIcon,
    items: [
      { label: 'Market Alerts', to: '/market-alerts', icon: BellIcon },
      { label: 'Historical Data', to: '/historical-data', icon: HistoryIcon },
      { label: 'Price Peaks', to: '/price-peaks', icon: PeakIcon }
    ]
  },
  {
    id: 'data-management',
    label: 'Data Management',
    icon: DatabaseIcon,
    items: [
      { label: 'Assets', to: '/assets', icon: AssetIcon },
      { label: 'Exchanges', to: '/exchanges', icon: ExchangeIcon },
      { label: 'Alerts', to: '/alerts', icon: AlertsIcon }
    ]
  }
];

const accountGroup: NavGroupConfig = {
  id: 'account-system',
  label: 'Account & System',
  icon: SettingsIcon,
  items: [{ label: 'Maintenance', to: '/maintenance', icon: MaintenanceIcon }]
};

function createInitialOpenState(pathname: string): Record<string, boolean> {
  const state: Record<string, boolean> = {
    portfolio: true,
    'market-analysis': false,
    'data-management': false,
    'account-system': false
  };

  const activeGroup = [...primaryGroups, accountGroup].find((group) =>
    group.items.some((item) => pathname === item.to || pathname.startsWith(`${item.to}/`))
  );

  if (activeGroup) {
    state[activeGroup.id] = true;
  }

  return state;
}

function NavGroup({ group, isOpen, currentPath, onToggle, children }: NavGroupProps): JSX.Element {
  const GroupIcon = group.icon;
  return (
    <section className="nav-group">
      <button
        type="button"
        className="nav-group-trigger"
        onClick={() => onToggle(group.id)}
        aria-expanded={isOpen}
        aria-controls={`nav-group-${group.id}`}
      >
        <span className="nav-group-trigger-title">
          <GroupIcon className="nav-group-icon" />
          {group.label}
        </span>
        <ChevronIcon className="nav-group-chevron" open={isOpen} />
      </button>
      <div id={`nav-group-${group.id}`} className={`nav-group-items ${isOpen ? 'open' : ''}`}>
        {group.items.map((item) => {
          const ItemIcon = item.icon;
          const active = currentPath === item.to || currentPath.startsWith(`${item.to}/`);
          return (
            <Link key={item.to} className={`nav-item nav-item-indent ${active ? 'active' : ''}`} to={item.to}>
              <ItemIcon className="nav-item-icon" />
              <span>{item.label}</span>
            </Link>
          );
        })}
        {children}
      </div>
    </section>
  );
}

export function AppHeader(): JSX.Element {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAppSelector((state) => state.auth.user);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>(() => createInitialOpenState(location.pathname));

  const profileName = useMemo(() => user?.username?.trim() || 'User', [user?.username]);

  useEffect(() => {
    setMobileMenuOpen(false);
    const activeGroup = [...primaryGroups, accountGroup].find((group) =>
      group.items.some((item) => location.pathname === item.to || location.pathname.startsWith(`${item.to}/`))
    );
    if (!activeGroup) {
      return;
    }
    setOpenGroups((current) => {
      if (current[activeGroup.id]) {
        return current;
      }
      return { ...current, [activeGroup.id]: true };
    });
  }, [location.pathname]);

  function toggleGroup(groupId: string): void {
    setOpenGroups((current) => ({ ...current, [groupId]: !current[groupId] }));
  }

  function handleSignOut(): void {
    dispatch(logout());
    navigate('/login', { replace: true });
  }

  return (
    <>
      <div className="sidebar-mobile-bar">
        <div className="sidebar-brand">Trading App</div>
        <button
          type="button"
          className="sidebar-menu-button"
          aria-label={mobileMenuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={mobileMenuOpen}
          onClick={() => setMobileMenuOpen((open) => !open)}
        >
          {mobileMenuOpen ? <CloseIcon className="sidebar-menu-icon" /> : <MenuIcon className="sidebar-menu-icon" />}
        </button>
      </div>
      <button
        type="button"
        className={`sidebar-overlay ${mobileMenuOpen ? 'open' : ''}`}
        aria-label="Close navigation"
        onClick={() => setMobileMenuOpen(false)}
      />
      <aside className={`app-header app-sidebar ${mobileMenuOpen ? 'open' : ''}`} aria-label="Primary navigation">
        <div className="sidebar-brand">Trading App</div>
        <nav className="sidebar-nav">
          <div className="sidebar-nav-main">
            {primaryGroups.map((group) => (
              <NavGroup
                key={group.id}
                group={group}
                isOpen={openGroups[group.id]}
                currentPath={location.pathname}
                onToggle={toggleGroup}
              />
            ))}
          </div>
          <div className="sidebar-nav-bottom">
            <NavGroup
              group={accountGroup}
              isOpen={openGroups[accountGroup.id]}
              currentPath={location.pathname}
              onToggle={toggleGroup}
            >
              <div className="nav-item nav-item-indent nav-item-static" aria-label={`Signed in as ${profileName}`}>
                <UserIcon className="nav-item-icon" />
                <span>{profileName}</span>
              </div>
              <button type="button" className="nav-item nav-item-indent nav-item-signout" onClick={handleSignOut}>
                <LogoutIcon className="nav-item-icon" />
                <span>Sign Out</span>
              </button>
            </NavGroup>
          </div>
        </nav>
      </aside>
    </>
  );
}
