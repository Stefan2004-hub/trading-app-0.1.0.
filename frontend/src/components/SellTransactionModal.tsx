import { useEffect, useMemo, useRef, useState } from 'react';
import type { TradeFormPayload, TransactionItem } from '../types/trading';
import {
  decimalToDisplay,
  divideDecimal,
  fractionalToPercent,
  isPositiveDecimal,
  multiplyDecimal
} from '../utils/decimal';
import { useAssetSpotPrices } from '../hooks/useAssetSpotPrices';
import { Button } from './ui/button';
import { Dialog } from './ui/dialog';
import { Input } from './ui/input';
import { Label } from './ui/label';

interface SellTransactionModalProps {
  open: boolean;
  transaction: TransactionItem | null;
  assetLabel: string;
  assetSymbol: string;
  exchangeLabel: string;
  onClose: () => void;
  onSubmit: (payload: TradeFormPayload) => Promise<boolean>;
}

interface SellFormState {
  feeAmountUsd: string;
  feePercentage: string;
  unitPriceUsd: string;
}

const EMPTY_FORM: SellFormState = {
  feeAmountUsd: '',
  feePercentage: '',
  unitPriceUsd: ''
};

function asText(value: unknown): string {
  return value == null ? '' : String(value);
}

function toInitialState(transaction: TransactionItem): SellFormState {
  const notionalUsd = multiplyDecimal(transaction.netAmount, transaction.unitPriceUsd);
  const feePercentage = transaction.feePercentage ? fractionalToPercent(transaction.feePercentage) : null;
  const fallbackRatio =
    transaction.feeAmount && notionalUsd && isPositiveDecimal(notionalUsd)
      ? divideDecimal(transaction.feeAmount, notionalUsd, 18)
      : null;
  const fallbackPercentage = fallbackRatio ? multiplyDecimal(fallbackRatio, '100') : null;

  return {
    feeAmountUsd: transaction.feeAmount ?? '',
    feePercentage: decimalToDisplay(feePercentage ?? fallbackPercentage ?? '0', 18) ?? '',
    unitPriceUsd: asText(transaction.unitPriceUsd)
  };
}

function syncFee(form: SellFormState, grossAmount: string, mode: 'PERCENTAGE' | 'AMOUNT'): SellFormState {
  const notionalUsd =
    isPositiveDecimal(grossAmount) && isPositiveDecimal(form.unitPriceUsd)
      ? multiplyDecimal(grossAmount, form.unitPriceUsd)
      : null;

  if (!mode || !notionalUsd || !isPositiveDecimal(notionalUsd)) {
    return form;
  }

  if (mode === 'PERCENTAGE') {
    if (!isPositiveDecimal(form.feePercentage)) {
      return { ...form, feeAmountUsd: '' };
    }
    const feeProduct = multiplyDecimal(notionalUsd, form.feePercentage);
    const feeAmount = feeProduct ? divideDecimal(feeProduct, '100', 18) : null;
    return { ...form, feeAmountUsd: feeAmount ?? '' };
  }

  if (!isPositiveDecimal(form.feeAmountUsd)) {
    return { ...form, feePercentage: '' };
  }
  const ratio = divideDecimal(form.feeAmountUsd, notionalUsd, 18);
  const feePercentage = ratio ? multiplyDecimal(ratio, '100') : null;
  return { ...form, feePercentage: feePercentage ?? '' };
}

export function SellTransactionModal({
  open,
  transaction,
  assetLabel,
  assetSymbol,
  exchangeLabel,
  onClose,
  onSubmit
}: SellTransactionModalProps): JSX.Element | null {
  const [form, setForm] = useState<SellFormState>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const unitPriceManuallyEditedRef = useRef(false);

  const normalizedAssetSymbol = assetSymbol.trim().toUpperCase();
  const priceStatesBySymbol = useAssetSpotPrices(normalizedAssetSymbol ? [normalizedAssetSymbol] : []);
  const selectedAssetPriceState = normalizedAssetSymbol ? priceStatesBySymbol[normalizedAssetSymbol] : undefined;
  const isPriceLoading = Boolean(normalizedAssetSymbol) && selectedAssetPriceState?.status === 'loading';

  useEffect(() => {
    if (!transaction || !open) {
      unitPriceManuallyEditedRef.current = false;
      setForm(EMPTY_FORM);
      return;
    }
    unitPriceManuallyEditedRef.current = false;
    setForm(toInitialState(transaction));
  }, [open, transaction]);

  useEffect(() => {
    if (!open || !transaction) {
      return;
    }
    if (!normalizedAssetSymbol || selectedAssetPriceState?.status !== 'success' || !selectedAssetPriceState.priceUsd) {
      return;
    }
    if (unitPriceManuallyEditedRef.current) {
      return;
    }

    setForm((current) => {
      const next = { ...current, unitPriceUsd: selectedAssetPriceState.priceUsd ?? '' };
      if (next.feePercentage.trim()) {
        return syncFee(next, transaction.netAmount, 'PERCENTAGE');
      }
      if (next.feeAmountUsd.trim()) {
        return syncFee(next, transaction.netAmount, 'AMOUNT');
      }
      return next;
    });
  }, [
    normalizedAssetSymbol,
    open,
    selectedAssetPriceState?.priceUsd,
    selectedAssetPriceState?.status,
    transaction
  ]);

  const sellQuantity = useMemo(() => asText(transaction?.netAmount ?? ''), [transaction]);

  if (!open || !transaction) {
    return null;
  }
  const activeTransaction = transaction;

  async function handleSubmit(): Promise<void> {
    setSaving(true);
    const unitPriceUsd = asText(form.unitPriceUsd).trim();
    const payload: TradeFormPayload = {
      assetId: activeTransaction.assetId,
      exchangeId: activeTransaction.exchangeId,
      grossAmount: activeTransaction.netAmount,
      feeAmount: form.feeAmountUsd.trim() ? form.feeAmountUsd.trim() : undefined,
      feePercentage: form.feePercentage.trim() ? form.feePercentage.trim() : undefined,
      feeCurrency: 'USD',
      unitPriceUsd
    };

    const ok = await onSubmit(payload);
    setSaving(false);
    if (ok) {
      onClose();
    }
  }

  return (
    <Dialog open={open} title="Sell Transaction" onClose={saving ? () => undefined : onClose}>
      <div className="modal-grid">
        <p>
          <strong>Asset:</strong> {assetLabel}
        </p>
        <p>
          <strong>Exchange:</strong> {exchangeLabel}
        </p>
        <p>
          <strong>Amount:</strong> {activeTransaction.netAmount} coins (fixed)
        </p>

        <Label htmlFor="sell-fee-percentage">Fee Percentage</Label>
        <div className="input-with-suffix">
          <Input
            id="sell-fee-percentage"
            type="number"
            min="0"
            step="any"
            value={form.feePercentage}
            onChange={(event) => {
              setForm((current) => syncFee({ ...current, feePercentage: event.target.value }, sellQuantity, 'PERCENTAGE'));
            }}
          />
          <span className="input-suffix">%</span>
        </div>

        <Label htmlFor="sell-fee-amount">Fee Amount (USD)</Label>
        <Input
          id="sell-fee-amount"
          type="number"
          min="0"
          step="any"
            value={form.feeAmountUsd}
            onChange={(event) => {
              setForm((current) => syncFee({ ...current, feeAmountUsd: event.target.value }, sellQuantity, 'AMOUNT'));
            }}
          />

        <Label htmlFor="sell-unit-price">Unit Price (USD){isPriceLoading ? ' (Loading market price...)' : ''}</Label>
        <Input
          id="sell-unit-price"
          type="number"
          min="0.000000000000000001"
          step="any"
          value={asText(form.unitPriceUsd)}
          placeholder={isPriceLoading ? 'Fetching current market price...' : undefined}
          aria-busy={isPriceLoading}
          onChange={(event) => {
            unitPriceManuallyEditedRef.current = true;
            setForm((current) => {
              const next = { ...current, unitPriceUsd: event.target.value };
              if (next.feePercentage.trim()) {
                return syncFee(next, sellQuantity, 'PERCENTAGE');
              }
              if (next.feeAmountUsd.trim()) {
                return syncFee(next, sellQuantity, 'AMOUNT');
              }
              return next;
            });
          }}
          required
        />
      </div>

      <div className="dialog-actions">
        <Button type="button" variant="secondary" onClick={onClose} disabled={saving}>
          Cancel
        </Button>
        <Button
          type="button"
          variant="success"
          onClick={() => {
            void handleSubmit();
          }}
          disabled={saving || !asText(form.unitPriceUsd).trim()}
        >
          {saving ? <span className="button-spinner" aria-hidden="true" /> : null}
          {saving ? 'Selling...' : 'Sell'}
        </Button>
      </div>
    </Dialog>
  );
}
