import { useEffect, useMemo, useState } from 'react';
import type { TradeFormPayload, TransactionItem } from '../types/trading';
import { Button } from './ui/button';
import { Dialog } from './ui/dialog';
import { Input } from './ui/input';
import { Label } from './ui/label';

interface SellTransactionModalProps {
  open: boolean;
  transaction: TransactionItem | null;
  assetLabel: string;
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

function asText(value: unknown): string {
  return value == null ? '' : String(value);
}

function toInitialState(transaction: TransactionItem): SellFormState {
  const sellQuantity = Number(transaction.netAmount);
  const unitPrice = Number(transaction.unitPriceUsd ?? '0');
  const feeAmount = transaction.feeAmount ? Number(transaction.feeAmount) : null;
  const feePercentage = transaction.feePercentage ? Number(transaction.feePercentage) * 100 : null;
  const notionalUsd = sellQuantity > 0 && unitPrice > 0 ? sellQuantity * unitPrice : 0;

  const fallbackPercentage = feeAmount && notionalUsd > 0 ? (feeAmount / notionalUsd) * 100 : null;

  return {
    feeAmountUsd: transaction.feeAmount ?? '',
    feePercentage: formatCalculated(feePercentage ?? fallbackPercentage ?? 0),
    unitPriceUsd: asText(transaction.unitPriceUsd)
  };
}

function syncFee(form: SellFormState, grossAmount: number, mode: 'PERCENTAGE' | 'AMOUNT'): SellFormState {
  const unitPriceUsd = parsePositive(form.unitPriceUsd);
  const notionalUsd = unitPriceUsd ? grossAmount * unitPriceUsd : null;

  if (!mode || !Number.isFinite(grossAmount) || grossAmount <= 0 || !notionalUsd || notionalUsd <= 0) {
    return form;
  }

  if (mode === 'PERCENTAGE') {
    const percentage = parsePositive(form.feePercentage);
    if (!percentage) {
      return { ...form, feeAmountUsd: '' };
    }
    return { ...form, feeAmountUsd: formatCalculated((notionalUsd * percentage) / 100) };
  }

  const amount = parsePositive(form.feeAmountUsd);
  if (!amount) {
    return { ...form, feePercentage: '' };
  }
  return { ...form, feePercentage: formatCalculated((amount / notionalUsd) * 100) };
}

export function SellTransactionModal({
  open,
  transaction,
  assetLabel,
  exchangeLabel,
  onClose,
  onSubmit
}: SellTransactionModalProps): JSX.Element | null {
  const [form, setForm] = useState<SellFormState>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!transaction || !open) {
      setForm(EMPTY_FORM);
      return;
    }
    setForm(toInitialState(transaction));
  }, [open, transaction]);

  const sellQuantity = useMemo(() => Number(transaction?.netAmount ?? '0'), [transaction]);

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

        <Label htmlFor="sell-unit-price">Unit Price (USD)</Label>
        <Input
          id="sell-unit-price"
          type="number"
          min="0.000000000000000001"
          step="any"
          value={asText(form.unitPriceUsd)}
          onChange={(event) =>
            setForm((current) => {
              const next = { ...current, unitPriceUsd: event.target.value };
              if (next.feePercentage.trim()) {
                return syncFee(next, sellQuantity, 'PERCENTAGE');
              }
              if (next.feeAmountUsd.trim()) {
                return syncFee(next, sellQuantity, 'AMOUNT');
              }
              return next;
            })
          }
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
