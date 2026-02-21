import { FormEvent, useEffect, useMemo, useState } from 'react';
import { decimalToDisplay, divideDecimal, isPositiveDecimal, multiplyDecimal } from '../utils/decimal';
import type { BuyInputMode, TradeFormPayload, TransactionType } from '../types/trading';
import type { AssetOption, ExchangeOption } from '../types/trading';
import { tradingApi } from '../api/tradingApi';
import { SearchableLookupField } from './SearchableLookupField';

type FeeInputMode = 'PERCENTAGE' | 'AMOUNT' | null;

interface TradeFormProps {
  title: string;
  tradeType: TransactionType;
  assets: AssetOption[];
  exchanges: ExchangeOption[];
  submitting: boolean;
  formId?: string;
  className?: string;
  hideSubmitButton?: boolean;
  submitLabel?: string;
  defaultBuyInputMode?: BuyInputMode;
  initialAssetId?: string;
  initialExchangeId?: string;
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

export function TradeForm({
  title,
  tradeType,
  assets,
  exchanges,
  submitting,
  formId,
  className,
  hideSubmitButton = false,
  submitLabel,
  defaultBuyInputMode,
  initialAssetId,
  initialExchangeId,
  onBuyInputModeChange,
  onSubmit
}: TradeFormProps): JSX.Element {
  const [form, setForm] = useState<LocalTradeFormState>(INITIAL_FORM);
  const [feeInputMode, setFeeInputMode] = useState<FeeInputMode>(null);
  const [selectedAsset, setSelectedAsset] = useState<AssetOption | null>(null);
  const [selectedExchange, setSelectedExchange] = useState<ExchangeOption | null>(null);

  useEffect(() => {
    if (tradeType === 'BUY' && defaultBuyInputMode && defaultBuyInputMode !== form.inputMode) {
      setForm((current) => ({ ...current, inputMode: defaultBuyInputMode }));
    }
  }, [defaultBuyInputMode, form.inputMode, tradeType]);

  useEffect(() => {
    if (!initialAssetId) {
      return;
    }
    const asset = assets.find((row) => row.id === initialAssetId) ?? null;
    setSelectedAsset(asset);
    setForm((current) =>
      current.assetId === initialAssetId ? current : { ...current, assetId: initialAssetId }
    );
  }, [assets, initialAssetId]);

  useEffect(() => {
    if (!initialExchangeId) {
      return;
    }
    const exchange = exchanges.find((row) => row.id === initialExchangeId) ?? null;
    setSelectedExchange(exchange);
    setForm((current) =>
      current.exchangeId === initialExchangeId ? current : { ...current, exchangeId: initialExchangeId }
    );
  }, [exchanges, initialExchangeId]);

  useEffect(() => {
    if (form.assetId && (!selectedAsset || selectedAsset.id !== form.assetId)) {
      const asset = assets.find((row) => row.id === form.assetId) ?? null;
      if (asset) {
        setSelectedAsset(asset);
      }
    }
  }, [assets, form.assetId, selectedAsset]);

  useEffect(() => {
    if (form.exchangeId && (!selectedExchange || selectedExchange.id !== form.exchangeId)) {
      const exchange = exchanges.find((row) => row.id === form.exchangeId) ?? null;
      if (exchange) {
        setSelectedExchange(exchange);
      }
    }
  }, [exchanges, form.exchangeId, selectedExchange]);

  const selectedAssetSymbol = useMemo(
    () => selectedAsset?.symbol ?? assets.find((asset) => asset.id === form.assetId)?.symbol ?? '',
    [assets, form.assetId, selectedAsset]
  );

  const effectiveGrossAmount = useMemo(() => {
    if (tradeType === 'BUY' && form.inputMode === 'USD_AMOUNT') {
      if (!isPositiveDecimal(form.usdAmount) || !isPositiveDecimal(form.unitPriceUsd)) {
        return null;
      }
      return divideDecimal(form.usdAmount, form.unitPriceUsd, 18);
    }
    return isPositiveDecimal(form.grossAmount) ? form.grossAmount.trim() : null;
  }, [form.grossAmount, form.inputMode, form.unitPriceUsd, form.usdAmount, tradeType]);

  function effectiveGrossFrom(nextForm: LocalTradeFormState): string | null {
    if (tradeType === 'BUY' && nextForm.inputMode === 'USD_AMOUNT') {
      if (!isPositiveDecimal(nextForm.usdAmount) || !isPositiveDecimal(nextForm.unitPriceUsd)) {
        return null;
      }
      return divideDecimal(nextForm.usdAmount, nextForm.unitPriceUsd, 18);
    }
    return isPositiveDecimal(nextForm.grossAmount) ? nextForm.grossAmount.trim() : null;
  }

  function syncFeeByMode(nextForm: LocalTradeFormState, mode: FeeInputMode): LocalTradeFormState {
    const grossAmount = effectiveGrossFrom(nextForm);
    const feeBaseAmount =
      tradeType === 'SELL' && grossAmount && isPositiveDecimal(nextForm.unitPriceUsd)
        ? multiplyDecimal(grossAmount, nextForm.unitPriceUsd)
        : grossAmount;
    const nextAssetSymbol = assets.find((asset) => asset.id === nextForm.assetId)?.symbol ?? '';
    const feeCurrency = nextForm.feeCurrency || (tradeType === 'SELL' ? 'USD' : nextAssetSymbol);
    if (!mode || !feeBaseAmount) {
      return nextForm;
    }

    if (mode === 'PERCENTAGE') {
      if (!isPositiveDecimal(nextForm.feePercentage)) {
        return { ...nextForm, feeAmount: '' };
      }
      const feeProduct = multiplyDecimal(feeBaseAmount, nextForm.feePercentage);
      const feeAmount = feeProduct ? divideDecimal(feeProduct, '100', 18) : null;
      return {
        ...nextForm,
        feeAmount: feeAmount ?? '',
        feeCurrency
      };
    }

    if (!isPositiveDecimal(nextForm.feeAmount)) {
      return { ...nextForm, feePercentage: '' };
    }
    const ratio = divideDecimal(nextForm.feeAmount, feeBaseAmount, 18);
    const feePercentage = ratio ? multiplyDecimal(ratio, '100') : null;
    return {
      ...nextForm,
      feePercentage: feePercentage ?? '',
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
    <form id={formId} className={`trade-form${className ? ` ${className}` : ''}`} onSubmit={handleSubmit}>
      <h3>{title}</h3>

      <SearchableLookupField
        id={`${title}-asset`}
        label="Asset"
        placeholder="Search assets by symbol or name"
        required
        value={selectedAsset}
        onSelect={(option) => {
          const asset = option as AssetOption | null;
          setSelectedAsset(asset);
          setForm((current) => {
            const next = { ...current, assetId: asset?.id ?? '' };
            return syncFeeByMode(next, feeInputMode);
          });
        }}
        onSearch={(search) => tradingApi.listAssets(search)}
        quickAddLabel="Add New Asset"
        onQuickAdd={async (search) => {
          const normalized = search.trim();
          const symbol = normalized.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 10);
          return tradingApi.createAsset({
            symbol: symbol || normalized.toUpperCase().slice(0, 10),
            name: normalized
          });
        }}
      />

      <SearchableLookupField
        id={`${title}-exchange`}
        label="Exchange"
        placeholder="Search exchanges by symbol or name"
        required
        value={selectedExchange}
        onSelect={(option) => {
          const exchange = option as ExchangeOption | null;
          setSelectedExchange(exchange);
          setForm((current) => ({ ...current, exchangeId: exchange?.id ?? '' }));
        }}
        onSearch={(search) => tradingApi.listExchanges(search)}
        quickAddLabel="Add New Exchange"
        onQuickAdd={async (search) => {
          const normalized = search.trim();
          const symbol = normalized.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 10);
          return tradingApi.createExchange({
            symbol: symbol || normalized.toUpperCase().slice(0, 10),
            name: normalized
          });
        }}
      />

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
            value={effectiveGrossAmount ? decimalToDisplay(effectiveGrossAmount, 18) ?? '' : ''}
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

      {!hideSubmitButton ? (
        <button type="submit" disabled={submitting}>
          {submitting ? 'Submitting...' : submitLabel ?? `Submit ${title}`}
        </button>
      ) : null}
    </form>
  );
}
