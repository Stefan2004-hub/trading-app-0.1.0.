import { Navigate, createBrowserRouter } from 'react-router-dom';
import { RequireAuth } from '../components/RequireAuth';
import { DashboardPage } from '../pages/DashboardPage';
import { GoogleEntryPage } from '../pages/GoogleEntryPage';
import { LoginPage } from '../pages/LoginPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { RegisterPage } from '../pages/RegisterPage';

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
    path: '/dashboard',
    element: (
      <RequireAuth>
        <DashboardPage />
      </RequireAuth>
    )
  },
  {
    path: '*',
    element: <NotFoundPage />
  }
]);
