import { useTranslation } from 'react-i18next';
import { cn } from '@/lib/utils';
import type { GamePhase } from '@/types/api';

interface Props {
  phase: GamePhase;
  helpText?: string;
}

/**
 * Contextual help shown beneath the board explaining what to do in the
 * current phase. Drives most of its content from the i18n bundle.
 */
export function ActionPanel({ phase, helpText }: Props) {
  const { t } = useTranslation();
  const phaseLabel = t(`phases.${phase}`);

  const hint =
    helpText ??
    (phase === 'ADD'
      ? 'Clique nos seus territórios para posicionar tropas.'
      : phase === 'ATTACK'
        ? 'Selecione um território próprio e depois um adjacente inimigo.'
        : 'Selecione um território próprio e depois outro próprio contíguo.');

  return (
    <div className={cn('rounded-md border border-table-700 bg-table-800 px-3 py-2 text-sm')}>
      <p className="text-army-white">
        <span className="mr-2 inline-block rounded-full bg-army-red/30 px-2 py-0.5 text-xs uppercase tracking-wider text-red-200">
          {phaseLabel}
        </span>
        <span className="text-table-100">{hint}</span>
      </p>
    </div>
  );
}
