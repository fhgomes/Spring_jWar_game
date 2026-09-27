import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronDown, ChevronUp, Target } from 'lucide-react';
import { cn } from '@/lib/utils';

interface Props {
  objectiveText: string | undefined;
  /** True if viewer is a spectator (objectives hidden). */
  isSpectator: boolean;
}

export function ObjectivePanel({ objectiveText, isSpectator }: Props) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(true);

  if (isSpectator) {
    return (
      <div className="rounded-md border border-table-700 bg-table-800 p-3 text-sm text-table-200">
        <Target className="mr-2 inline-block h-4 w-4 align-text-bottom" />
        {t('match.objectives_hidden')}
      </div>
    );
  }

  if (!objectiveText) return null;

  return (
    <section className="rounded-md border border-table-700 bg-table-800">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="flex w-full items-center justify-between gap-2 px-3 py-2 text-sm font-semibold text-army-white hover:bg-table-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red"
      >
        <span className="flex items-center gap-2">
          <Target className="h-4 w-4" aria-hidden="true" />
          {t('match.my_objective')}
        </span>
        {open ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
      </button>
      <div
        className={cn(
          'overflow-hidden transition-all',
          open ? 'max-h-40 px-3 py-2' : 'max-h-0',
        )}
      >
        <p className="text-sm leading-relaxed text-table-100">{objectiveText}</p>
      </div>
    </section>
  );
}
