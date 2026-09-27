import { useTranslation } from 'react-i18next';
import { Lock } from 'lucide-react';
import { cn } from '@/lib/utils';
import { ARMY_COLOR_HEX } from '@/types/game';
import type { ArmyColor } from '@/types/api';

const SUPPORTED: ArmyColor[] = ['RED', 'BLUE', 'GREEN', 'YELLOW', 'BLACK', 'WHITE'];

interface Props {
  selectedColor: ArmyColor | null;
  takenColors: ArmyColor[];
  onSelect(color: ArmyColor): void;
  disabled?: boolean;
}

export function ColorPicker({ selectedColor, takenColors, onSelect, disabled }: Props) {
  const { t } = useTranslation();

  return (
    <div className="flex flex-wrap gap-3" role="radiogroup" aria-label={t('room.color_picker_title') ?? ''}>
      {SUPPORTED.map((color) => {
        const isTaken = takenColors.includes(color) && selectedColor !== color;
        const isSelected = selectedColor === color;
        const localizedName = t(`colors.${color}`) ?? color;

        return (
          <button
            key={color}
            type="button"
            role="radio"
            aria-checked={isSelected}
            aria-label={t('colors.pick_aria', { color: localizedName }) ?? localizedName}
            aria-disabled={isTaken || disabled || undefined}
            disabled={isTaken || disabled}
            onClick={() => onSelect(color)}
            title={isTaken ? t('colors.taken') ?? '' : localizedName}
            className={cn(
              'relative h-12 w-12 rounded-full border-2 transition-all',
              'focus:outline-none focus-visible:ring-2 focus-visible:ring-army-red focus-visible:ring-offset-2 focus-visible:ring-offset-table-900',
              isSelected ? 'border-army-white ring-2 ring-army-red' : 'border-table-600',
              isTaken && 'pointer-events-none opacity-40',
            )}
            style={{ backgroundColor: ARMY_COLOR_HEX[color] }}
          >
            {isTaken && (
              <span className="absolute inset-0 grid place-items-center">
                <Lock className="h-4 w-4 text-army-white drop-shadow" aria-hidden="true" />
              </span>
            )}
            <span className="sr-only">{localizedName}</span>
          </button>
        );
      })}
    </div>
  );
}
