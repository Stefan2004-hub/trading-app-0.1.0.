import { useEffect, useRef } from 'react';
import { strategyApi } from '../api/strategyApi';
import { resolveAssetSpotPrice } from '../hooks/useAssetSpotPrices';
import { loadStrategyData } from '../store/strategySlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';

const ALERT_DEDUPE_TTL_MS = 30_000;

interface GenerationGuardState {
  priceKey: string;
  attemptedAt: number;
}

function toPriceKey(priceUsd: string): string {
  const numeric = Number(priceUsd);
  if (Number.isFinite(numeric)) {
    return numeric.toFixed(8);
  }
  return priceUsd.trim();
}

export function StrategyAlertAutoGenerator(): null {
  const dispatch = useAppDispatch();
  const authStatus = useAppSelector((state) => state.auth.status);
  const inProgressRef = useRef(false);
  const generationGuardByAssetRef = useRef<Map<string, GenerationGuardState>>(new Map());

  useEffect(() => {
    if (authStatus !== 'authenticated') {
      generationGuardByAssetRef.current.clear();
      inProgressRef.current = false;
      return;
    }

    let cancelled = false;

    const evaluateAlerts = async (): Promise<void> => {
      if (cancelled || inProgressRef.current) {
        return;
      }

      inProgressRef.current = true;
      try {
        const [assets, buyStrategies, sellStrategies] = await Promise.all([
          strategyApi.listAssets(),
          strategyApi.listBuyStrategies(),
          strategyApi.listSellStrategies()
        ]);
        if (cancelled) {
          return;
        }

        const activeAssetIds = new Set<string>();
        for (const strategy of buyStrategies) {
          if (strategy.active) {
            activeAssetIds.add(strategy.assetId);
          }
        }
        for (const strategy of sellStrategies) {
          if (strategy.active) {
            activeAssetIds.add(strategy.assetId);
          }
        }
        if (activeAssetIds.size === 0) {
          return;
        }

        const assetSymbolById = new Map<string, string>();
        for (const asset of assets) {
          assetSymbolById.set(asset.id, asset.symbol);
        }

        let generatedAnyAlert = false;
        const now = Date.now();
        for (const assetId of activeAssetIds) {
          const symbol = assetSymbolById.get(assetId);
          if (!symbol) {
            continue;
          }

          try {
            const priceState = await resolveAssetSpotPrice(symbol);
            const priceUsd = priceState.priceUsd;
            if (!priceUsd) {
              continue;
            }

            const priceKey = toPriceKey(priceUsd);
            const guard = generationGuardByAssetRef.current.get(assetId);
            if (guard && guard.priceKey === priceKey && now - guard.attemptedAt < ALERT_DEDUPE_TTL_MS) {
              continue;
            }
            generationGuardByAssetRef.current.set(assetId, {
              priceKey,
              attemptedAt: now
            });

            const generated = await strategyApi.generateAlerts({
              assetId,
              currentPriceUsd: priceUsd
            });
            if (generated.length > 0) {
              generatedAnyAlert = true;
            }
          } catch {
            // Ignore single-asset failures and continue remaining assets.
          }
        }

        if (generatedAnyAlert && !cancelled) {
          await dispatch(loadStrategyData()).unwrap();
        }
      } finally {
        inProgressRef.current = false;
      }
    };

    void evaluateAlerts();
    const onWindowFocus = (): void => {
      void evaluateAlerts();
    };
    const onVisibilityChange = (): void => {
      if (document.visibilityState === 'visible') {
        void evaluateAlerts();
      }
    };

    window.addEventListener('focus', onWindowFocus);
    document.addEventListener('visibilitychange', onVisibilityChange);

    return () => {
      cancelled = true;
      window.removeEventListener('focus', onWindowFocus);
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [authStatus, dispatch]);

  return null;
}
