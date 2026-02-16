import { useEffect, useRef } from 'react';
import { RouterProvider } from 'react-router-dom';
import { AUTH_EXPIRED_EVENT } from '../api/http';
import { fetchMe, logout } from '../store/authSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { router } from './router';

export function App(): JSX.Element {
  const dispatch = useAppDispatch();
  const { accessToken, status } = useAppSelector((state) => state.auth);
  const validatedTokenRef = useRef<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      validatedTokenRef.current = null;
      return;
    }
    if (status === 'loading') {
      return;
    }
    if (validatedTokenRef.current === accessToken) {
      return;
    }
    validatedTokenRef.current = accessToken;
    // Validate each token once on app startup or token change.
    void dispatch(fetchMe());
  }, [accessToken, dispatch, status]);

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
