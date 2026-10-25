import { useTranslation } from 'react-i18next';

/**
 * Split-screen hero for auth pages (FR-018 of spec 008).
 * Visible only at `lg+`; mobile shows a small inline logo instead.
 */
export function AuthHero() {
  const { t } = useTranslation();
  return (
    <aside className="relative hidden overflow-hidden bg-table-950 lg:flex lg:flex-col lg:justify-end">
      {/* Background — abstract continent silhouettes */}
      <svg
        viewBox="0 0 800 1000"
        className="absolute inset-0 h-full w-full opacity-40"
        aria-hidden="true"
      >
        <defs>
          <linearGradient id="hero-bg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#0e0d08" />
            <stop offset="100%" stopColor="#46412a" />
          </linearGradient>
        </defs>
        <rect width="800" height="1000" fill="url(#hero-bg)" />
        <g fill="#facc15" opacity="0.18">
          <ellipse cx="180" cy="220" rx="140" ry="100" />
          <ellipse cx="540" cy="180" rx="200" ry="120" />
          <ellipse cx="400" cy="520" rx="170" ry="130" />
          <ellipse cx="650" cy="780" rx="110" ry="90" />
          <ellipse cx="240" cy="800" rx="120" ry="80" />
        </g>
        <g fill="#dc2626" opacity="0.12">
          <circle cx="120" cy="120" r="40" />
          <circle cx="680" cy="320" r="50" />
          <circle cx="400" cy="700" r="60" />
        </g>
      </svg>

      <div className="relative z-10 p-12">
        <h1 className="font-serif text-5xl text-army-white">jWar</h1>
        <p className="mt-4 max-w-md font-serif text-2xl leading-tight text-army-white">
          {t('app.tagline')}
        </p>
      </div>
    </aside>
  );
}
