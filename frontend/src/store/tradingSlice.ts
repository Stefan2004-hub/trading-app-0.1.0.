import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { tradingApi } from '../api/tradingApi';
import { userApi } from '../api/userApi';
import type {
  AssetOption,
  BuyInputMode,
  ExchangeOption,
  PortfolioAssetPerformance,
  PortfolioSummary,
  TradeFormPayload,
  TransactionItem,
  UpdateTransactionNetAmountPayload,
  UpdateTransactionPayload
} from '../types/trading';
import type { UserPreferences } from '../types/userPreferences';

interface TradingState {
  assets: AssetOption[];
  exchanges: ExchangeOption[];
  transactions: TransactionItem[];
  summary: PortfolioSummary | null;
  performance: PortfolioAssetPerformance[];
  userPreferences: UserPreferences | null;
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
  userPreferences: null,
  bootstrapAttempted: false,
  loading: false,
  submitting: false,
  error: null
};

export const loadTradingBootstrap = createAsyncThunk('trading/loadBootstrap', async () => {
  const [assets, exchanges, transactions, summary, performance, userPreferences] = await Promise.all([
    tradingApi.listAssets(),
    tradingApi.listExchanges(),
    tradingApi.listTransactions(),
    tradingApi.getPortfolioSummary(),
    tradingApi.getPortfolioPerformance(),
    userApi.getPreferences()
  ]);

  return { assets, exchanges, transactions, summary, performance, userPreferences };
});

export const loadTransactions = createAsyncThunk('trading/loadTransactions', async (search: string | undefined) => {
  return tradingApi.listTransactions(search);
});

export const submitBuyTrade = createAsyncThunk('trading/submitBuy', async (payload: TradeFormPayload, { dispatch }) => {
  await tradingApi.buy(payload);
  await dispatch(loadTradingBootstrap()).unwrap();
});

export const submitSellTrade = createAsyncThunk('trading/submitSell', async (payload: TradeFormPayload, { dispatch }) => {
  await tradingApi.sell(payload);
  await dispatch(loadTradingBootstrap()).unwrap();
});

export const updateTransaction = createAsyncThunk(
  'trading/updateTransaction',
  async ({ id, payload }: { id: string; payload: UpdateTransactionPayload }, { dispatch }) => {
    await tradingApi.updateTransaction(id, payload);
    await dispatch(loadTradingBootstrap()).unwrap();
  }
);

export const updateTransactionNetAmount = createAsyncThunk(
  'trading/updateTransactionNetAmount',
  async ({ id, payload }: { id: string; payload: UpdateTransactionNetAmountPayload }, { dispatch }) => {
    await tradingApi.updateTransactionNetAmount(id, payload);
    await dispatch(loadTradingBootstrap()).unwrap();
  }
);

export const deleteTransaction = createAsyncThunk(
  'trading/deleteTransaction',
  async (id: string, { dispatch }) => {
    await tradingApi.deleteTransaction(id);
    await dispatch(loadTradingBootstrap()).unwrap();
  }
);

export const updateDefaultBuyInputMode = createAsyncThunk(
  'trading/updateDefaultBuyInputMode',
  async (mode: BuyInputMode) => userApi.updateDefaultBuyInputMode(mode)
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
      state.bootstrapAttempted = true;
      state.assets = action.payload.assets;
      state.exchanges = action.payload.exchanges;
      state.transactions = action.payload.transactions;
      state.summary = action.payload.summary;
      state.performance = action.payload.performance;
      state.userPreferences = action.payload.userPreferences;
    });
    builder.addCase(loadTradingBootstrap.rejected, (state, action) => {
      state.loading = false;
      state.bootstrapAttempted = true;
      state.error = action.error.message ?? 'Failed to load trading data';
    });

    builder.addCase(loadTransactions.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(loadTransactions.fulfilled, (state, action) => {
      state.loading = false;
      state.transactions = action.payload;
    });
    builder.addCase(loadTransactions.rejected, (state, action) => {
      state.loading = false;
      state.error = action.error.message ?? 'Failed to load transactions';
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

    builder.addCase(updateTransaction.pending, (state) => {
      state.submitting = true;
      state.error = null;
    });
    builder.addCase(updateTransaction.fulfilled, (state) => {
      state.submitting = false;
    });
    builder.addCase(updateTransaction.rejected, (state, action) => {
      state.submitting = false;
      state.error = action.error.message ?? 'Update transaction failed';
    });

    builder.addCase(updateTransactionNetAmount.pending, (state) => {
      state.submitting = true;
      state.error = null;
    });
    builder.addCase(updateTransactionNetAmount.fulfilled, (state) => {
      state.submitting = false;
    });
    builder.addCase(updateTransactionNetAmount.rejected, (state, action) => {
      state.submitting = false;
      state.error = action.error.message ?? 'Update transaction quantity failed';
    });

    builder.addCase(deleteTransaction.pending, (state) => {
      state.submitting = true;
      state.error = null;
    });
    builder.addCase(deleteTransaction.fulfilled, (state) => {
      state.submitting = false;
    });
    builder.addCase(deleteTransaction.rejected, (state, action) => {
      state.submitting = false;
      state.error = action.error.message ?? 'Delete transaction failed';
    });

    builder.addCase(updateDefaultBuyInputMode.fulfilled, (state, action) => {
      state.userPreferences = action.payload;
    });
  }
});

export const { clearTradingError } = tradingSlice.actions;
export default tradingSlice.reducer;
