import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import type { MatchFinishedPayload } from '@/types/api';

interface Props {
  open: boolean;
  payload: MatchFinishedPayload | null;
  onViewFinalMap(): void;
}

export function EndGameModal({ open, payload, onViewFinalMap }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  if (!payload) return null;

  return (
    <Modal
      open={open}
      onClose={() => undefined}
      isStatic
      hideCloseButton
      title={t('match.end_game_title', { name: payload.winnerDisplayName })}
      size="lg"
    >
      <div className="space-y-4">
        <p className="text-sm text-table-100">
          {t('match.end_game_objective', { text: payload.objectiveText })}
        </p>

        <div className="grid grid-cols-2 gap-3 rounded-md border border-table-700 bg-table-900 p-3 text-sm">
          <div>
            <p className="text-table-300">Turnos jogados</p>
            <p className="text-lg font-bold text-army-white">{payload.turnNumber}</p>
          </div>
        </div>

        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onViewFinalMap}>
            {t('match.view_final_map')}
          </Button>
          <Button onClick={() => navigate('/lobby')}>{t('match.back_to_lobby')}</Button>
        </div>
      </div>
    </Modal>
  );
}
