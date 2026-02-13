export function formatUsd(value: string | null | undefined): string {
  const numeric = Number(value ?? 0);
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2
  }).format(Number.isFinite(numeric) ? numeric : 0);
}

export function formatNumber(value: string | null | undefined): string {
  const numeric = Number(value ?? 0);
  return new Intl.NumberFormat('en-US', {
    maximumFractionDigits: 8
  }).format(Number.isFinite(numeric) ? numeric : 0);
}

export function formatPercent(value: string | null | undefined): string {
  const numeric = Number(value ?? 0);
  return `${numeric.toFixed(2)}%`;
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}
