import type { Config } from 'tailwindcss'

const config: Config = {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // primary-* utilities resolve to CSS variables populated at runtime
        // by src/lib/accentPresets.ts. The Settings → Appearance picker
        // retints every primary-* utility without a rebuild. Fallback values
        // match the indigo preset so SSR / pre-hydration paint matches the
        // historical default.
        primary: {
          50:  'rgb(var(--primary-50, 238 242 255) / <alpha-value>)',
          100: 'rgb(var(--primary-100, 224 231 255) / <alpha-value>)',
          200: 'rgb(var(--primary-200, 199 210 254) / <alpha-value>)',
          300: 'rgb(var(--primary-300, 165 180 252) / <alpha-value>)',
          400: 'rgb(var(--primary-400, 129 140 248) / <alpha-value>)',
          500: 'rgb(var(--primary-500, 99 102 241) / <alpha-value>)',
          600: 'rgb(var(--primary-600, 79 70 229) / <alpha-value>)',
          700: 'rgb(var(--primary-700, 67 56 202) / <alpha-value>)',
          800: 'rgb(var(--primary-800, 55 48 163) / <alpha-value>)',
          900: 'rgb(var(--primary-900, 49 46 129) / <alpha-value>)',
        },
      },
    },
  },
  plugins: [require('@tailwindcss/typography')],
}

export default config
