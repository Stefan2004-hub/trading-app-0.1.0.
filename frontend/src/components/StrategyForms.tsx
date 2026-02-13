import { FormEvent, useState } from 'react';
import type { AssetOption } from '../types/trading';
import type { UpsertBuyStrategyPayload, UpsertSellStrategyPayload } from '../types/strategy';

interface StrategyFormsProps {
  assets: AssetOption[];
  submitting: boolean;
  onSubmitSell: (payload: UpsertSellStrategyPayload) => Promise<boolean>;
  onSubmitBuy: (payload: UpsertBuyStrategyPayload) => Promise<boolean>;
}

export function StrategyForms({ assets, submitting, onSubmitSell, onSubmitBuy }: StrategyFormsProps): JSX.Element {
  const [sellAssetId, setSellAssetId] = useState('');
  const [sellThresholdPercent, setSellThresholdPercent] = useState('');
  const [sellActive, setSellActive] = useState(true);

  const [buyAssetId, setBuyAssetId] = useState('');
  const [dipThresholdPercent, setDipThresholdPercent] = useState('');
  const [buyAmountUsd, setBuyAmountUsd] = useState('');
  const [buyActive, setBuyActive] = useState(true);

  async function submitSell(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    const ok = await onSubmitSell({
      assetId: sellAssetId,
      thresholdPercent: sellThresholdPercent,
      active: sellActive
    });
    if (ok) {
      setSellThresholdPercent('');
    }
  }

  async function submitBuy(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    const ok = await onSubmitBuy({
      assetId: buyAssetId,
      dipThresholdPercent,
      buyAmountUsd,
      active: buyActive
    });
    if (ok) {
      setDipThresholdPercent('');
      setBuyAmountUsd('');
    }
  }

  return (
    <section className="forms-grid">
      <form className="trade-form" onSubmit={submitSell}>
        <h3>Sell Strategy</h3>

        <label htmlFor="sell-asset">Asset</label>
        <select id="sell-asset" value={sellAssetId} onChange={(event) => setSellAssetId(event.target.value)} required>
          <option value="">Select asset</option>
          {assets.map((asset) => (
            <option key={asset.id} value={asset.id}>
              {asset.symbol} - {asset.name}
            </option>
          ))}
        </select>

        <label htmlFor="sell-threshold">Threshold Percent</label>
        <input
          id="sell-threshold"
          type="number"
          min="0.01"
          step="any"
          value={sellThresholdPercent}
          onChange={(event) => setSellThresholdPercent(event.target.value)}
          required
        />

        <label className="checkbox-row" htmlFor="sell-active">
          <input
            id="sell-active"
            type="checkbox"
            checked={sellActive}
            onChange={(event) => setSellActive(event.target.checked)}
          />
          Active
        </label>

        <button type="submit" disabled={submitting}>
          {submitting ? 'Saving...' : 'Save Sell Strategy'}
        </button>
      </form>

      <form className="trade-form" onSubmit={submitBuy}>
        <h3>Buy Strategy</h3>

        <label htmlFor="buy-asset">Asset</label>
        <select id="buy-asset" value={buyAssetId} onChange={(event) => setBuyAssetId(event.target.value)} required>
          <option value="">Select asset</option>
          {assets.map((asset) => (
            <option key={asset.id} value={asset.id}>
              {asset.symbol} - {asset.name}
            </option>
          ))}
        </select>

        <label htmlFor="buy-dip-threshold">Dip Threshold Percent</label>
        <input
          id="buy-dip-threshold"
          type="number"
          min="0.01"
          step="any"
          value={dipThresholdPercent}
          onChange={(event) => setDipThresholdPercent(event.target.value)}
          required
        />

        <label htmlFor="buy-amount-usd">Buy Amount USD</label>
        <input
          id="buy-amount-usd"
          type="number"
          min="0.01"
          step="any"
          value={buyAmountUsd}
          onChange={(event) => setBuyAmountUsd(event.target.value)}
          required
        />

        <label className="checkbox-row" htmlFor="buy-active">
          <input
            id="buy-active"
            type="checkbox"
            checked={buyActive}
            onChange={(event) => setBuyActive(event.target.checked)}
          />
          Active
        </label>

        <button type="submit" disabled={submitting}>
          {submitting ? 'Saving...' : 'Save Buy Strategy'}
        </button>
      </form>
    </section>
  );
}
