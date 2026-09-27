import clsx, { type ClassValue } from 'clsx';

/**
 * Concatenate class names conditionally.
 */
export function cn(...inputs: ClassValue[]): string {
  return clsx(inputs);
}

/**
 * Format a date in pt-BR locale.
 */
export function formatDate(date: string | Date, options?: Intl.DateTimeFormatOptions): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  return new Intl.DateTimeFormat('pt-BR', options ?? { dateStyle: 'short', timeStyle: 'short' }).format(d);
}

/**
 * Format a relative time in PT-BR — "agora", "há 2 min", "às 14:32".
 */
export function formatRelative(date: string | Date): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  const diffSec = Math.floor((Date.now() - d.getTime()) / 1000);

  if (diffSec < 30) return 'agora';
  if (diffSec < 60) return `há ${diffSec}s`;
  if (diffSec < 3600) return `há ${Math.floor(diffSec / 60)} min`;
  if (diffSec < 86400) return `há ${Math.floor(diffSec / 3600)}h`;

  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(d);
}

/**
 * Unique short ID for client-side keys (toast queue, optimistic temp ids).
 */
export function uid(): string {
  return Math.random().toString(36).slice(2, 10);
}

export function clamp(n: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, n));
}
