import type { ReactNode } from 'react';
import { useToastStore, type ToastVariant } from '@/stores/useToastStore';
import { cn } from '@/lib/utils';
import { CheckCircle, AlertTriangle, XCircle, Info, X } from 'lucide-react';

const VARIANT_CLASSES: Record<ToastVariant, string> = {
  info: 'border-blue-500 bg-blue-950/90',
  success: 'border-green-600 bg-green-950/90',
  warning: 'border-yellow-500 bg-yellow-950/90',
  error: 'border-red-600 bg-red-950/90',
};

const ICONS: Record<ToastVariant, ReactNode> = {
  info: <Info className="h-5 w-5 text-blue-400" aria-hidden="true" />,
  success: <CheckCircle className="h-5 w-5 text-green-400" aria-hidden="true" />,
  warning: <AlertTriangle className="h-5 w-5 text-yellow-400" aria-hidden="true" />,
  error: <XCircle className="h-5 w-5 text-red-400" aria-hidden="true" />,
};

export function ToastViewport() {
  const toasts = useToastStore((s) => s.toasts);
  const dismiss = useToastStore((s) => s.dismiss);

  return (
    <div
      aria-live="polite"
      aria-atomic="true"
      className="pointer-events-none fixed right-4 top-4 z-[60] flex w-full max-w-sm flex-col gap-2"
    >
      {toasts.map((t) => (
        <div
          key={t.id}
          role="status"
          className={cn(
            'pointer-events-auto flex items-start gap-3 rounded-md border-l-4 p-3 shadow-strong animate-slide-in-right',
            VARIANT_CLASSES[t.variant],
          )}
        >
          <div className="mt-0.5 shrink-0">{ICONS[t.variant]}</div>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-medium text-army-white">{t.title}</p>
            {t.description && <p className="mt-0.5 text-xs text-table-200">{t.description}</p>}
          </div>
          <button
            type="button"
            aria-label="Dispensar notificação"
            onClick={() => dismiss(t.id)}
            className="shrink-0 rounded p-1 text-table-300 transition-colors hover:bg-table-700 hover:text-army-white focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      ))}
    </div>
  );
}
