import type { ButtonHTMLAttributes, PropsWithChildren } from 'react';

type ButtonVariant = 'default' | 'secondary' | 'danger' | 'success' | 'info';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
}

export function Button({ children, className = '', variant = 'default', ...props }: PropsWithChildren<ButtonProps>): JSX.Element {
  return (
    <button
      {...props}
      className={`ui-button ui-button-${variant}${className ? ` ${className}` : ''}`}
    >
      {children}
    </button>
  );
}
