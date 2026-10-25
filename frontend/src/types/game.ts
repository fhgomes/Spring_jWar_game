import type { ArmyColor } from './api';

/**
 * Static map metadata describing geometry (SVG centroids, polygon paths,
 * tints) for the 42 territories of the WAR map. Lives in
 * `src/components/game/map-data.ts`; the type lives here.
 */
export interface TerritoryGeometry {
  /** Backend enum key — e.g., "BRA". */
  key: string;
  /** Backend numeric code. */
  code: number;
  /** Continent key — e.g., "AMS". */
  continent: ContinentKey;
  /** PT-BR display name (also keyed in i18n). */
  name: string;
  /** SVG polygon path data. */
  pathD: string;
  /** Centroid for the troop badge. */
  centroid: { x: number; y: number };
}

export type ContinentKey = 'AMS' | 'AMN' | 'EUR' | 'AFR' | 'OCE' | 'ASI';

export interface ContinentMeta {
  key: ContinentKey;
  /** PT-BR display name. */
  name: string;
  /** Tailwind color hex for the tint overlay. */
  tint: string;
  /** Bonus reward troops when fully owned. */
  reward: number;
  /** Centroid label position. */
  labelCentroid: { x: number; y: number };
}

/** UI-only state for the action history drawer. */
export interface ActionLogEntry {
  id: string;
  type: string;
  text: string; // localized
  timestamp: string;
}

/** Color-blind pattern overlay per army color. */
export const COLOR_PATTERN: Record<ArmyColor, 'circle' | 'triangle' | 'square' | 'star' | 'hex' | 'plus'> = {
  RED: 'circle',
  BLUE: 'triangle',
  GREEN: 'square',
  YELLOW: 'star',
  BLACK: 'hex',
  WHITE: 'plus',
  GRAY: 'hex',
  PURPLE: 'star',
};

export const ARMY_COLOR_HEX: Record<ArmyColor, string> = {
  RED: '#dc2626',
  BLUE: '#2563eb',
  GREEN: '#16a34a',
  YELLOW: '#eab308',
  BLACK: '#1f2937',
  WHITE: '#f9fafb',
  GRAY: '#6b7280',
  PURPLE: '#9333ea',
};
