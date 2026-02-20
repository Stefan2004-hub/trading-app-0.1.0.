import { TradeForm } from './TradeForm';
import { Button } from './ui/button';
import { Dialog } from './ui/dialog';
import type { BuyInputMode, TradeFormPayload } from '../types/trading';
import type { AssetOption, ExchangeOption } from '../types/trading';

interface BuyTransactionModalProps {
  open: boolean;
  assets: AssetOption[];
  exchanges: ExchangeOption[];
  submitting: boolean;
  defaultBuyInputMode?: BuyInputMode;
  onClose: () => void;
  onBuyInputModeChange: (mode: BuyInputMode) => void;
  onSubmit: (payload: TradeFormPayload) => Promise<boolean>;
}

export function BuyTransactionModal({
  open,
  assets,
  exchanges,
  submitting,
  defaultBuyInputMode,
  onClose,
  onBuyInputModeChange,
  onSubmit
}: BuyTransactionModalProps): JSX.Element | null {
  const formId = 'buy-transaction-form';

  if (!open) {
    return null;
  }

  return (
    <Dialog
      open={open}
      title="Buy Transaction"
      onClose={submitting ? () => undefined : onClose}
      panelClassName="buy-transaction-modal"
    >
      <div className="buy-modal-scroll">
        <TradeForm
          formId={formId}
          hideSubmitButton
          title="Buy"
          tradeType="BUY"
          assets={assets}
          exchanges={exchanges}
          submitting={submitting}
          defaultBuyInputMode={defaultBuyInputMode}
          onBuyInputModeChange={onBuyInputModeChange}
          onSubmit={async (payload) => {
            const ok = await onSubmit(payload);
            if (ok) {
              onClose();
            }
            return ok;
          }}
        />
      </div>

      <div className="buy-modal-footer">
        <Button type="submit" form={formId} variant="default" disabled={submitting}>
          {submitting ? 'Submitting...' : 'Buy'}
        </Button>
      </div>
    </Dialog>
  );
}
