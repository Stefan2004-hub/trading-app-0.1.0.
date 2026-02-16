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

interface TradingState {
  assets: AssetOption[];
  exchanges: ExchangeOption[];
  transactions: TransactionItem[];
  summary: PortfolioSummary | null;
  performance: PortfolioAssetPerformance[];
  bootstrapAttempted: boolean;
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
  bootstrapAttempted: false,
  loading: false,
  submitting: false,
  error: null
};

export const loadTradingBootstrap = createAsyncThunk('trading/loadBootstrap', async () => {
  const [assets, exchanges, transactions, summary, performance] = await Promise.all([
    tradingApi.listAssets(),
    tradingApi.listExchanges(),
    tradingApi.listTransactions(),
    tradingApi.getPortfolioSummary(),
    tradingApi.getPortfolioPerformance()
  ]);

  return { assets, exchanges, transactions, summary, performance };
});

export const submitBuyTrade = createAsyncThunk('trading/submitBuy', async (payload: TradeFormPayload, { dispatch }) => {
  await tradingApi.buy(payload);
  await dispatch(loadTradingBootstrap()).unwrap();
});

export const submitSellTrade = createAsyncThunk('trading/submitSell', async (payload: TradeFormPayload, { dispatch }) => {
  await tradingApi.sell(payload);
  await dispatch(loadTradingBootstrap()).unwrap();
});

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
      state.bootstrapAttempted = true;
      state.assets = action.payload.assets;
      state.exchanges = action.payload.exchanges;
      state.transactions = action.payload.transactions;
      state.summary = action.payload.summary;
      state.performance = action.payload.performance;
    });
    builder.addCase(loadTradingBootstrap.rejected, (state, action) => {
      state.loading = false;
      state.bootstrapAttempted = true;
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
