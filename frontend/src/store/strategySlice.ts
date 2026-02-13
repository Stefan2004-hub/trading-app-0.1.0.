import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { strategyApi } from '../api/strategyApi';
import type {
  BuyStrategyItem,
  SellStrategyItem,
  StrategyAlertItem,
  UpsertBuyStrategyPayload,
  UpsertSellStrategyPayload
} from '../types/strategy';
import type { AssetOption } from '../types/trading';
import type { RootState } from './index';

interface StrategyState {
  assets: AssetOption[];
  sellStrategies: SellStrategyItem[];
  buyStrategies: BuyStrategyItem[];
  alerts: StrategyAlertItem[];
  loading: boolean;
  submitting: boolean;
  error: string | null;
}

const initialState: StrategyState = {
  assets: [],
  sellStrategies: [],
  buyStrategies: [],
  alerts: [],
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

export const loadStrategyData = createAsyncThunk('strategy/loadData', async (_, { getState }) => {
  const state = getState() as RootState;
  const accessToken = requireToken(state);

  const [assets, sellStrategies, buyStrategies, alerts] = await Promise.all([
    strategyApi.listAssets(),
    strategyApi.listSellStrategies({ accessToken }),
    strategyApi.listBuyStrategies({ accessToken }),
    strategyApi.listAlerts({ accessToken })
  ]);

  return { assets, sellStrategies, buyStrategies, alerts };
});

export const upsertSellStrategy = createAsyncThunk(
  'strategy/upsertSell',
  async (payload: UpsertSellStrategyPayload, { getState, dispatch }) => {
    const state = getState() as RootState;
    const accessToken = requireToken(state);
    await strategyApi.upsertSellStrategy(payload, { accessToken });
    await dispatch(loadStrategyData()).unwrap();
  }
);

export const upsertBuyStrategy = createAsyncThunk(
  'strategy/upsertBuy',
  async (payload: UpsertBuyStrategyPayload, { getState, dispatch }) => {
    const state = getState() as RootState;
    const accessToken = requireToken(state);
    await strategyApi.upsertBuyStrategy(payload, { accessToken });
    await dispatch(loadStrategyData()).unwrap();
  }
);

export const acknowledgeStrategyAlert = createAsyncThunk(
  'strategy/acknowledgeAlert',
  async (alertId: string, { getState, dispatch }) => {
    const state = getState() as RootState;
    const accessToken = requireToken(state);
    await strategyApi.acknowledgeAlert(alertId, { accessToken });
    await dispatch(loadStrategyData()).unwrap();
  }
);

const strategySlice = createSlice({
  name: 'strategy',
  initialState,
  reducers: {
    clearStrategyError(state) {
      state.error = null;
    }
  },
  extraReducers: (builder) => {
    builder.addCase(loadStrategyData.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(loadStrategyData.fulfilled, (state, action) => {
      state.loading = false;
      state.assets = action.payload.assets;
      state.sellStrategies = action.payload.sellStrategies;
      state.buyStrategies = action.payload.buyStrategies;
      state.alerts = action.payload.alerts;
    });
    builder.addCase(loadStrategyData.rejected, (state, action) => {
      state.loading = false;
      state.error = action.error.message ?? 'Failed to load strategy data';
    });

    builder.addCase(upsertSellStrategy.pending, (state) => {
      state.submitting = true;
      state.error = null;
    });
    builder.addCase(upsertSellStrategy.fulfilled, (state) => {
      state.submitting = false;
    });
    builder.addCase(upsertSellStrategy.rejected, (state, action) => {
      state.submitting = false;
      state.error = action.error.message ?? 'Failed to save sell strategy';
    });

    builder.addCase(upsertBuyStrategy.pending, (state) => {
      state.submitting = true;
      state.error = null;
    });
    builder.addCase(upsertBuyStrategy.fulfilled, (state) => {
      state.submitting = false;
    });
    builder.addCase(upsertBuyStrategy.rejected, (state, action) => {
      state.submitting = false;
      state.error = action.error.message ?? 'Failed to save buy strategy';
    });

    builder.addCase(acknowledgeStrategyAlert.pending, (state) => {
      state.submitting = true;
      state.error = null;
    });
    builder.addCase(acknowledgeStrategyAlert.fulfilled, (state) => {
      state.submitting = false;
    });
    builder.addCase(acknowledgeStrategyAlert.rejected, (state, action) => {
      state.submitting = false;
      state.error = action.error.message ?? 'Failed to acknowledge alert';
    });
  }
});

export const { clearStrategyError } = strategySlice.actions;
export default strategySlice.reducer;
