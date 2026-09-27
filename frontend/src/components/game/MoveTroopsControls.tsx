import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { clamp } from '@/lib/utils';

interface Props {
  open: boolean;
  onClose(): void;
  onConfirm(count: number): Promise<void>;
  sourceName: string;
  targetName: string;
  minMove: number;
  maxMove: number;
  /** Custom title — overrides default "Quantos exércitos mover?". */
  title?: string;
  loading?: boolean;
}

/**
 * Shared slider for MOVE and MOVE_AFTER_CONQUEST modals.
 */
export function MoveTroopsControls({
  open,
  onClose,
  onConfirm,
  sourceName,
  targetName,
  minMove,
  maxMove,
  title,
  loading,
}: Props) {
  const { t } = useTranslation();
  const [count, setCount] = useState(minMove);

  useEffect(() => {
    if (open) setCount(clamp(minMove, minMove, maxMove));
  }, [open, minMove, maxMove]);

  const submit = async () => {
    await onConfirm(count);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title ?? t('match.move_modal_title')}
      description={`${sourceName} → ${targetName}`}
      size="md"
    >
      <div className="space-y-5">
        <div>
          <label className="flex items-center justify-between text-sm text-table-100">
            <span>Mover</span>
            <span className="font-mono text-lg text-army-white">{count}</span>
          </label>
          <input
            type="range"
            min={minMove}
            max={maxMove}
            value={count}
            onChange={(e) => setCount(parseInt(e.target.value, 10))}
            className="mt-2 w-full accent-army-red"
            aria-valuemin={minMove}
            aria-valuemax={maxMove}
            aria-valuenow={count}
          />
          <div className="mt-1 flex justify-between text-xs text-table-300">
            <span>{minMove}</span>
            <span>{maxMove}</span>
          </div>
        </div>

        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button type="button" onClick={submit} loading={loading}>
            {t('common.confirm')}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
