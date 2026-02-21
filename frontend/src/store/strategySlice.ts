import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { strategyApi } from '../api/strategyApi';
import { resolveAssetSpotPrice } from '../hooks/useAssetSpotPrices';
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
  dataAttempted: boolean;
  loading: boolean;
  submitting: boolean;
  error: string | null;
}

const initialState: StrategyState = {
  assets: [],
  sellStrategies: [],
  buyStrategies: [],
  alerts: [],
  dataAttempted: false,
  loading: false,
  submitting: false,
  error: null
};

export const loadStrategyData = createAsyncThunk('strategy/loadData', async () => {
  const [assets, sellStrategies, buyStrategies, alerts] = await Promise.all([
    strategyApi.listAssets(),
    strategyApi.listSellStrategies(),
    strategyApi.listBuyStrategies(),
    strategyApi.listAlerts()
  ]);

  return { assets, sellStrategies, buyStrategies, alerts };
});

export const upsertSellStrategy = createAsyncThunk(
  'strategy/upsertSell',
  async (payload: UpsertSellStrategyPayload, { dispatch }) => {
    await strategyApi.upsertSellStrategy(payload);
    await dispatch(loadStrategyData()).unwrap();
  }
);

export const upsertBuyStrategy = createAsyncThunk(
  'strategy/upsertBuy',
  async (payload: UpsertBuyStrategyPayload, { dispatch, getState }) => {
    await strategyApi.upsertBuyStrategy(payload);
    await dispatch(loadStrategyData()).unwrap();

    const state = getState() as RootState;
    let asset = state.strategy.assets.find((row) => row.id === payload.assetId) ?? null;
    if (!asset) {
      const assets = await strategyApi.listAssets();
      asset = assets.find((row) => row.id === payload.assetId) ?? null;
    }
    if (!asset?.symbol) {
      return;
    }

    try {
      const priceState = await resolveAssetSpotPrice(asset.symbol);
      if (!priceState.priceUsd) {
        return;
      }

      const generated = await strategyApi.generateAlerts({
        assetId: payload.assetId,
        currentPriceUsd: priceState.priceUsd
      });
      if (generated.length > 0) {
        await dispatch(loadStrategyData()).unwrap();
      }
    } catch {
      // Saving strategy should not fail when live price lookup/alert generation is temporarily unavailable.
    }
  }
);

export const acknowledgeStrategyAlert = createAsyncThunk(
  'strategy/acknowledgeAlert',
  async (alertId: string, { dispatch }) => {
    await strategyApi.acknowledgeAlert(alertId);
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
      state.dataAttempted = true;
      state.assets = action.payload.assets;
      state.sellStrategies = action.payload.sellStrategies;
      state.buyStrategies = action.payload.buyStrategies;
      state.alerts = action.payload.alerts;
    });
    builder.addCase(loadStrategyData.rejected, (state, action) => {
      state.loading = false;
      state.dataAttempted = true;
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
