import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronDown, ChevronUp, History } from 'lucide-react';
import { cn, formatRelative } from '@/lib/utils';
import type { ActionLogEntry } from '@/types/game';

interface Props {
  entries: ActionLogEntry[];
}

export function ActionFeed({ entries }: Props) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [seenCount, setSeenCount] = useState(0);
  const newCount = Math.max(0, entries.length - seenCount);

  const handleToggle = () => {
    setOpen((v) => {
      const next = !v;
      if (next) setSeenCount(entries.length);
      return next;
    });
  };

  return (
    <section className="border-t border-table-700 bg-table-900/95">
      <button
        type="button"
        onClick={handleToggle}
        aria-expanded={open}
        className="flex w-full items-center justify-between gap-2 px-4 py-2 text-sm font-medium text-army-white hover:bg-table-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red"
      >
        <span className="flex items-center gap-2">
          <History className="h-4 w-4" aria-hidden="true" />
          {t('match.history')}
          {!open && newCount > 0 && (
            <span className="rounded-full bg-army-red px-2 py-0.5 text-xs text-army-white">
              {t('match.history_new')} {newCount}
            </span>
          )}
        </span>
        {open ? <ChevronDown className="h-4 w-4" /> : <ChevronUp className="h-4 w-4" />}
      </button>

      <div
        className={cn(
          'overflow-y-auto transition-all',
          open ? 'max-h-48 px-4 py-2' : 'max-h-0',
        )}
      >
        {entries.length === 0 ? (
          <p className="py-4 text-center text-sm text-table-300">{t('match.history_empty')}</p>
        ) : (
          <ul className="space-y-1">
            {entries
              .slice()
              .reverse()
              .map((e) => (
                <li key={e.id} className="flex items-baseline gap-2 text-xs text-table-100">
                  <span className="shrink-0 font-mono text-table-300">
                    {formatRelative(e.timestamp)}
                  </span>
                  <span>{e.text || e.type}</span>
                </li>
              ))}
          </ul>
        )}
      </div>
    </section>
  );
}
