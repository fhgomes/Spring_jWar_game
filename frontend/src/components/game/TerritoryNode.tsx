import { useTranslation } from 'react-i18next';
import { cn } from '@/lib/utils';
import { ARMY_COLOR_HEX } from '@/types/game';
import { TroopBadge } from './TroopBadge';
import type { ArmyColor } from '@/types/api';
import type { TerritoryGeometry } from '@/types/game';

interface Props {
  geometry: TerritoryGeometry;
  ownerColor: ArmyColor | null;
  troops: number;
  selected?: boolean;
  attackTarget?: boolean;
  moveTarget?: boolean;
  dimmed?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}

export function TerritoryNode({
  geometry,
  ownerColor,
  troops,
  selected,
  attackTarget,
  moveTarget,
  dimmed,
  disabled,
  onClick,
}: Props) {
  const { t } = useTranslation();
  const name = t(`countries.${geometry.key}`) ?? geometry.name;
  const fill = ownerColor ? ARMY_COLOR_HEX[ownerColor] : '#3f3a26';

  return (
    <g
      data-country={geometry.key}
      data-code={geometry.code}
      className={cn(
        'transition-opacity',
        disabled && !attackTarget && !moveTarget && 'cursor-not-allowed',
        dimmed && 'opacity-40',
      )}
      onClick={disabled ? undefined : onClick}
      role="button"
      tabIndex={0}
      aria-label={`${name} (${troops})`}
      onKeyDown={(e) => {
        if ((e.key === 'Enter' || e.key === ' ') && !disabled) {
          e.preventDefault();
          onClick?.();
        }
      }}
    >
      <title>{`${name} — ${troops}`}</title>
      <path
        d={geometry.pathD}
        fill={fill}
        fillOpacity={0.82}
        stroke="#0f0d08"
        strokeWidth={1.5}
        className={cn(
          'transition-colors hover:fill-opacity-100',
          !disabled && 'cursor-pointer',
        )}
      />
      {selected && (
        <path
          d={geometry.pathD}
          fill="none"
          stroke="#22c55e"
          strokeWidth={3}
          pointerEvents="none"
        />
      )}
      {attackTarget && (
        <path
          d={geometry.pathD}
          fill="none"
          stroke="#ef4444"
          strokeWidth={3}
          strokeDasharray="6 4"
          pointerEvents="none"
        >
          <animate
            attributeName="stroke-dashoffset"
            from="0"
            to="20"
            dur="1s"
            repeatCount="indefinite"
          />
        </path>
      )}
      {moveTarget && (
        <path
          d={geometry.pathD}
          fill="none"
          stroke="#22c55e"
          strokeWidth={3}
          strokeDasharray="6 4"
          pointerEvents="none"
        />
      )}
      <text
        x={geometry.centroid.x}
        y={geometry.centroid.y - 24}
        textAnchor="middle"
        fontSize={10}
        fill="#fefefe"
        className="pointer-events-none select-none drop-shadow"
      >
        {name}
      </text>
      <TroopBadge
        troops={troops}
        color={ownerColor}
        cx={geometry.centroid.x}
        cy={geometry.centroid.y}
      />
    </g>
  );
}
