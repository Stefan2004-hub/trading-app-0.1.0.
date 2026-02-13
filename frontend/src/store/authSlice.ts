import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import { authApi } from '../api/authApi';
import type { AuthResponse, LoginPayload, RegisterPayload, UserProfile } from '../types/auth';
import { sessionStorageUtil } from '../utils/storage';

type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'unauthenticated';

interface AuthState {
  status: AuthStatus;
  user: UserProfile | null;
  accessToken: string | null;
  refreshToken: string | null;
  error: string | null;
}

const storedSession = sessionStorageUtil.read();

const initialState: AuthState = storedSession
  ? {
      status: 'authenticated',
      user: storedSession.user,
      accessToken: storedSession.accessToken,
      refreshToken: storedSession.refreshToken,
      error: null
    }
  : {
      status: 'unauthenticated',
      user: null,
      accessToken: null,
      refreshToken: null,
      error: null
    };

function persistSession(payload: AuthResponse): void {
  sessionStorageUtil.write({
    accessToken: payload.accessToken,
    refreshToken: payload.refreshToken,
    user: {
      userId: payload.userId,
      email: payload.email,
      username: payload.username,
      authProvider: payload.authProvider
    }
  });
}

export const login = createAsyncThunk('auth/login', async (payload: LoginPayload) => {
  return authApi.login(payload);
});

export const register = createAsyncThunk('auth/register', async (payload: RegisterPayload) => {
  return authApi.register(payload);
});

export const fetchMe = createAsyncThunk('auth/me', async (accessToken: string) => {
  return authApi.me(accessToken);
});

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout(state) {
      sessionStorageUtil.clear();
      state.status = 'unauthenticated';
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
      state.error = null;
    },
    clearAuthError(state) {
      state.error = null;
    }
  },
  extraReducers: (builder) => {
    builder.addCase(login.pending, (state) => {
      state.status = 'loading';
      state.error = null;
    });
    builder.addCase(login.fulfilled, (state, action: PayloadAction<AuthResponse>) => {
      persistSession(action.payload);
      state.status = 'authenticated';
      state.user = {
        userId: action.payload.userId,
        email: action.payload.email,
        username: action.payload.username,
        authProvider: action.payload.authProvider
      };
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      state.error = null;
    });
    builder.addCase(login.rejected, (state, action) => {
      sessionStorageUtil.clear();
      state.status = 'unauthenticated';
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
      state.error = action.error.message ?? 'Login failed';
    });

    builder.addCase(register.pending, (state) => {
      state.status = 'loading';
      state.error = null;
    });
    builder.addCase(register.fulfilled, (state) => {
      state.status = 'unauthenticated';
      state.error = null;
    });
    builder.addCase(register.rejected, (state, action) => {
      state.status = 'unauthenticated';
      state.error = action.error.message ?? 'Registration failed';
    });

    builder.addCase(fetchMe.pending, (state) => {
      state.status = 'loading';
      state.error = null;
    });
    builder.addCase(fetchMe.fulfilled, (state, action: PayloadAction<UserProfile>) => {
      state.status = 'authenticated';
      state.user = action.payload;
      state.error = null;
    });
    builder.addCase(fetchMe.rejected, (state, action) => {
      sessionStorageUtil.clear();
      state.status = 'unauthenticated';
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
      state.error = action.error.message ?? 'Session expired';
    });
  }
});

export const { logout, clearAuthError } = authSlice.actions;
export default authSlice.reducer;
