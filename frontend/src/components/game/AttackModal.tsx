import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { clamp } from '@/lib/utils';
import type { AttackResultDto } from '@/types/api';
import { DiceRoll } from './DiceRoll';

interface Props {
  open: boolean;
  onClose(): void;
  onAttack(diceCount: number): Promise<void>;
  /** Number of troops in source territory. */
  sourceTroops: number;
  /** PT-BR label of source territory. */
  sourceName: string;
  /** PT-BR label of target territory. */
  targetName: string;
  /** Set once attack resolves (server response). */
  result: AttackResultDto | null;
  loading?: boolean;
}

export function AttackModal({
  open,
  onClose,
  onAttack,
  sourceTroops,
  sourceName,
  targetName,
  result,
  loading,
}: Props) {
  const { t } = useTranslation();
  const maxDice = Math.min(3, Math.max(1, sourceTroops - 1));
  const [dice, setDice] = useState(maxDice);

  const handleAttack = async () => {
    await onAttack(dice);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`${sourceName} → ${targetName}`}
      description={t('match.attack_modal_title') ?? ''}
      size="md"
    >
      {!result ? (
        <div className="space-y-4">
          <div>
            <p className="mb-2 text-sm text-table-200">{t('match.attack_dice_label')}</p>
            <div className="flex gap-2">
              {[1, 2, 3].map((n) => (
                <button
                  key={n}
                  type="button"
                  onClick={() => setDice(clamp(n, 1, maxDice))}
                  disabled={n > maxDice}
                  className={`h-12 w-12 rounded-md border-2 text-lg font-bold transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red disabled:cursor-not-allowed disabled:opacity-30 ${
                    dice === n
                      ? 'border-army-red bg-army-red text-army-white'
                      : 'border-table-600 text-army-white hover:border-army-red'
                  }`}
                  aria-pressed={dice === n}
                  aria-label={`${n} dado${n > 1 ? 's' : ''}`}
                >
                  {n}
                </button>
              ))}
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={onClose}>
              {t('common.cancel')}
            </Button>
            <Button type="button" onClick={handleAttack} loading={loading}>
              {t('match.attack_button')}
            </Button>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          <h3 className="text-sm font-semibold text-army-white">
            {t('match.attack_result_title')}
          </h3>
          <div className="flex items-center justify-around gap-4">
            <div className="text-center">
              <p className="mb-1 text-xs uppercase text-table-300">Ataque</p>
              <DiceRoll values={result.attackers} variant="attack" />
            </div>
            <div className="text-center">
              <p className="mb-1 text-xs uppercase text-table-300">Defesa</p>
              <DiceRoll values={result.defense} variant="defense" />
            </div>
          </div>
          <p className="text-center text-sm text-table-100">
            {t('match.attack_summary', {
              atkLoss: result.srcCountryLoss,
              defLoss: result.targetCountryLoss,
            })}
          </p>
          {result.conquered && (
            <p className="text-center text-lg font-semibold text-army-yellow">
              {t('match.conquered')}
            </p>
          )}
          <div className="flex justify-end">
            <Button onClick={onClose}>{t('common.close')}</Button>
          </div>
        </div>
      )}
    </Modal>
  );
}
