import { Button } from './ui/button';
import { Dialog } from './ui/dialog';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  confirmText: string;
  loadingText?: string;
  cancelText?: string;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmText,
  loadingText = 'Working...',
  cancelText = 'Cancel',
  loading = false,
  onConfirm,
  onCancel
}: ConfirmDialogProps): JSX.Element | null {
  return (
    <Dialog open={open} title={title} onClose={loading ? () => undefined : onCancel}>
      <p>{message}</p>
      <div className="dialog-actions">
        <Button type="button" variant="secondary" onClick={onCancel} disabled={loading}>
          {cancelText}
        </Button>
        <Button type="button" variant="danger" onClick={onConfirm} disabled={loading}>
          {loading ? <span className="button-spinner" aria-hidden="true" /> : null}
          {loading ? loadingText : confirmText}
        </Button>
      </div>
    </Dialog>
  );
}
