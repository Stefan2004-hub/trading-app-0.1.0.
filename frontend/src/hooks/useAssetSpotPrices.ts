import { useEffect, useMemo, useState } from 'react';
import { tradingApi } from '../api/tradingApi';

type PriceSource = 'coinbase' | 'gateio';
type PriceStatus = 'loading' | 'success' | 'error';

interface PriceResult {
  priceUsd: string;
  source: PriceSource;
  fetchedAt: number;
}

export interface AssetPriceState {
  status: PriceStatus;
  priceUsd?: string;
  source?: PriceSource;
}

const CACHE_TTL_MS = 30_000;
const resultCache = new Map<string, PriceResult>();
const inFlightCache = new Map<string, Promise<PriceResult>>();

function normalizeSymbol(symbol: string): string {
  return symbol.trim().toUpperCase();
}

function isFresh(result: PriceResult): boolean {
  return Date.now() - result.fetchedAt < CACHE_TTL_MS;
}

async function resolveAssetPrice(symbol: string): Promise<PriceResult> {
  const cached = resultCache.get(symbol);
  if (cached && isFresh(cached)) {
    return cached;
  }

  const existingInFlight = inFlightCache.get(symbol);
  if (existingInFlight) {
    return existingInFlight;
  }

  const nextInFlight = (async () => {
    try {
      const response = await tradingApi.getSpotPrice(symbol);
      const result: PriceResult = {
        priceUsd: response.priceUsd,
        source: response.source,
        fetchedAt: Date.now()
      };
      resultCache.set(symbol, result);
      return result;
    } finally {
      inFlightCache.delete(symbol);
    }
  })();

  inFlightCache.set(symbol, nextInFlight);
  return nextInFlight;
}

export async function resolveAssetSpotPrice(symbol: string): Promise<AssetPriceState> {
  const normalized = normalizeSymbol(symbol);
  if (!normalized) {
    throw new Error('symbol is required');
  }

  const result = await resolveAssetPrice(normalized);
  return {
    status: 'success',
    priceUsd: result.priceUsd,
    source: result.source
  };
}

export function useAssetSpotPrices(symbols: string[]): Record<string, AssetPriceState> {
  const normalizedSymbols = useMemo(
    () => Array.from(new Set(symbols.map(normalizeSymbol).filter((symbol) => symbol.length > 0))),
    [symbols]
  );
  const [pricesBySymbol, setPricesBySymbol] = useState<Record<string, AssetPriceState>>({});

  useEffect(() => {
    let cancelled = false;

    setPricesBySymbol((current) => {
      const next: Record<string, AssetPriceState> = {};
      for (const symbol of normalizedSymbols) {
        const cached = resultCache.get(symbol);
        if (cached && isFresh(cached)) {
          next[symbol] = { status: 'success', priceUsd: cached.priceUsd, source: cached.source };
        } else if (current[symbol]?.status === 'success') {
          next[symbol] = current[symbol];
        } else {
          next[symbol] = { status: 'loading' };
        }
      }
      return next;
    });

    for (const symbol of normalizedSymbols) {
      const cached = resultCache.get(symbol);
      if (cached && isFresh(cached)) {
        continue;
      }

      void resolveAssetPrice(symbol)
        .then((result) => {
          if (cancelled) {
            return;
          }
          setPricesBySymbol((current) => ({
            ...current,
            [symbol]: { status: 'success', priceUsd: result.priceUsd, source: result.source }
          }));
        })
        .catch(() => {
          if (cancelled) {
            return;
          }
          setPricesBySymbol((current) => ({
            ...current,
            [symbol]: { status: 'error' }
          }));
        });
    }

    return () => {
      cancelled = true;
    };
  }, [normalizedSymbols]);

  return pricesBySymbol;
}
