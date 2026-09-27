import { useMemo } from 'react';
import { CONTINENTS, TERRITORIES } from './map-data';
import { TerritoryNode } from './TerritoryNode';
import type { ArmyColor } from '@/types/api';
import type { CountrySnapshot, PlayerSnapshot } from '@/types/api';

interface Props {
  countries: CountrySnapshot[];
  players: PlayerSnapshot[];
  selectedCountry?: number | null;
  attackTargets?: number[];
  moveTargets?: number[];
  dimmedCountries?: number[];
  disabled?: boolean;
  onTerritoryClick?(country: CountrySnapshot): void;
}

export function BoardSvg({
  countries,
  players,
  selectedCountry,
  attackTargets = [],
  moveTargets = [],
  dimmedCountries = [],
  disabled,
  onTerritoryClick,
}: Props) {
  // Map userId -> color for quick lookup.
  const colorByUserId = useMemo(() => {
    const m: Record<string, ArmyColor> = {};
    players.forEach((p) => {
      m[p.userId] = p.color;
    });
    return m;
  }, [players]);

  const countryByCode = useMemo(() => {
    const m: Record<number, CountrySnapshot> = {};
    countries.forEach((c) => {
      m[c.code] = c;
    });
    return m;
  }, [countries]);

  return (
    <svg
      viewBox="0 0 1600 900"
      role="img"
      aria-label="Mapa do jogo"
      className="h-full w-full select-none"
      preserveAspectRatio="xMidYMid meet"
    >
      {/* Ocean / table background */}
      <rect width="1600" height="900" fill="#1a1810" />
      <rect width="1600" height="900" fill="url(#oceanGradient)" opacity="0.15" />

      <defs>
        <radialGradient id="oceanGradient" cx="50%" cy="50%" r="80%">
          <stop offset="0%" stopColor="#1e3a5f" />
          <stop offset="100%" stopColor="#0f1a2a" />
        </radialGradient>
      </defs>

      {/* Continent backdrops */}
      {CONTINENTS.map((c) => {
        const cellsInContinent = TERRITORIES.filter((t) => t.continent === c.key);
        if (cellsInContinent.length === 0) return null;
        const xs = cellsInContinent.map((t) => t.centroid.x);
        const ys = cellsInContinent.map((t) => t.centroid.y);
        const x0 = Math.min(...xs) - 60;
        const x1 = Math.max(...xs) + 60;
        const y0 = Math.min(...ys) - 60;
        const y1 = Math.max(...ys) + 60;
        return (
          <g key={c.key}>
            <rect
              x={x0}
              y={y0}
              width={x1 - x0}
              height={y1 - y0}
              rx={20}
              ry={20}
              fill={c.tint}
              opacity={0.08}
            />
            <text
              x={c.labelCentroid.x}
              y={y0 + 28}
              textAnchor="middle"
              fontSize={20}
              fontWeight={700}
              fill={c.tint}
              opacity={0.85}
              className="pointer-events-none"
            >
              {c.name.toUpperCase()}
            </text>
          </g>
        );
      })}

      {/* Territories */}
      {TERRITORIES.map((geom) => {
        const country = countryByCode[geom.code];
        const owner = country?.ownerUserId ? colorByUserId[country.ownerUserId] : null;
        return (
          <TerritoryNode
            key={geom.key}
            geometry={geom}
            ownerColor={owner ?? null}
            troops={country?.troops ?? 0}
            selected={selectedCountry === geom.code}
            attackTarget={attackTargets.includes(geom.code)}
            moveTarget={moveTargets.includes(geom.code)}
            dimmed={dimmedCountries.includes(geom.code)}
            disabled={disabled || !country}
            onClick={() => country && onTerritoryClick?.(country)}
          />
        );
      })}
    </svg>
  );
}
