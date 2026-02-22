import { Navigate, createBrowserRouter } from 'react-router-dom';
import { RequireAuth } from '../components/RequireAuth';
import { DashboardPage } from '../pages/DashboardPage';
import { ExchangesPage } from '../pages/ExchangesPage';
import { GoogleCallbackPage } from '../pages/GoogleCallbackPage';
import { GoogleEntryPage } from '../pages/GoogleEntryPage';
import { LoginPage } from '../pages/LoginPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { RegisterPage } from '../pages/RegisterPage';
import { AssetsPage } from '../pages/AssetsPage';
import { AlertsPage } from '../pages/AlertsPage';
import { MarketAlertsPage } from '../pages/MarketAlertsPage';
import { HistoricalDataPage } from '../pages/HistoricalDataPage';
import { PricePeaksPage } from '../pages/PricePeaksPage';
import { StrategiesPage } from '../pages/StrategiesPage';
import { TransactionsPage } from '../pages/TransactionsPage';
import { MaintenancePage } from '../pages/MaintenancePage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/dashboard" replace />
  },
  {
    path: '/login',
    element: <LoginPage />
  },
  {
    path: '/register',
    element: <RegisterPage />
  },
  {
    path: '/auth/google',
    element: <GoogleEntryPage />
  },
  {
    path: '/auth/google/callback',
    element: <GoogleCallbackPage />
  },
  {
    path: '/dashboard',
    element: (
      <RequireAuth>
        <DashboardPage />
      </RequireAuth>
    )
  },
  {
    path: '/assets',
    element: (
      <RequireAuth>
        <AssetsPage />
      </RequireAuth>
    )
  },
  {
    path: '/exchanges',
    element: (
      <RequireAuth>
        <ExchangesPage />
      </RequireAuth>
    )
  },
  {
    path: '/transactions',
    element: (
      <RequireAuth>
        <TransactionsPage />
      </RequireAuth>
    )
  },
  {
    path: '/alerts',
    element: (
      <RequireAuth>
        <AlertsPage />
      </RequireAuth>
    )
  },
  {
    path: '/market-alerts',
    element: (
      <RequireAuth>
        <MarketAlertsPage />
      </RequireAuth>
    )
  },
  {
    path: '/historical-data',
    element: (
      <RequireAuth>
        <HistoricalDataPage />
      </RequireAuth>
    )
  },
  {
    path: '/price-peaks',
    element: (
      <RequireAuth>
        <PricePeaksPage />
      </RequireAuth>
    )
  },
  {
    path: '/strategies',
    element: (
      <RequireAuth>
        <StrategiesPage />
      </RequireAuth>
    )
  },
  {
    path: '/maintenance',
    element: (
      <RequireAuth>
        <MaintenancePage />
      </RequireAuth>
    )
  },
  {
    path: '*',
    element: <NotFoundPage />
  }
]);
