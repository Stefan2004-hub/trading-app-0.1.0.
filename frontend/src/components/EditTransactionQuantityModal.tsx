import { useEffect, useState } from 'react';
import type { TransactionItem, UpdateTransactionNetAmountPayload } from '../types/trading';
import { formatUsd } from '../utils/format';
import { Button } from './ui/button';
import { Dialog } from './ui/dialog';
import { Input } from './ui/input';
import { Label } from './ui/label';

interface EditTransactionQuantityModalProps {
  open: boolean;
  transaction: TransactionItem | null;
  assetLabel: string;
  onClose: () => void;
  onSubmit: (transactionId: string, payload: UpdateTransactionNetAmountPayload) => Promise<boolean>;
}

function asText(value: unknown): string {
  return value == null ? '' : String(value);
}

export function EditTransactionQuantityModal({
  open,
  transaction,
  assetLabel,
  onClose,
  onSubmit
}: EditTransactionQuantityModalProps): JSX.Element | null {
  const [netAmount, setNetAmount] = useState<string>('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open || !transaction) {
      setNetAmount('');
      return;
    }
    setNetAmount(asText(transaction.netAmount));
  }, [open, transaction]);

  if (!open || !transaction) {
    return null;
  }
  const activeTransaction = transaction;

  async function handleSave(): Promise<void> {
    const trimmedNetAmount = asText(netAmount).trim();
    if (!trimmedNetAmount) {
      return;
    }

    setSaving(true);
    const ok = await onSubmit(activeTransaction.id, {
      netAmount: trimmedNetAmount
    });
    setSaving(false);
    if (ok) {
      onClose();
    }
  }

  return (
    <Dialog open={open} title="Edit Quantity" onClose={saving ? () => undefined : onClose}>
      <div className="modal-grid">
        <p>
          <strong>Asset:</strong> {assetLabel}
        </p>
        <p>
          <strong>Purchase Price:</strong> {formatUsd(transaction.unitPriceUsd)}
        </p>

        <Label htmlFor="edit-net-amount">Net Amount</Label>
        <Input
          id="edit-net-amount"
          type="number"
          min="0"
          step="any"
          value={netAmount}
          onChange={(event) => setNetAmount(event.target.value)}
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
          onClick={() => {
            void handleSave();
          }}
          disabled={saving || !asText(netAmount).trim()}
        >
          Save Quantity
        </Button>
      </div>
    </Dialog>
  );
}
