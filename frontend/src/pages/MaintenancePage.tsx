import { useState } from 'react';
import { tradingApi } from '../api/tradingApi';
import { AppHeader } from '../components/AppHeader';

export function MaintenancePage(): JSX.Element {
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  async function handleDownloadSqlBackup(): Promise<void> {
    setDownloading(true);
    setError(null);
    setInfo(null);

    try {
      const { blob, fileName } = await tradingApi.downloadSqlBackup();
      const objectUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(objectUrl);
      setInfo(`Backup downloaded: ${fileName}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to download SQL backup.');
    } finally {
      setDownloading(false);
    }
  }

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Maintenance</h1>
        <p>Download a SQL backup script for your account data.</p>

        {error ? <p className="auth-error">{error}</p> : null}
        {info ? <p className="field-help">{info}</p> : null}

        <div className="maintenance-actions">
          <button
            type="button"
            className="secondary"
            onClick={() => void handleDownloadSqlBackup()}
            disabled={downloading}
          >
            {downloading ? 'Downloading...' : 'Download SQL Backup'}
          </button>
        </div>
      </section>
    </main>
  );
}
