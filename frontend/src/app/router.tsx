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
import { StrategiesPage } from '../pages/StrategiesPage';
import { TransactionsPage } from '../pages/TransactionsPage';

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
    path: '/strategies',
    element: (
      <RequireAuth>
        <StrategiesPage />
      </RequireAuth>
    )
  },
  {
    path: '*',
    element: <NotFoundPage />
  }
]);
