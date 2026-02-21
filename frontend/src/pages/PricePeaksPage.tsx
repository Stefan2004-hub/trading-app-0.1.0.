import { FormEvent, useEffect, useMemo, useState } from 'react';
import { AppHeader } from '../components/AppHeader';
import { tradingApi } from '../api/tradingApi';
import type { PricePeakItem } from '../types/trading';
import { formatDateTime, formatUsd } from '../utils/format';

interface PricePeakFormState {
  peakPrice: string;
  peakTimestamp: string;
  active: boolean;
}

const EMPTY_FORM: PricePeakFormState = {
  peakPrice: '',
  peakTimestamp: '',
  active: true
};

function toDateTimeLocalValue(value: string | null | undefined): string {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const pad = (num: number): string => String(num).padStart(2, '0');
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hour = pad(date.getHours());
  const minute = pad(date.getMinutes());
  return `${year}-${month}-${day}T${hour}:${minute}`;
}

function toIsoFromLocal(localValue: string): string | null {
  if (!localValue.trim()) {
    return null;
  }
  const date = new Date(localValue);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  return date.toISOString();
}

export function PricePeaksPage(): JSX.Element {
  const [rows, setRows] = useState<PricePeakItem[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<PricePeakFormState>(EMPTY_FORM);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadRows(search);
    }, 300);
    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [search]);

  const editingRow = useMemo(() => rows.find((row) => row.id === editingId) ?? null, [editingId, rows]);

  async function loadRows(searchTerm?: string): Promise<void> {
    setLoading(true);
    setError(null);
    try {
      setRows(await tradingApi.listPricePeaks(searchTerm));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load price peaks.');
    } finally {
      setLoading(false);
    }
  }

  function startEdit(row: PricePeakItem): void {
    setEditingId(row.id);
    setForm({
      peakPrice: row.peakPrice,
      peakTimestamp: toDateTimeLocalValue(row.peakTimestamp),
      active: row.active
    });
  }

  function clearEdit(): void {
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    if (!editingId) {
      return;
    }
    const peakTimestamp = toIsoFromLocal(form.peakTimestamp);
    if (!peakTimestamp) {
      setError('Peak timestamp is invalid.');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      await tradingApi.updatePricePeak(editingId, {
        peakPrice: form.peakPrice.trim(),
        peakTimestamp,
        active: form.active
      });
      clearEdit();
      await loadRows(search);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update price peak.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string): Promise<void> {
    setError(null);
    try {
      await tradingApi.deletePricePeak(id);
      if (editingId === id) {
        clearEdit();
      }
      await loadRows(search);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete price peak.');
    }
  }

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Price Peaks</h1>
        <p className="field-help">New BUY transactions may reset peak values automatically.</p>
        {error ? <p className="auth-error">{error}</p> : null}

        <div className="search-controls">
          <label htmlFor="price-peaks-search">Search</label>
          <input
            id="price-peaks-search"
            className="search-input"
            type="search"
            placeholder="Search by asset symbol or name"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        <form className="trade-form" onSubmit={handleSubmit}>
          <h3>{editingId ? 'Edit Price Peak' : 'Select a row to edit'}</h3>
          <label htmlFor="price-peak-price">Peak Price (USD)</label>
          <input
            id="price-peak-price"
            type="number"
            min="0.00000001"
            step="any"
            value={form.peakPrice}
            disabled={!editingId}
            required={Boolean(editingId)}
            onChange={(event) => setForm((current) => ({ ...current, peakPrice: event.target.value }))}
          />

          <label htmlFor="price-peak-time">Peak Timestamp</label>
          <input
            id="price-peak-time"
            type="datetime-local"
            value={form.peakTimestamp}
            disabled={!editingId}
            required={Boolean(editingId)}
            onChange={(event) => setForm((current) => ({ ...current, peakTimestamp: event.target.value }))}
          />

          <label className="checkbox-row" htmlFor="price-peak-active">
            <input
              id="price-peak-active"
              type="checkbox"
              checked={form.active}
              disabled={!editingId}
              onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))}
            />
            Active
          </label>

          <div className="transaction-actions">
            <button type="submit" disabled={saving || !editingId}>
              {saving ? 'Saving...' : 'Update Price Peak'}
            </button>
            {editingId ? (
              <button type="button" className="secondary" onClick={clearEdit}>
                Cancel Edit
              </button>
            ) : null}
          </div>
          {editingRow ? (
            <p className="field-help">
              Editing: <strong>{editingRow.assetSymbol}</strong> ({editingRow.assetName})
            </p>
          ) : null}
        </form>

        <section className="history-panel">
          <h2>Price Peaks</h2>
          {loading ? <p>Loading...</p> : null}
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Asset</th>
                  <th>Peak Price</th>
                  <th>Peak Timestamp</th>
                  <th>Active</th>
                  <th>Last Buy Tx</th>
                  <th>Updated</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.assetSymbol} - {row.assetName}</td>
                    <td>{formatUsd(row.peakPrice)}</td>
                    <td>{formatDateTime(row.peakTimestamp)}</td>
                    <td>{row.active ? 'Yes' : 'No'}</td>
                    <td>{row.lastBuyTransactionId ?? '-'}</td>
                    <td>{formatDateTime(row.updatedAt)}</td>
                    <td>
                      <div className="transaction-actions">
                        <button
                          type="button"
                          className="row-action-button row-action-edit"
                          onClick={() => startEdit(row)}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="row-action-button row-action-delete"
                          onClick={() => void handleDelete(row.id)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!loading && rows.length === 0 ? (
                  <tr>
                    <td colSpan={7}>No price peaks found.</td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>
      </section>
    </main>
  );
}
