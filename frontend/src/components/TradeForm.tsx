import { FormEvent, useEffect, useMemo, useState } from 'react';
import type { BuyInputMode, TradeFormPayload, TransactionType } from '../types/trading';
import type { AssetOption, ExchangeOption } from '../types/trading';

type FeeInputMode = 'PERCENTAGE' | 'AMOUNT' | null;

interface TradeFormProps {
  title: string;
  tradeType: TransactionType;
  assets: AssetOption[];
  exchanges: ExchangeOption[];
  submitting: boolean;
  defaultBuyInputMode?: BuyInputMode;
  onBuyInputModeChange?: (mode: BuyInputMode) => void;
  onSubmit: (payload: TradeFormPayload) => Promise<boolean>;
}

interface LocalTradeFormState {
  assetId: string;
  exchangeId: string;
  grossAmount: string;
  usdAmount: string;
  feeAmount: string;
  feePercentage: string;
  feeCurrency: string;
  unitPriceUsd: string;
  inputMode: BuyInputMode;
}

const INITIAL_FORM: LocalTradeFormState = {
  assetId: '',
  exchangeId: '',
  grossAmount: '',
  usdAmount: '',
  feeAmount: '',
  feePercentage: '',
  feeCurrency: '',
  unitPriceUsd: '',
  inputMode: 'COIN_AMOUNT'
};

function parsePositive(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  const numeric = Number(trimmed);
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null;
}

function formatCalculated(value: number): string {
  return Number.isFinite(value) ? value.toFixed(18).replace(/\.?0+$/, '') : '';
}

export function TradeForm({
  title,
  tradeType,
  assets,
  exchanges,
  submitting,
  defaultBuyInputMode,
  onBuyInputModeChange,
  onSubmit
}: TradeFormProps): JSX.Element {
  const [form, setForm] = useState<LocalTradeFormState>(INITIAL_FORM);
  const [feeInputMode, setFeeInputMode] = useState<FeeInputMode>(null);

  useEffect(() => {
    if (tradeType === 'BUY' && defaultBuyInputMode && defaultBuyInputMode !== form.inputMode) {
      setForm((current) => ({ ...current, inputMode: defaultBuyInputMode }));
    }
  }, [defaultBuyInputMode, form.inputMode, tradeType]);

  const selectedAssetSymbol = useMemo(
    () => assets.find((asset) => asset.id === form.assetId)?.symbol ?? '',
    [assets, form.assetId]
  );

  const effectiveGrossAmount = useMemo(() => {
    if (tradeType === 'BUY' && form.inputMode === 'USD_AMOUNT') {
      const usdAmount = parsePositive(form.usdAmount);
      const unitPrice = parsePositive(form.unitPriceUsd);
      if (!usdAmount || !unitPrice) {
        return null;
      }
      return usdAmount / unitPrice;
    }
    return parsePositive(form.grossAmount);
  }, [form.grossAmount, form.inputMode, form.unitPriceUsd, form.usdAmount, tradeType]);

  function effectiveGrossFrom(nextForm: LocalTradeFormState): number | null {
    if (tradeType === 'BUY' && nextForm.inputMode === 'USD_AMOUNT') {
      const usdAmount = parsePositive(nextForm.usdAmount);
      const unitPrice = parsePositive(nextForm.unitPriceUsd);
      if (!usdAmount || !unitPrice) {
        return null;
      }
      return usdAmount / unitPrice;
    }
    return parsePositive(nextForm.grossAmount);
  }

  function syncFeeByMode(nextForm: LocalTradeFormState, mode: FeeInputMode): LocalTradeFormState {
    const grossAmount = effectiveGrossFrom(nextForm);
    const nextAssetSymbol = assets.find((asset) => asset.id === nextForm.assetId)?.symbol ?? '';
    const feeCurrency = nextForm.feeCurrency || nextAssetSymbol;
    if (!mode || !grossAmount) {
      return nextForm;
    }

    if (mode === 'PERCENTAGE') {
      const percentage = parsePositive(nextForm.feePercentage);
      if (!percentage) {
        return { ...nextForm, feeAmount: '' };
      }
      return {
        ...nextForm,
        feeAmount: formatCalculated((grossAmount * percentage) / 100),
        feeCurrency
      };
    }

    const amount = parsePositive(nextForm.feeAmount);
    if (!amount) {
      return { ...nextForm, feePercentage: '' };
    }

    return {
      ...nextForm,
      feePercentage: formatCalculated((amount / grossAmount) * 100),
      feeCurrency
    };
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();

    const payload: TradeFormPayload = {
      assetId: form.assetId,
      exchangeId: form.exchangeId,
      unitPriceUsd: form.unitPriceUsd,
      feeAmount: form.feeAmount.trim() ? form.feeAmount.trim() : undefined,
      feePercentage: form.feePercentage.trim() ? form.feePercentage.trim() : undefined,
      feeCurrency: form.feeCurrency.trim() ? form.feeCurrency.trim().toUpperCase() : undefined
    };

    if (tradeType === 'BUY') {
      payload.inputMode = form.inputMode;
      if (form.inputMode === 'USD_AMOUNT') {
        payload.usdAmount = form.usdAmount;
      } else {
        payload.grossAmount = form.grossAmount;
      }
    } else {
      payload.grossAmount = form.grossAmount;
    }

    const ok = await onSubmit(payload);
    if (ok) {
      setForm((current) => ({
        ...INITIAL_FORM,
        assetId: current.assetId,
        exchangeId: current.exchangeId,
        feeCurrency: current.feeCurrency,
        inputMode: tradeType === 'BUY' ? current.inputMode : 'COIN_AMOUNT'
      }));
      setFeeInputMode(null);
    }
  }

  return (
    <form className="trade-form" onSubmit={handleSubmit}>
      <h3>{title}</h3>

      <label htmlFor={`${title}-asset`}>Asset</label>
      <select
        id={`${title}-asset`}
        value={form.assetId}
        onChange={(event) =>
          setForm((current) => {
            const next = { ...current, assetId: event.target.value };
            return syncFeeByMode(next, feeInputMode);
          })
        }
        required
      >
        <option value="">Select asset</option>
        {assets.map((asset) => (
          <option key={asset.id} value={asset.id}>
            {asset.symbol} - {asset.name}
          </option>
        ))}
      </select>

      <label htmlFor={`${title}-exchange`}>Exchange</label>
      <select
        id={`${title}-exchange`}
        value={form.exchangeId}
        onChange={(event) => setForm((current) => ({ ...current, exchangeId: event.target.value }))}
        required
      >
        <option value="">Select exchange</option>
        {exchanges.map((exchange) => (
          <option key={exchange.id} value={exchange.id}>
            {exchange.name}
          </option>
        ))}
      </select>

      {tradeType === 'BUY' ? (
        <>
          <label htmlFor={`${title}-inputMode`}>Buy Input Mode</label>
          <select
            id={`${title}-inputMode`}
            value={form.inputMode}
            onChange={(event) => {
              const nextMode = event.target.value as BuyInputMode;
              setForm((current) => syncFeeByMode({ ...current, inputMode: nextMode }, feeInputMode));
              onBuyInputModeChange?.(nextMode);
            }}
          >
            <option value="COIN_AMOUNT">Coin Amount</option>
            <option value="USD_AMOUNT">USD Amount</option>
          </select>
        </>
      ) : null}

      {tradeType === 'BUY' && form.inputMode === 'USD_AMOUNT' ? (
        <>
          <label htmlFor={`${title}-usdAmount`}>USD Amount</label>
          <input
            id={`${title}-usdAmount`}
            type="number"
            min="0.000000000000000001"
            step="any"
            value={form.usdAmount}
            onChange={(event) =>
              setForm((current) => syncFeeByMode({ ...current, usdAmount: event.target.value }, feeInputMode))
            }
            required
          />

          <label htmlFor={`${title}-calculatedAmount`}>Coin Amount (calculated)</label>
          <input
            id={`${title}-calculatedAmount`}
            type="text"
            value={effectiveGrossAmount ? formatCalculated(effectiveGrossAmount) : ''}
            readOnly
          />
        </>
      ) : (
        <>
          <label htmlFor={`${title}-amount`}>Amount</label>
          <input
            id={`${title}-amount`}
            type="number"
            min="0.000000000000000001"
            step="any"
            value={form.grossAmount}
            onChange={(event) =>
              setForm((current) => syncFeeByMode({ ...current, grossAmount: event.target.value }, feeInputMode))
            }
            required
          />
        </>
      )}

      <label htmlFor={`${title}-unitPrice`}>Unit Price (USD)</label>
      <input
        id={`${title}-unitPrice`}
        type="number"
        min="0.000000000000000001"
        step="any"
        value={form.unitPriceUsd}
        onChange={(event) =>
          setForm((current) => syncFeeByMode({ ...current, unitPriceUsd: event.target.value }, feeInputMode))
        }
        required
      />

      <label htmlFor={`${title}-feePercentage`}>Fee Percentage (optional)</label>
      <div className="input-with-suffix">
        <input
          id={`${title}-feePercentage`}
          type="number"
          min="0"
          step="any"
          value={form.feePercentage}
          onChange={(event) => {
            setFeeInputMode('PERCENTAGE');
            setForm((current) => syncFeeByMode({ ...current, feePercentage: event.target.value }, 'PERCENTAGE'));
          }}
        />
        <span className="input-suffix">%</span>
      </div>

      <label htmlFor={`${title}-feeAmount`}>Fee Amount (optional)</label>
      <input
        id={`${title}-feeAmount`}
        type="number"
        min="0"
        step="any"
        value={form.feeAmount}
        onChange={(event) => {
          setFeeInputMode('AMOUNT');
          setForm((current) => syncFeeByMode({ ...current, feeAmount: event.target.value }, 'AMOUNT'));
        }}
      />

      <label htmlFor={`${title}-feeCurrency`}>Fee Currency (optional)</label>
      <input
        id={`${title}-feeCurrency`}
        value={form.feeCurrency}
        onChange={(event) => setForm((current) => ({ ...current, feeCurrency: event.target.value }))}
        maxLength={10}
        placeholder={selectedAssetSymbol || 'BTC'}
      />

      <button type="submit" disabled={submitting}>
        {submitting ? 'Submitting...' : `Submit ${title}`}
      </button>
    </form>
  );
}
