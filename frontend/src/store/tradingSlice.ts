import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { tradingApi } from '../api/tradingApi';
import type {
  AssetOption,
  ExchangeOption,
  PortfolioAssetPerformance,
  PortfolioSummary,
  TradeFormPayload,
  TransactionItem
} from '../types/trading';
import type { RootState } from './index';

interface TradingState {
  assets: AssetOption[];
  exchanges: ExchangeOption[];
  transactions: TransactionItem[];
  summary: PortfolioSummary | null;
  performance: PortfolioAssetPerformance[];
  loading: boolean;
  submitting: boolean;
  error: string | null;
}

const initialState: TradingState = {
  assets: [],
  exchanges: [],
  transactions: [],
  summary: null,
  performance: [],
  loading: false,
  submitting: false,
  error: null
};

function requireToken(state: RootState): string {
  const token = state.auth.accessToken;
  if (!token) {
    throw new Error('Missing access token');
  }
  return token;
}

export const loadTradingBootstrap = createAsyncThunk(
  'trading/loadBootstrap',
  async (_, { getState }) => {
    const state = getState() as RootState;
    const accessToken = requireToken(state);

    const [assets, exchanges, transactions, summary, performance] = await Promise.all([
      tradingApi.listAssets(),
      tradingApi.listExchanges(),
      tradingApi.listTransactions({ accessToken }),
      tradingApi.getPortfolioSummary({ accessToken }),
      tradingApi.getPortfolioPerformance({ accessToken })
    ]);

    return { assets, exchanges, transactions, summary, performance };
  }
);

export const submitBuyTrade = createAsyncThunk(
  'trading/submitBuy',
  async (payload: TradeFormPayload, { getState, dispatch }) => {
    const state = getState() as RootState;
    const accessToken = requireToken(state);
    await tradingApi.buy(payload, { accessToken });
    await dispatch(loadTradingBootstrap()).unwrap();
  }
);

export const submitSellTrade = createAsyncThunk(
  'trading/submitSell',
  async (payload: TradeFormPayload, { getState, dispatch }) => {
    const state = getState() as RootState;
    const accessToken = requireToken(state);
    await tradingApi.sell(payload, { accessToken });
    await dispatch(loadTradingBootstrap()).unwrap();
  }
);

const tradingSlice = createSlice({
  name: 'trading',
  initialState,
  reducers: {
    clearTradingError(state) {
      state.error = null;
    }
  },
  extraReducers: (builder) => {
    builder.addCase(loadTradingBootstrap.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(loadTradingBootstrap.fulfilled, (state, action) => {
      state.loading = false;
      state.assets = action.payload.assets;
      state.exchanges = action.payload.exchanges;
      state.transactions = action.payload.transactions;
      state.summary = action.payload.summary;
      state.performance = action.payload.performance;
    });
    builder.addCase(loadTradingBootstrap.rejected, (state, action) => {
      state.loading = false;
      state.error = action.error.message ?? 'Failed to load trading data';
    });

    builder.addCase(submitBuyTrade.pending, (state) => {
      state.submitting = true;
      state.error = null;
    });
    builder.addCase(submitBuyTrade.fulfilled, (state) => {
      state.submitting = false;
    });
    builder.addCase(submitBuyTrade.rejected, (state, action) => {
      state.submitting = false;
      state.error = action.error.message ?? 'Buy order failed';
    });

    builder.addCase(submitSellTrade.pending, (state) => {
      state.submitting = true;
      state.error = null;
    });
    builder.addCase(submitSellTrade.fulfilled, (state) => {
      state.submitting = false;
    });
    builder.addCase(submitSellTrade.rejected, (state, action) => {
      state.submitting = false;
      state.error = action.error.message ?? 'Sell order failed';
    });
  }
});

export const { clearTradingError } = tradingSlice.actions;
export default tradingSlice.reducer;
