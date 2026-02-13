import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';
import strategyReducer from './strategySlice';
import tradingReducer from './tradingSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    trading: tradingReducer,
    strategy: strategyReducer
  }
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
