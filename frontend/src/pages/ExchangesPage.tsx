import { FormEvent, useEffect, useState } from 'react';
import { AppHeader } from '../components/AppHeader';
import { tradingApi } from '../api/tradingApi';
import type { ExchangeOption } from '../types/trading';

interface ExchangeFormState {
  symbol: string;
  name: string;
}

const EMPTY_FORM: ExchangeFormState = { symbol: '', name: '' };

export function ExchangesPage(): JSX.Element {
  const [exchanges, setExchanges] = useState<ExchangeOption[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<ExchangeFormState>(EMPTY_FORM);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadExchanges(search);
    }, 300);
    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [search]);

  async function loadExchanges(searchTerm?: string): Promise<void> {
    setLoading(true);
    setError(null);
    try {
      setExchanges(await tradingApi.listExchanges(searchTerm));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load exchanges.');
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
        await tradingApi.updateExchange(editingId, payload);
      } else {
        await tradingApi.createExchange(payload);
      }
      setForm(EMPTY_FORM);
      setEditingId(null);
      await loadExchanges(search);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save exchange.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string): Promise<void> {
    setError(null);
    try {
      await tradingApi.deleteExchange(id);
      if (editingId === id) {
        setEditingId(null);
        setForm(EMPTY_FORM);
      }
      await loadExchanges(search);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete exchange.');
    }
  }

  function startEdit(exchange: ExchangeOption): void {
    setEditingId(exchange.id);
    setForm({ symbol: exchange.symbol, name: exchange.name });
  }

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Exchanges</h1>
        {error ? <p className="auth-error">{error}</p> : null}

        <div className="search-controls">
          <label htmlFor="exchange-search">Search</label>
          <input
            id="exchange-search"
            className="search-input"
            type="search"
            placeholder="Search by symbol or name"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        <form className="trade-form" onSubmit={handleSubmit}>
          <label htmlFor="exchange-symbol">Symbol</label>
          <input
            id="exchange-symbol"
            value={form.symbol}
            maxLength={10}
            required
            onChange={(event) => setForm((current) => ({ ...current, symbol: event.target.value }))}
          />

          <label htmlFor="exchange-name">Name</label>
          <input
            id="exchange-name"
            value={form.name}
            maxLength={50}
            required
            onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
          />

          <div className="transaction-actions">
            <button type="submit" disabled={saving}>
              {saving ? 'Saving...' : editingId ? 'Update Exchange' : 'Create Exchange'}
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
          <h2>Exchange List</h2>
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
                {exchanges.map((exchange) => (
                  <tr key={exchange.id}>
                    <td>{exchange.symbol}</td>
                    <td>{exchange.name}</td>
                    <td>
                      <div className="transaction-actions">
                        <button
                          type="button"
                          className="row-action-button row-action-edit"
                          onClick={() => startEdit(exchange)}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="row-action-button row-action-delete"
                          onClick={() => void handleDelete(exchange.id)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!loading && exchanges.length === 0 ? (
                  <tr>
                    <td colSpan={3}>No exchanges found.</td>
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
