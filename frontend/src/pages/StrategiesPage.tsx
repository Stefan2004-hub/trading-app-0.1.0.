import { useEffect } from 'react';
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

export function StrategiesPage(): JSX.Element {
  const dispatch = useAppDispatch();
  const { assets, sellStrategies, buyStrategies, alerts, loading, dataAttempted, submitting, error } = useAppSelector(
    (state) => state.strategy
  );

  useEffect(() => {
    if (loading || dataAttempted) {
      return;
    }
    void dispatch(loadStrategyData());
  }, [dataAttempted, dispatch, loading]);

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Strategies</h1>
        {error ? <p className="auth-error">{error}</p> : null}

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

        <StrategyTable
          assets={assets}
          sellStrategies={sellStrategies}
          buyStrategies={buyStrategies}
          submitting={submitting}
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
      </section>
    </main>
  );
}
