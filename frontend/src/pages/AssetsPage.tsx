import { FormEvent, useEffect, useState } from 'react';
import { AppHeader } from '../components/AppHeader';
import { tradingApi } from '../api/tradingApi';
import type { AssetOption } from '../types/trading';

interface AssetFormState {
  symbol: string;
  name: string;
}

const EMPTY_FORM: AssetFormState = { symbol: '', name: '' };

export function AssetsPage(): JSX.Element {
  const [assets, setAssets] = useState<AssetOption[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<AssetFormState>(EMPTY_FORM);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadAssets(search);
    }, 300);
    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [search]);

  async function loadAssets(searchTerm?: string): Promise<void> {
    setLoading(true);
    setError(null);
    try {
      setAssets(await tradingApi.listAssets(searchTerm));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load assets.');
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setSaving(true);
    setError(null);
    const payload = { symbol: form.symbol.trim().toUpperCase(), name: form.name.trim() };

    try {
      if (editingId) {
        await tradingApi.updateAsset(editingId, payload);
      } else {
        await tradingApi.createAsset(payload);
      }
      setForm(EMPTY_FORM);
      setEditingId(null);
      await loadAssets(search);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save asset.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string): Promise<void> {
    setError(null);
    try {
      await tradingApi.deleteAsset(id);
      if (editingId === id) {
        setEditingId(null);
        setForm(EMPTY_FORM);
      }
      await loadAssets(search);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete asset.');
    }
  }

  function startEdit(asset: AssetOption): void {
    setEditingId(asset.id);
    setForm({ symbol: asset.symbol, name: asset.name });
  }

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Assets</h1>
        {error ? <p className="auth-error">{error}</p> : null}

        <div className="search-controls">
          <label htmlFor="asset-search">Search</label>
          <input
            id="asset-search"
            className="search-input"
            type="search"
            placeholder="Search by symbol or name"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        <form className="trade-form" onSubmit={handleSubmit}>
          <label htmlFor="asset-symbol">Symbol</label>
          <input
            id="asset-symbol"
            value={form.symbol}
            maxLength={10}
            required
            onChange={(event) => setForm((current) => ({ ...current, symbol: event.target.value }))}
          />

          <label htmlFor="asset-name">Name</label>
          <input
            id="asset-name"
            value={form.name}
            maxLength={50}
            required
            onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
          />

          <div className="transaction-actions">
            <button type="submit" disabled={saving}>
              {saving ? 'Saving...' : editingId ? 'Update Asset' : 'Create Asset'}
            </button>
            {editingId ? (
              <button
                type="button"
                className="secondary"
                onClick={() => {
                  setEditingId(null);
                  setForm(EMPTY_FORM);
                }}
              >
                Cancel Edit
              </button>
            ) : null}
          </div>
        </form>

        <section className="history-panel">
          <h2>Asset List</h2>
          {loading ? <p>Loading...</p> : null}
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Symbol</th>
                  <th>Name</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {assets.map((asset) => (
                  <tr key={asset.id}>
                    <td>{asset.symbol}</td>
                    <td>{asset.name}</td>
                    <td>
                      <div className="transaction-actions">
                        <button type="button" className="row-action-button row-action-edit" onClick={() => startEdit(asset)}>
                          Edit
                        </button>
                        <button type="button" className="row-action-button row-action-delete" onClick={() => void handleDelete(asset.id)}>
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!loading && assets.length === 0 ? (
                  <tr>
                    <td colSpan={3}>No assets found.</td>
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
