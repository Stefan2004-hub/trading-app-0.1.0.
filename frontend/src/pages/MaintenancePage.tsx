import { useEffect, useState } from 'react';
import { tradingApi } from '../api/tradingApi';
import { AppHeader } from '../components/AppHeader';

export function MaintenancePage(): JSX.Element {
  const [downloading, setDownloading] = useState(false);
  const [loadingHeartbeat, setLoadingHeartbeat] = useState(true);
  const [togglingHeartbeat, setTogglingHeartbeat] = useState(false);
  const [keepAliveActive, setKeepAliveActive] = useState<boolean | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    async function loadHeartbeatStatus(): Promise<void> {
      setLoadingHeartbeat(true);
      try {
        const status = await tradingApi.getKeepAliveStatus();
        if (mounted) {
          setKeepAliveActive(status.keepAliveActive);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Failed to load heartbeat status.');
        }
      } finally {
        if (mounted) {
          setLoadingHeartbeat(false);
        }
      }
    }

    void loadHeartbeatStatus();

    return () => {
      mounted = false;
    };
  }, []);

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

  async function handleToggleHeartbeat(): Promise<void> {
    setTogglingHeartbeat(true);
    setError(null);
    setInfo(null);

    try {
      const status = await tradingApi.toggleKeepAlive();
      setKeepAliveActive(status.keepAliveActive);
      setInfo(status.keepAliveActive ? 'Heartbeat enabled.' : 'Heartbeat disabled.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to toggle heartbeat.');
    } finally {
      setTogglingHeartbeat(false);
    }
  }

  const heartbeatStatusText =
    keepAliveActive === null
      ? 'Status: Checking...'
      : keepAliveActive
        ? 'Status: Server will stay awake'
        : 'Status: Server will sleep when idle';

  return (
    <main className="workspace-shell">
      <AppHeader />
      <section className="workspace-panel">
        <h1>Maintenance</h1>
        <p>Manage system utilities and account-level maintenance actions.</p>

        {error ? <p className="auth-error">{error}</p> : null}
        {info ? <p className="field-help">{info}</p> : null}

        <section className="maintenance-heartbeat" aria-labelledby="server-heartbeat-heading">
          <h2 id="server-heartbeat-heading">Server Heartbeat</h2>
          <p>Toggle keep-alive ping behavior for your Render free tier runtime.</p>
          <div className="maintenance-heartbeat-controls">
            <button
              type="button"
              role="switch"
              aria-checked={Boolean(keepAliveActive)}
              className={`heartbeat-toggle ${keepAliveActive ? 'active' : ''}`.trim()}
              onClick={() => void handleToggleHeartbeat()}
              disabled={loadingHeartbeat || togglingHeartbeat || keepAliveActive === null}
            >
              {togglingHeartbeat ? 'Updating...' : keepAliveActive ? 'On' : 'Off'}
            </button>
            <span className="maintenance-heartbeat-status">{heartbeatStatusText}</span>
          </div>
        </section>

        <p>Download a SQL backup script for your account data.</p>
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
