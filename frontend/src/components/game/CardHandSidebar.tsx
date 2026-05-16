import { useTranslation } from 'react-i18next';
import { useState, type ComponentType } from 'react';
import { Circle, Triangle, Square, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import type { CardSnapshot, CountrySnapshot } from '@/types/api';

interface Props {
  cards: CardSnapshot[];
  ownedCountryKeys: string[];
  /** Backend-supplied award for the current exchange round (TABELA II). */
  exchangeRoundAward?: number;
  onExchange(cardIds: string[]): Promise<void>;
  loading?: boolean;
  countries: CountrySnapshot[];
}

const SHAPE_ICONS: Record<CardSnapshot['shape'], ComponentType<{ className?: string }>> = {
  CIRCLE: Circle,
  TRIANGLE: Triangle,
  SQUARE: Square,
  JOKER: Sparkles,
};

export function CardHandSidebar({
  cards,
  ownedCountryKeys,
  exchangeRoundAward,
  onExchange,
  loading,
  countries,
}: Props) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState<string[]>([]);

  const isValidSet = isValidExchange(cards.filter((c) => selected.includes(c.id)));

  const toggle = (cardId: string) => {
    setSelected((prev) => {
      if (prev.includes(cardId)) return prev.filter((id) => id !== cardId);
      if (prev.length >= 3) return prev;
      return [...prev, cardId];
    });
  };

  const submit = async () => {
    if (!isValidSet) return;
    await onExchange(selected);
    setSelected([]);
  };

  return (
    <aside className="flex h-full flex-col gap-2 border-l border-table-700 bg-table-800 p-3">
      <h2 className="text-sm font-semibold uppercase text-army-white">{t('match.my_cards')}</h2>

      {cards.length === 0 ? (
        <p className="py-6 text-center text-sm text-table-300">{t('match.no_cards')}</p>
      ) : (
        <ul className="flex-1 space-y-2 overflow-y-auto">
          {cards.map((card) => {
            const Icon = SHAPE_ICONS[card.shape];
            const country = card.countryKey
              ? countries.find((c) => c.key === card.countryKey)
              : null;
            const owns = card.countryKey ? ownedCountryKeys.includes(card.countryKey) : false;
            const isSelected = selected.includes(card.id);
            return (
              <li key={card.id}>
                <button
                  type="button"
                  onClick={() => toggle(card.id)}
                  className={cn(
                    'flex w-full items-center gap-2 rounded-md border-2 p-2 text-left text-sm transition-colors',
                    'focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red',
                    isSelected
                      ? 'border-army-red bg-army-red/10'
                      : 'border-table-600 hover:bg-table-700',
                  )}
                  aria-pressed={isSelected}
                >
                  <Icon className="h-4 w-4 shrink-0 text-army-white" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium text-army-white">
                      {card.countryKey
                        ? t(`countries.${card.countryKey}`) ?? country?.name ?? card.countryKey
                        : 'Coringa'}
                    </p>
                    {owns && (
                      <p className="text-xs text-army-yellow">{t('common.you')} possui</p>
                    )}
                  </div>
                </button>
              </li>
            );
          })}
        </ul>
      )}

      <div className="border-t border-table-700 pt-2">
        {!isValidSet ? (
          <p className="mb-2 text-xs text-table-300">{t('match.exchange_cards_select')}</p>
        ) : (
          <p className="mb-2 text-xs text-army-yellow">
            {t('match.exchange_cards_reward', { count: exchangeRoundAward ?? 4 })}
          </p>
        )}
        <Button
          fullWidth
          disabled={!isValidSet}
          loading={loading}
          onClick={submit}
        >
          {t('match.exchange_cards')}
        </Button>
      </div>
    </aside>
  );
}

/**
 * Manual §10: a valid exchange set is exactly 3 cards, either:
 *   - all the same shape, OR
 *   - all different shapes (jokers wild).
 */
export function isValidExchange(cards: CardSnapshot[]): boolean {
  if (cards.length !== 3) return false;
  const shapes = cards.map((c) => c.shape);
  const jokers = shapes.filter((s) => s === 'JOKER').length;
  const nonJokers = shapes.filter((s) => s !== 'JOKER');
  const unique = new Set(nonJokers);

  // All same (with jokers filling in)
  if (unique.size <= 1) return true;
  // All different (3 distinct non-joker, or 2 distinct + 1 joker, or 1 + 2 jokers)
  if (unique.size + jokers >= 3 && unique.size === nonJokers.length) return true;
  return false;
}
