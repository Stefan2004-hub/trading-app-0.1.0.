import { useEffect, useMemo, useState } from 'react';

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

async function fetchCoinbaseSpotPrice(symbol: string): Promise<string> {
  const response = await fetch(`https://api.coinbase.com/v2/prices/${symbol}-USD/spot`);
  if (!response.ok) {
    throw new Error(`Coinbase returned ${response.status}`);
  }

  const payload = (await response.json()) as { data?: { amount?: string } };
  const amount = payload?.data?.amount;
  if (!amount) {
    throw new Error('Coinbase amount missing');
  }

  return amount;
}

async function fetchGateIoSpotPrice(symbol: string): Promise<string> {
  const response = await fetch(`https://api.gateio.ws/api/v4/spot/tickers?currency_pair=${symbol}_USDT`);
  if (!response.ok) {
    throw new Error(`Gate.io returned ${response.status}`);
  }

  const payload = (await response.json()) as Array<{ last?: string }>;
  const amount = payload[0]?.last;
  if (!amount) {
    throw new Error('Gate.io last price missing');
  }

  return amount;
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
      const coinbasePrice = await fetchCoinbaseSpotPrice(symbol);
      const coinbaseResult: PriceResult = {
        priceUsd: coinbasePrice,
        source: 'coinbase',
        fetchedAt: Date.now()
      };
      resultCache.set(symbol, coinbaseResult);
      return coinbaseResult;
    } catch {
      const gatePrice = await fetchGateIoSpotPrice(symbol);
      const gateResult: PriceResult = {
        priceUsd: gatePrice,
        source: 'gateio',
        fetchedAt: Date.now()
      };
      resultCache.set(symbol, gateResult);
      return gateResult;
    } finally {
      inFlightCache.delete(symbol);
    }
  })();

  inFlightCache.set(symbol, nextInFlight);
  return nextInFlight;
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
