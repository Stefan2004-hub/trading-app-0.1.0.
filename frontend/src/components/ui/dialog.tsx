import type { PropsWithChildren } from 'react';

interface DialogProps {
  open: boolean;
  title: string;
  onClose: () => void;
  panelClassName?: string;
}

export function Dialog({ open, title, onClose, panelClassName, children }: PropsWithChildren<DialogProps>): JSX.Element | null {
  if (!open) {
    return null;
  }

  return (
    <div className="dialog-backdrop" role="presentation" onClick={onClose}>
      <div
        className={`dialog-panel${panelClassName ? ` ${panelClassName}` : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="dialog-header">
          <h3>{title}</h3>
          <button type="button" className="dialog-close" onClick={onClose} aria-label="Close dialog">
            x
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
