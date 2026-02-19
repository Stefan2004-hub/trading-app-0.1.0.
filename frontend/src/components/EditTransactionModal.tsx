import { useEffect, useMemo, useState } from 'react';
import type { TransactionItem, UpdateTransactionPayload } from '../types/trading';
import { formatDateTime } from '../utils/format';
import { ConfirmDialog } from './ConfirmDialog';
import { Button } from './ui/button';
import { Dialog } from './ui/dialog';
import { Input } from './ui/input';
import { Label } from './ui/label';

type FeeMode = 'PERCENTAGE' | 'AMOUNT' | null;

interface EditTransactionModalProps {
  open: boolean;
  transaction: TransactionItem | null;
  assetLabel: string;
  exchangeLabel: string;
  onClose: () => void;
  onSubmit: (transactionId: string, payload: UpdateTransactionPayload) => Promise<boolean>;
}

interface EditFormState {
  grossAmount: string;
  feeAmount: string;
  feePercentage: string;
  unitPriceUsd: string;
}

const EMPTY_FORM: EditFormState = {
  grossAmount: '',
  feeAmount: '',
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

function toFormState(transaction: TransactionItem): EditFormState {
  const gross = Number(transaction.grossAmount);
  const feeAmountValue = transaction.feeAmount ? Number(transaction.feeAmount) : null;
  const feePercentageValue = transaction.feePercentage ? Number(transaction.feePercentage) * 100 : null;

  const computedPercent = feeAmountValue && gross > 0 ? (feeAmountValue / gross) * 100 : null;

  return {
    grossAmount: transaction.grossAmount,
    feeAmount: transaction.feeAmount ?? '',
    feePercentage: formatCalculated(feePercentageValue ?? computedPercent ?? 0),
    unitPriceUsd: transaction.unitPriceUsd
  };
}

function syncFee(form: EditFormState, mode: FeeMode): EditFormState {
  const gross = parsePositive(form.grossAmount);
  if (!mode || !gross) {
    return form;
  }

  if (mode === 'PERCENTAGE') {
    const percentage = parsePositive(form.feePercentage);
    if (!percentage) {
      return { ...form, feeAmount: '' };
    }
    return { ...form, feeAmount: formatCalculated((gross * percentage) / 100) };
  }

  const amount = parsePositive(form.feeAmount);
  if (!amount) {
    return { ...form, feePercentage: '' };
  }
  return { ...form, feePercentage: formatCalculated((amount / gross) * 100) };
}

export function EditTransactionModal({
  open,
  transaction,
  assetLabel,
  exchangeLabel,
  onClose,
  onSubmit
}: EditTransactionModalProps): JSX.Element | null {
  const [form, setForm] = useState<EditFormState>(EMPTY_FORM);
  const [feeMode, setFeeMode] = useState<FeeMode>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!transaction || !open) {
      setForm(EMPTY_FORM);
      setFeeMode(null);
      return;
    }
    setForm(toFormState(transaction));
    setFeeMode(null);
  }, [open, transaction]);

  const isUsdFee = useMemo(() => transaction?.feeCurrency?.toUpperCase() === 'USD', [transaction]);

  if (!open || !transaction) {
    return null;
  }
  const activeTransaction = transaction;

  async function handleConfirmSave(): Promise<void> {
    setSaving(true);

    const payload: UpdateTransactionPayload = {
      grossAmount: form.grossAmount,
      feeAmount: form.feeAmount.trim() ? form.feeAmount.trim() : undefined,
      feePercentage:
        !isUsdFee && form.feePercentage.trim() ? form.feePercentage.trim() : undefined,
      unitPriceUsd: form.unitPriceUsd
    };

    const ok = await onSubmit(activeTransaction.id, payload);
    setSaving(false);
    if (ok) {
      setConfirmOpen(false);
      onClose();
    }
  }

  return (
    <>
      <Dialog open={open} title="Edit Transaction" onClose={saving ? () => undefined : onClose}>
        <div className="modal-grid">
          <p>
            <strong>Asset:</strong> {assetLabel}
          </p>
          <p>
            <strong>Exchange:</strong> {exchangeLabel}
          </p>
          <p>
            <strong>Type:</strong> {activeTransaction.transactionType}
          </p>
          <p>
            <strong>Date:</strong> {formatDateTime(activeTransaction.transactionDate)}
          </p>

          <Label htmlFor="edit-gross">Gross Amount (coins)</Label>
          <Input
            id="edit-gross"
            type="number"
            step="any"
            value={form.grossAmount}
            onChange={(event) => setForm((current) => syncFee({ ...current, grossAmount: event.target.value }, feeMode))}
            required
          />

          <Label htmlFor="edit-fee-percentage">Fee Percentage</Label>
          <div className="input-with-suffix">
            <Input
              id="edit-fee-percentage"
              type="number"
              min="0"
              step="any"
              value={form.feePercentage}
              onChange={(event) => {
                setFeeMode('PERCENTAGE');
                setForm((current) => syncFee({ ...current, feePercentage: event.target.value }, 'PERCENTAGE'));
              }}
            />
            <span className="input-suffix">%</span>
          </div>

          <Label htmlFor="edit-fee-amount">
            Fee Amount{activeTransaction.feeCurrency ? ` (${activeTransaction.feeCurrency})` : ''}
          </Label>
          <Input
            id="edit-fee-amount"
            type="number"
            min="0"
            step="any"
            value={form.feeAmount}
            onChange={(event) => {
              setFeeMode('AMOUNT');
              setForm((current) => syncFee({ ...current, feeAmount: event.target.value }, 'AMOUNT'));
            }}
          />

          <Label htmlFor="edit-unit-price">Unit Price (USD)</Label>
          <Input
            id="edit-unit-price"
            type="number"
            step="any"
            value={form.unitPriceUsd}
            onChange={(event) => setForm((current) => ({ ...current, unitPriceUsd: event.target.value }))}
            required
          />
        </div>

        <div className="dialog-actions">
          <Button type="button" variant="secondary" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button
            type="button"
            variant="info"
            onClick={() => setConfirmOpen(true)}
            disabled={
              saving ||
              !form.grossAmount.trim() ||
              !form.unitPriceUsd.trim()
            }
          >
            Save
          </Button>
        </div>
      </Dialog>

      <ConfirmDialog
        open={confirmOpen}
        title="Confirm Update"
        message="Save changes to this transaction?"
        confirmText="Confirm Save"
        loading={saving}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => {
          void handleConfirmSave();
        }}
      />
    </>
  );
}
