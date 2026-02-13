import { useEffect } from 'react';
import { RouterProvider } from 'react-router-dom';
import { AUTH_EXPIRED_EVENT } from '../api/http';
import { fetchMe, logout } from '../store/authSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { router } from './router';

export function App(): JSX.Element {
  const dispatch = useAppDispatch();
  const { accessToken, status, user } = useAppSelector((state) => state.auth);

  useEffect(() => {
    if (!accessToken || user || status === 'loading') {
      return;
    }
    void dispatch(fetchMe());
  }, [accessToken, dispatch, status, user]);

  useEffect(() => {
    const onAuthExpired = (): void => {
      dispatch(logout());
    };

    window.addEventListener(AUTH_EXPIRED_EVENT, onAuthExpired);
    return () => {
      window.removeEventListener(AUTH_EXPIRED_EVENT, onAuthExpired);
    };
  }, [dispatch]);

  return <RouterProvider router={router} />;
}
