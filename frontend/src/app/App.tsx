import { useEffect } from 'react';
import { RouterProvider } from 'react-router-dom';
import { fetchMe } from '../store/authSlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { router } from './router';

export function App(): JSX.Element {
  const dispatch = useAppDispatch();
  const { accessToken, status, user } = useAppSelector((state) => state.auth);

  useEffect(() => {
    if (!accessToken || user || status === 'loading') {
      return;
    }
    void dispatch(fetchMe(accessToken));
  }, [accessToken, dispatch, status, user]);

  return <RouterProvider router={router} />;
}
