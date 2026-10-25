import { useTranslation } from 'react-i18next';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import type { CardSnapshot } from '@/types/api';

interface Props {
  open: boolean;
  onClose(): void;
  onConfirm(): Promise<void>;
  selected: CardSnapshot[];
  ownedCountryKeys: string[];
  award: number;
  loading?: boolean;
}

export function ExchangeCardsModal({
  open,
  onClose,
  onConfirm,
  selected,
  ownedCountryKeys,
  award,
  loading,
}: Props) {
  const { t } = useTranslation();

  const ownedBonusCards = selected.filter(
    (c) => c.countryKey && ownedCountryKeys.includes(c.countryKey),
  );

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={t('match.exchange_modal_title')}
      size="md"
    >
      <div className="space-y-4">
        <p className="text-sm text-army-white">
          {t('match.exchange_cards_reward', { count: award })}
        </p>

        {ownedBonusCards.length > 0 && (
          <ul className="rounded-md border border-army-yellow/40 bg-army-yellow/10 p-2 text-sm text-yellow-100">
            {ownedBonusCards.map((c) => (
              <li key={c.id}>
                {t('match.exchange_cards_territory_bonus', {
                  country: c.countryKey ? t(`countries.${c.countryKey}`) : '',
                })}
              </li>
            ))}
          </ul>
        )}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button type="button" onClick={onConfirm} loading={loading}>
            {t('common.confirm')}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
