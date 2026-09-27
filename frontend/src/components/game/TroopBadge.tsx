import { cn } from '@/lib/utils';
import { ARMY_COLOR_HEX } from '@/types/game';
import type { ArmyColor } from '@/types/api';

interface Props {
  troops: number;
  color: ArmyColor | null;
  cx: number;
  cy: number;
  size?: number;
}

export function TroopBadge({ troops, color, cx, cy, size = 14 }: Props) {
  const fill = color ? ARMY_COLOR_HEX[color] : '#374151';
  const textColor = color === 'WHITE' || color === 'YELLOW' ? '#111' : '#fff';
  return (
    <g pointerEvents="none">
      <circle cx={cx} cy={cy} r={size} fill={fill} stroke="#0f0d08" strokeWidth={1.5} />
      <text
        x={cx}
        y={cy + 4}
        textAnchor="middle"
        fontSize={size * 0.95}
        fontWeight={700}
        fill={textColor}
        className={cn('select-none')}
      >
        {troops}
      </text>
    </g>
  );
}
