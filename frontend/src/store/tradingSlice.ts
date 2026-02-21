import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { tradingApi } from '../api/tradingApi';
import { userApi } from '../api/userApi';
import type {
  AccumulationTradeItem,
  AssetOption,
  BuyInputMode,
  ExchangeOption,
  PortfolioAssetPerformance,
  PortfolioSummary,
  TradeFormPayload,
  TransactionItem,
  TransactionView,
  UpdateTransactionNetAmountPayload,
  UpdateTransactionPayload
} from '../types/trading';
import type { UserPreferences } from '../types/userPreferences';

interface TradingState {
  assets: AssetOption[];
  exchanges: ExchangeOption[];
  transactions: TransactionItem[];
  accumulationTrades: AccumulationTradeItem[];
  transactionPage: number;
  transactionPageSize: number;
  transactionTotalPages: number;
  transactionTotalElements: number;
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
  accumulationTrades: [],
  transactionPage: 0,
  transactionPageSize: 20,
  transactionTotalPages: 0,
  transactionTotalElements: 0,
  summary: null,
  performance: [],
  userPreferences: null,
  bootstrapAttempted: false,
  loading: false,
  submitting: false,
  error: null
};

export const loadTradingBootstrap = createAsyncThunk('trading/loadBootstrap', async (userId?: string) => {
  const accumulationTradesPromise = tradingApi.listAccumulationTrades({ userId }).catch(() => [] as AccumulationTradeItem[]);
  const [assets, exchanges, transactionsPage, summary, performance, userPreferences, accumulationTrades] = await Promise.all([
    tradingApi.listAssets(),
    tradingApi.listExchanges(),
    tradingApi.listTransactions({ page: 0, size: 20 }),
    tradingApi.getPortfolioSummary(),
    tradingApi.getPortfolioPerformance(),
    userApi.getPreferences(),
    accumulationTradesPromise
  ]);

  return { assets, exchanges, transactionsPage, summary, performance, userPreferences, accumulationTrades };
});

export const loadTransactions = createAsyncThunk(
  'trading/loadTransactions',
  async ({
    page,
    size,
    search,
    view,
    groupSize
  }: {
    page: number;
    size: number;
    search?: string;
    view?: TransactionView;
    groupSize?: number;
  }) => tradingApi.listTransactions({ page, size, search, view, groupSize })
);

export const submitBuyTrade = createAsyncThunk(
  'trading/submitBuy',
  async ({ payload, userId }: { payload: TradeFormPayload; userId?: string }, { dispatch }) => {
    const createdTransaction = await tradingApi.buy(payload);
    await dispatch(loadTradingBootstrap(userId)).unwrap();
    return createdTransaction;
  }
);

export const submitBuyAndCloseAccumulation = createAsyncThunk(
  'trading/submitBuyAndCloseAccumulation',
  async (
    { payload, accumulationTradeId, userId }: { payload: TradeFormPayload; accumulationTradeId: string; userId?: string },
    { dispatch }
  ) => {
    const createdTransaction = await tradingApi.buy(payload);
    await tradingApi.closeAccumulationTrade({
      accumulationTradeId,
      reentryTransactionId: createdTransaction.id,
      userId
    });
    await dispatch(loadTradingBootstrap(userId)).unwrap();
    return createdTransaction;
  }
);

export const submitSellTrade = createAsyncThunk(
  'trading/submitSell',
  async ({ payload, userId }: { payload: TradeFormPayload; userId?: string }, { dispatch }) => {
    const createdTransaction = await tradingApi.sell(payload);
    await dispatch(loadTradingBootstrap(userId)).unwrap();
    return createdTransaction;
  }
);

export const updateTransaction = createAsyncThunk(
  'trading/updateTransaction',
  async ({ id, payload, userId }: { id: string; payload: UpdateTransactionPayload; userId?: string }, { dispatch }) => {
    await tradingApi.updateTransaction(id, payload);
    await dispatch(loadTradingBootstrap(userId)).unwrap();
  }
);

export const updateTransactionNetAmount = createAsyncThunk(
  'trading/updateTransactionNetAmount',
  async (
    { id, payload, userId }: { id: string; payload: UpdateTransactionNetAmountPayload; userId?: string },
    { dispatch }
  ) => {
    await tradingApi.updateTransactionNetAmount(id, payload);
    await dispatch(loadTradingBootstrap(userId)).unwrap();
  }
);

export const deleteTransaction = createAsyncThunk(
  'trading/deleteTransaction',
  async ({ id, userId }: { id: string; userId?: string }, { dispatch }) => {
    await tradingApi.deleteTransaction(id);
    await dispatch(loadTradingBootstrap(userId)).unwrap();
  }
);

export const openAccumulationTrade = createAsyncThunk(
  'trading/openAccumulationTrade',
  async (
    payload: { exitTransactionId: string; predictionNotes?: string; userId?: string },
    { dispatch }
  ) => {
    const openedTrade = await tradingApi.openAccumulationTrade(payload);
    await dispatch(loadTradingBootstrap(payload.userId)).unwrap();
    return openedTrade;
  }
);

export const closeAccumulationTrade = createAsyncThunk(
  'trading/closeAccumulationTrade',
  async (
    payload: { accumulationTradeId: string; reentryTransactionId: string; predictionNotes?: string; userId?: string },
    { dispatch }
  ) => {
    const closedTrade = await tradingApi.closeAccumulationTrade(payload);
    await dispatch(loadTradingBootstrap(payload.userId)).unwrap();
    return closedTrade;
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
      state.transactions = action.payload.transactionsPage.content;
      state.transactionPage = action.payload.transactionsPage.number;
      state.transactionPageSize = action.payload.transactionsPage.size;
      state.transactionTotalPages = action.payload.transactionsPage.totalPages;
      state.transactionTotalElements = action.payload.transactionsPage.totalElements;
      state.accumulationTrades = action.payload.accumulationTrades;
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
      state.transactions = action.payload.content;
      state.transactionPage = action.payload.number;
      state.transactionPageSize = action.payload.size;
      state.transactionTotalPages = action.payload.totalPages;
      state.transactionTotalElements = action.payload.totalElements;
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

    builder.addCase(submitBuyAndCloseAccumulation.pending, (state) => {
      state.submitting = true;
      state.error = null;
    });
    builder.addCase(submitBuyAndCloseAccumulation.fulfilled, (state) => {
      state.submitting = false;
    });
    builder.addCase(submitBuyAndCloseAccumulation.rejected, (state, action) => {
      state.submitting = false;
      state.error = action.error.message ?? 'Buy and close accumulation failed';
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

    builder.addCase(openAccumulationTrade.pending, (state) => {
      state.submitting = true;
      state.error = null;
    });
    builder.addCase(openAccumulationTrade.fulfilled, (state) => {
      state.submitting = false;
    });
    builder.addCase(openAccumulationTrade.rejected, (state, action) => {
      state.submitting = false;
      state.error = action.error.message ?? 'Open accumulation trade failed';
    });

    builder.addCase(closeAccumulationTrade.pending, (state) => {
      state.submitting = true;
      state.error = null;
    });
    builder.addCase(closeAccumulationTrade.fulfilled, (state) => {
      state.submitting = false;
    });
    builder.addCase(closeAccumulationTrade.rejected, (state, action) => {
      state.submitting = false;
      state.error = action.error.message ?? 'Close accumulation trade failed';
    });

    builder.addCase(updateDefaultBuyInputMode.fulfilled, (state, action) => {
      state.userPreferences = action.payload;
    });
  }
});

export const { clearTradingError } = tradingSlice.actions;
export default tradingSlice.reducer;
