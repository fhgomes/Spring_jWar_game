import { forwardRef, type InputHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  error?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, error, type = 'text', ...props }, ref) => (
    <input
      ref={ref}
      type={type}
      aria-invalid={error || undefined}
      className={cn(
        'block w-full rounded-md border bg-table-800 px-3 py-2 text-army-white placeholder:text-table-300',
        'transition-colors',
        'focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red focus-visible:ring-offset-2 focus-visible:ring-offset-table-900',
        'disabled:cursor-not-allowed disabled:opacity-50',
        error ? 'border-army-red' : 'border-table-600 focus:border-army-red',
        className,
      )}
      {...props}
    />
  ),
);
Input.displayName = 'Input';
