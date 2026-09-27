import type { ContinentMeta, TerritoryGeometry } from '@/types/game';

/**
 * Stylized WAR map geometry. Coordinates use the SVG viewBox `0 0 1600 900`.
 *
 * IMPORTANT: These polygons are intentionally schematic — they preserve
 * relative positioning and adjacency feel but are NOT geographically
 * accurate. See `src/assets/map/README.md` for provenance.
 *
 * The 42 territories match `EClassicCountries` (Manual §1).
 */

export const CONTINENTS: ContinentMeta[] = [
  {
    key: 'AMS',
    name: 'America do Sul',
    tint: '#facc15',
    reward: 2,
    labelCentroid: { x: 320, y: 760 },
  },
  {
    key: 'AMN',
    name: 'America do Norte',
    tint: '#22c55e',
    reward: 5,
    labelCentroid: { x: 280, y: 200 },
  },
  {
    key: 'EUR',
    name: 'Europa',
    tint: '#ef4444',
    reward: 5,
    labelCentroid: { x: 740, y: 240 },
  },
  {
    key: 'AFR',
    name: 'Africa',
    tint: '#3b82f6',
    reward: 3,
    labelCentroid: { x: 760, y: 660 },
  },
  {
    key: 'OCE',
    name: 'Oceania',
    tint: '#a855f7',
    reward: 2,
    labelCentroid: { x: 1340, y: 720 },
  },
  {
    key: 'ASI',
    name: 'Asia',
    tint: '#f97316',
    reward: 7,
    labelCentroid: { x: 1180, y: 260 },
  },
];

/**
 * Each territory is an octagonal-ish polygon centered at the given centroid.
 * The path is built mechanically (centroid +/- offsets) so the visual is
 * consistent across all 42 cells.
 */
function poly(cx: number, cy: number, w = 70, h = 50): string {
  const x0 = cx - w / 2;
  const x1 = cx + w / 2;
  const y0 = cy - h / 2;
  const y1 = cy + h / 2;
  const dx = w * 0.25;
  return `M${x0 + dx} ${y0} L${x1 - dx} ${y0} L${x1} ${cy} L${x1 - dx} ${y1} L${x0 + dx} ${y1} L${x0} ${cy} Z`;
}

interface Cell {
  key: string;
  code: number;
  continent: TerritoryGeometry['continent'];
  name: string;
  x: number;
  y: number;
}

const CELLS: Cell[] = [
  // America do Sul (4) — left bottom column
  { key: 'VEN', code: 4, continent: 'AMS', name: 'Venezuela', x: 280, y: 580 },
  { key: 'PER', code: 3, continent: 'AMS', name: 'Peru', x: 260, y: 680 },
  { key: 'BRA', code: 1, continent: 'AMS', name: 'Brasil', x: 380, y: 700 },
  { key: 'ARG', code: 2, continent: 'AMS', name: 'Argentina', x: 320, y: 820 },

  // America do Norte (9) — left top region; 3 rows
  { key: 'ALA', code: 12, continent: 'AMN', name: 'Alaska', x: 100, y: 130 },
  { key: 'MAC', code: 10, continent: 'AMN', name: 'Mackenzie', x: 200, y: 130 },
  { key: 'GRO', code: 9, continent: 'AMN', name: 'Groenlândia', x: 480, y: 110 },
  { key: 'VAN', code: 8, continent: 'AMN', name: 'Vancouver', x: 140, y: 230 },
  { key: 'OTW', code: 12, continent: 'AMN', name: 'Ottawa', x: 300, y: 230 },
  { key: 'LAB', code: 11, continent: 'AMN', name: 'Labrador', x: 420, y: 230 },
  { key: 'CAL', code: 6, continent: 'AMN', name: 'Califórnia', x: 160, y: 340 },
  { key: 'NVA', code: 7, continent: 'AMN', name: 'Nova York', x: 320, y: 340 },
  { key: 'MEX', code: 5, continent: 'AMN', name: 'México', x: 240, y: 450 },

  // Europa (7) — top center
  { key: 'ISL', code: 13, continent: 'EUR', name: 'Islândia', x: 620, y: 160 },
  { key: 'SWD', code: 17, continent: 'EUR', name: 'Suécia', x: 760, y: 160 },
  { key: 'ENG', code: 14, continent: 'EUR', name: 'Inglaterra', x: 620, y: 260 },
  { key: 'ALE', code: 15, continent: 'EUR', name: 'Alemanha', x: 760, y: 260 },
  { key: 'POL', code: 18, continent: 'EUR', name: 'Polônia', x: 880, y: 260 },
  { key: 'FRA', code: 16, continent: 'EUR', name: 'França', x: 680, y: 360 },
  { key: 'MOS', code: 19, continent: 'EUR', name: 'Moscow', x: 880, y: 360 },

  // Africa (6) — center-bottom
  { key: 'ARL', code: 20, continent: 'AFR', name: 'Argélia', x: 660, y: 500 },
  { key: 'EGY', code: 21, continent: 'AFR', name: 'Egito', x: 800, y: 500 },
  { key: 'SUD', code: 22, continent: 'AFR', name: 'Sudão', x: 800, y: 620 },
  { key: 'CON', code: 23, continent: 'AFR', name: 'Congo', x: 720, y: 720 },
  { key: 'ADS', code: 25, continent: 'AFR', name: 'África do Sul', x: 760, y: 820 },
  { key: 'MAD', code: 24, continent: 'AFR', name: 'Madagascar', x: 900, y: 800 },

  // Oceania (4) — bottom right
  { key: 'SUM', code: 26, continent: 'OCE', name: 'Sumatra', x: 1240, y: 620 },
  { key: 'BOR', code: 29, continent: 'OCE', name: 'Borneo', x: 1340, y: 620 },
  { key: 'NVG', code: 28, continent: 'OCE', name: 'Nova Guiné', x: 1440, y: 700 },
  { key: 'AUS', code: 27, continent: 'OCE', name: 'Austrália', x: 1340, y: 820 },

  // Asia (13) — top-right massive block
  { key: 'ORI', code: 31, continent: 'ASI', name: 'Oriente Médio', x: 940, y: 480 },
  { key: 'IND', code: 42, continent: 'ASI', name: 'Índia', x: 1080, y: 480 },
  { key: 'VIE', code: 33, continent: 'ASI', name: 'Vietnã', x: 1220, y: 480 },
  { key: 'ARA', code: 32, continent: 'ASI', name: 'Aral', x: 1020, y: 360 },
  { key: 'OMK', code: 35, continent: 'ASI', name: 'Omsk', x: 1160, y: 360 },
  { key: 'CHI', code: 36, continent: 'ASI', name: 'China', x: 1300, y: 360 },
  { key: 'MON', code: 37, continent: 'ASI', name: 'Mongólia', x: 1300, y: 250 },
  { key: 'CHT', code: 40, continent: 'ASI', name: 'Chita', x: 1180, y: 250 },
  { key: 'DUD', code: 39, continent: 'ASI', name: 'Dudinka', x: 1060, y: 250 },
  { key: 'SIB', code: 30, continent: 'ASI', name: 'Sibéria', x: 1060, y: 140 },
  { key: 'VLD', code: 41, continent: 'ASI', name: 'Vladivostok', x: 1440, y: 250 },
  { key: 'JAP', code: 38, continent: 'ASI', name: 'Japão', x: 1500, y: 380 },
  { key: 'OTW2', code: 12, continent: 'ASI', name: 'Ottawa', x: 0, y: 0 }, // placeholder removed below
];

// Remove the stray placeholder; the real OTW is in AMN
CELLS.pop();

export const TERRITORIES: TerritoryGeometry[] = CELLS.map((c) => ({
  key: c.key,
  code: c.code,
  continent: c.continent,
  name: c.name,
  centroid: { x: c.x, y: c.y },
  pathD: poly(c.x, c.y),
}));

export const TERRITORY_BY_KEY: Record<string, TerritoryGeometry> = TERRITORIES.reduce(
  (acc, t) => {
    acc[t.key] = t;
    return acc;
  },
  {} as Record<string, TerritoryGeometry>,
);

export const TERRITORY_BY_CODE: Record<number, TerritoryGeometry> = TERRITORIES.reduce(
  (acc, t) => {
    acc[t.code] = t;
    return acc;
  },
  {} as Record<number, TerritoryGeometry>,
);
