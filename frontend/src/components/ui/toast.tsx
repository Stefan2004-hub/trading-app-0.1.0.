export type ToastVariant = 'success' | 'error';

export interface ToastItem {
  id: string;
  message: string;
  variant: ToastVariant;
}

interface ToastProps {
  items: ToastItem[];
}

export function ToastContainer({ items }: ToastProps): JSX.Element {
  return (
    <div className="toast-container" aria-live="polite" aria-atomic="true">
      {items.map((item) => (
        <div key={item.id} className={`toast toast-${item.variant}`}>
          <span className="toast-icon">{item.variant === 'success' ? 'OK' : 'X'}</span>
          <span>{item.message}</span>
        </div>
      ))}
    </div>
  );
}
