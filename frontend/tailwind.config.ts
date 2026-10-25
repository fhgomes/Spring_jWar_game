import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: 'class',
  theme: {
    screens: {
      sm: '640px',
      md: '768px',
      lg: '1024px',
      xl: '1280px',
      '2xl': '1536px',
    },
    extend: {
      colors: {
        // Six WAR army colors (Manual §2.1)
        army: {
          red: '#dc2626',
          blue: '#2563eb',
          green: '#16a34a',
          yellow: '#eab308',
          black: '#1f2937',
          white: '#f9fafb',
        },
        // Board table (dark green-brown background)
        table: {
          50: '#f5f4f0',
          100: '#e6e3d6',
          200: '#cdc7b2',
          300: '#a89f7f',
          400: '#80784f',
          500: '#5b5536',
          600: '#46412a',
          700: '#363220',
          800: '#272418',
          900: '#1a1810',
          950: '#0e0d08',
        },
        // Continent tints (subtle overlays)
        continent: {
          ams: '#facc15', // South America — yellow
          amn: '#22c55e', // North America — green
          eur: '#ef4444', // Europe — red
          afr: '#3b82f6', // Africa — blue
          oce: '#a855f7', // Oceania — purple
          asi: '#f97316', // Asia — orange
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        serif: ['Cinzel', 'Georgia', 'serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'monospace'],
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        DEFAULT: 'var(--radius)',
        md: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
        xl: 'var(--radius-xl)',
      },
      boxShadow: {
        soft: '0 2px 8px rgba(0, 0, 0, 0.2)',
        glow: '0 0 20px rgba(220, 38, 38, 0.4)',
      },
      keyframes: {
        'dice-roll': {
          '0%': { transform: 'rotateX(0) rotateY(0)' },
          '50%': { transform: 'rotateX(360deg) rotateY(720deg)' },
          '100%': { transform: 'rotateX(720deg) rotateY(1440deg)' },
        },
        'slide-in-right': {
          '0%': { transform: 'translateX(100%)', opacity: '0' },
          '100%': { transform: 'translateX(0)', opacity: '1' },
        },
        pulse: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.5' },
        },
      },
      animation: {
        'dice-roll': 'dice-roll 800ms ease-in-out',
        'slide-in-right': 'slide-in-right 250ms ease-out',
      },
    },
  },
  plugins: [],
};

export default config;
