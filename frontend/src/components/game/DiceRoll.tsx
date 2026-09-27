import { useEffect, useState } from 'react';
import { cn } from '@/lib/utils';

interface DiceRollProps {
  values: number[];
  variant: 'attack' | 'defense';
  animate?: boolean;
}

export function DiceRoll({ values, variant, animate = true }: DiceRollProps) {
  const [rolling, setRolling] = useState(animate);

  useEffect(() => {
    if (!animate) {
      setRolling(false);
      return;
    }
    setRolling(true);
    const timer = setTimeout(() => setRolling(false), 800);
    return () => clearTimeout(timer);
  }, [values, animate]);

  return (
    <div className="flex items-center gap-2">
      {values.map((v, i) => (
        <Die
          key={`${variant}-${i}`}
          value={v}
          rolling={rolling}
          variant={variant}
          delayMs={i * 80}
        />
      ))}
    </div>
  );
}

interface DieProps {
  value: number;
  rolling: boolean;
  variant: 'attack' | 'defense';
  delayMs: number;
}

function Die({ value, rolling, variant, delayMs }: DieProps) {
  const bg = variant === 'attack' ? '#dc2626' : '#1f2937';
  const fg = '#f9fafb';
  return (
    <div
      className={cn(
        'grid h-10 w-10 place-items-center rounded-md text-base font-bold shadow-soft',
        rolling && 'animate-dice-roll',
      )}
      style={{
        backgroundColor: bg,
        color: fg,
        animationDelay: `${delayMs}ms`,
      }}
      aria-label={`${variant === 'attack' ? 'Ataque' : 'Defesa'}: ${value}`}
    >
      {value}
    </div>
  );
}
