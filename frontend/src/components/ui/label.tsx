import type { LabelHTMLAttributes } from 'react';

export function Label({ className = '', ...props }: LabelHTMLAttributes<HTMLLabelElement>): JSX.Element {
  return <label {...props} className={`ui-label${className ? ` ${className}` : ''}`} />;
}
