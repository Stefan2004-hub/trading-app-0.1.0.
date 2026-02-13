import { FormEvent, useState } from 'react';
import type { AssetOption, ExchangeOption, TradeFormPayload } from '../types/trading';

interface TradeFormProps {
  title: string;
  assets: AssetOption[];
  exchanges: ExchangeOption[];
  submitting: boolean;
  onSubmit: (payload: TradeFormPayload) => Promise<boolean>;
}

const INITIAL_FORM: TradeFormPayload = {
  assetId: '',
  exchangeId: '',
  grossAmount: '',
  feeAmount: '',
  feeCurrency: 'USD',
  unitPriceUsd: ''
};

export function TradeForm({ title, assets, exchanges, submitting, onSubmit }: TradeFormProps): JSX.Element {
  const [form, setForm] = useState<TradeFormPayload>(INITIAL_FORM);

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    const ok = await onSubmit({
      ...form,
      feeAmount: form.feeAmount?.trim() ? form.feeAmount.trim() : undefined,
      feeCurrency: form.feeCurrency?.trim() ? form.feeCurrency.trim().toUpperCase() : undefined
    });
    if (ok) {
      setForm((current) => ({
        ...current,
        grossAmount: '',
        feeAmount: '',
        unitPriceUsd: ''
      }));
    }
  }

  return (
    <form className="trade-form" onSubmit={handleSubmit}>
      <h3>{title}</h3>

      <label htmlFor={`${title}-asset`}>Asset</label>
      <select
        id={`${title}-asset`}
        value={form.assetId}
        onChange={(event) => setForm((current) => ({ ...current, assetId: event.target.value }))}
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

      <label htmlFor={`${title}-amount`}>Amount</label>
      <input
        id={`${title}-amount`}
        type="number"
        min="0.000000000000000001"
        step="any"
        value={form.grossAmount}
        onChange={(event) => setForm((current) => ({ ...current, grossAmount: event.target.value }))}
        required
      />

      <label htmlFor={`${title}-unitPrice`}>Unit Price (USD)</label>
      <input
        id={`${title}-unitPrice`}
        type="number"
        min="0.000000000000000001"
        step="any"
        value={form.unitPriceUsd}
        onChange={(event) => setForm((current) => ({ ...current, unitPriceUsd: event.target.value }))}
        required
      />

      <label htmlFor={`${title}-feeAmount`}>Fee Amount (optional)</label>
      <input
        id={`${title}-feeAmount`}
        type="number"
        min="0"
        step="any"
        value={form.feeAmount ?? ''}
        onChange={(event) => setForm((current) => ({ ...current, feeAmount: event.target.value }))}
      />

      <label htmlFor={`${title}-feeCurrency`}>Fee Currency (optional)</label>
      <input
        id={`${title}-feeCurrency`}
        value={form.feeCurrency ?? ''}
        onChange={(event) => setForm((current) => ({ ...current, feeCurrency: event.target.value }))}
        maxLength={10}
      />

      <button type="submit" disabled={submitting}>
        {submitting ? 'Submitting...' : `Submit ${title}`}
      </button>
    </form>
  );
}
