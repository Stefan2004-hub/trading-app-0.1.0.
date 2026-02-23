import { useEffect, useMemo, useState } from 'react';
import { AppHeader } from '../components/AppHeader';
import { StrategyAlertList } from '../components/StrategyAlertList';
import { StrategyForms } from '../components/StrategyForms';
import { StrategyTable } from '../components/StrategyTable';
import {
  acknowledgeStrategyAlert,
  clearStrategyError,
  deleteBuyStrategy,
  deleteSellStrategy,
  deleteStrategyAlert,
  loadStrategyData,
  upsertBuyStrategy,
  upsertSellStrategy
} from '../store/strategySlice';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import type { ConfiguredStrategyFilter, ConfiguredStrategyItem } from '../types/strategy';

function ChevronsUpIcon({ className }: { className?: string }): JSX.Element {
  return (
    <svg aria-hidden="true" className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M7 14l5-5 5 5" />
      <path d="M7 20l5-5 5 5" />
    </svg>
  );
}

function ChevronsDownIcon({ className }: { className?: string }): JSX.Element {
  return (
    <svg aria-hidden="true" className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M7 10l5 5 5-5" />
      <path d="M7 4l5 5 5-5" />
    </svg>
  );
}

export function StrategiesPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const { assets, sellStrategies, buyStrategies, alerts, loading, dataAttempted, submitting, error } = useAppSelector(
    (state) => state.strategy
  );
  const [isFocused, setIsFocused] = useState(false);
  const [searchSymbol, setSearchSymbol] = useState('');
  const [strategyFilter, setStrategyFilter] = useState<ConfiguredStrategyFilter>('ALL');

  useEffect(() => {
    if (loading || dataAttempted) {
      return;
    }
    void dispatch(loadStrategyData());
  }, [dataAttempted, dispatch, loading]);

  const assetSymbolById = useMemo(() => {
    return new Map(assets.map((asset) => [asset.id, asset.symbol]));
  }, [assets]);

  const configuredStrategies = useMemo<ConfiguredStrategyItem[]>(() => {
    const sellRows = sellStrategies.map((item) => ({
      id: item.id,
      strategyType: 'SELL' as const,
      assetId: item.assetId,
      assetSymbol: assetSymbolById.get(item.assetId) ?? item.assetId,
      thresholdPercent: item.thresholdPercent,
      buyAmountUsd: null,
      active: item.active,
      updatedAt: item.updatedAt
    }));
    const buyRows = buyStrategies.map((item) => ({
      id: item.id,
      strategyType: 'BUY' as const,
      assetId: item.assetId,
      assetSymbol: assetSymbolById.get(item.assetId) ?? item.assetId,
      thresholdPercent: item.dipThresholdPercent,
      buyAmountUsd: item.buyAmountUsd,
      active: item.active,
      updatedAt: item.updatedAt
    }));

    return [...sellRows, ...buyRows];
  }, [assetSymbolById, buyStrategies, sellStrategies]);

  const filteredConfiguredStrategies = useMemo(() => {
    const query = searchSymbol.trim().toUpperCase();

    return configuredStrategies.filter((item) => {
      if (strategyFilter !== 'ALL' && item.strategyType !== strategyFilter) {
        return false;
      }
      if (!query) {
        return true;
      }
      return item.assetSymbol.toUpperCase().includes(query);
    });
  }, [configuredStrategies, searchSymbol, strategyFilter]);

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel strategies-workspace-panel">
        <h1>Strategies</h1>
        {error ? <p className="auth-error">{error}</p> : null}
        {!isFocused ? (
          <div className="strategies-focus-enter-row">
            <button
              type="button"
              className="ui-button ui-button-secondary strategies-focus-button"
              onClick={() => setIsFocused(true)}
            >
              <ChevronsUpIcon className="strategies-focus-icon" />
              Focus Configured View
            </button>
          </div>
        ) : null}

        <div className={`strategies-top-sections${isFocused ? ' collapsed' : ''}`}>
          <StrategyForms
            assets={assets}
            submitting={submitting}
            onSubmitSell={async (payload) => {
              dispatch(clearStrategyError());
              const action = await dispatch(upsertSellStrategy(payload));
              return upsertSellStrategy.fulfilled.match(action);
            }}
            onSubmitBuy={async (payload) => {
              dispatch(clearStrategyError());
              const action = await dispatch(upsertBuyStrategy(payload));
              return upsertBuyStrategy.fulfilled.match(action);
            }}
          />

          <StrategyAlertList
            alerts={alerts}
            assets={assets}
            submitting={submitting}
            onAcknowledge={async (alertId) => {
              dispatch(clearStrategyError());
              const action = await dispatch(acknowledgeStrategyAlert(alertId));
              return acknowledgeStrategyAlert.fulfilled.match(action);
            }}
            onDelete={async (alertId) => {
              dispatch(clearStrategyError());
              const action = await dispatch(deleteStrategyAlert(alertId));
              return deleteStrategyAlert.fulfilled.match(action);
            }}
          />
        </div>

        <section className={`configured-strategies-panel${isFocused ? ' focused' : ''}`}>
          <div className="configured-strategies-controls">
            <label className="configured-strategies-search">
              <span>Search by Asset Symbol</span>
              <input
                type="search"
                value={searchSymbol}
                placeholder="e.g. BTC"
                onChange={(event) => setSearchSymbol(event.target.value)}
              />
            </label>
            <div className="configured-strategies-filter-group" role="group" aria-label="Filter strategy type">
              {(['ALL', 'SELL', 'BUY'] as const).map((value) => (
                <button
                  key={value}
                  type="button"
                  className={`configured-strategies-filter-button${strategyFilter === value ? ' active' : ''}`}
                  onClick={() => setStrategyFilter(value)}
                >
                  {value}
                </button>
              ))}
            </div>
          </div>

          <StrategyTable
            strategies={filteredConfiguredStrategies}
            submitting={submitting}
            className={isFocused ? 'configured-strategies-table focused' : 'configured-strategies-table'}
            tableWrapClassName={isFocused ? 'configured-strategies-scroll' : undefined}
            onDeleteSell={async (strategyId) => {
              dispatch(clearStrategyError());
              const action = await dispatch(deleteSellStrategy(strategyId));
              return deleteSellStrategy.fulfilled.match(action);
            }}
            onDeleteBuy={async (strategyId) => {
              dispatch(clearStrategyError());
              const action = await dispatch(deleteBuyStrategy(strategyId));
              return deleteBuyStrategy.fulfilled.match(action);
            }}
          />

          {isFocused ? (
            <div className="strategies-focus-exit-row">
              <button
                type="button"
                className="ui-button ui-button-secondary strategies-focus-button"
                onClick={() => setIsFocused(false)}
              >
                <ChevronsDownIcon className="strategies-focus-icon" />
                Exit Focus Mode
              </button>
            </div>
          ) : null}
        </section>
      </section>
    </main>
  );
}
