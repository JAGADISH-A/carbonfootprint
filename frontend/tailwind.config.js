/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        surface: '#F7FBF7',
        card: '#ffffff',
        'card-hover': '#FAFDF9',
        border: '#E8F0E8',
        'border-light': '#F0F5F0',

        emerald: {
          50:  '#ECFDF5',
          100: '#D1FAE5',
          200: '#A7F3D0',
          300: '#6EE7B7',
          400: '#34D399',
          500: '#10B981',
          600: '#059669',
          700: '#047857',
          800: '#065F46',
          900: '#064E3B',
        },

        leaf: {
          50:  '#F4FCE3',
          100: '#E8F9C3',
          200: '#D4F38F',
          300: '#B8E84F',
          400: '#A3DC28',
          500: '#85CC12',
          600: '#66A30A',
          700: '#4D7C0F',
          800: '#3F6212',
          900: '#365314',
        },

        amber: {
          50:  '#FFFBEB',
          100: '#FEF3C7',
          200: '#FDE68A',
          300: '#FCD34D',
          400: '#FBBF24',
          500: '#F59E0B',
          600: '#D97706',
        },

        ink: {
          DEFAULT: '#1A2E1C',
          light:   '#3D5A40',
          muted:   '#6B8A6E',
          faint:   '#94A396',
          ghost:   '#B8C4B9',
        },

        cream: {
          50:  '#FEFDF8',
          100: '#FDF9ED',
          200: '#FAF2D6',
          300: '#F5E8B5',
          400: '#EDDA8A',
          500: '#E5CC60',
        },
      },

      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },

      borderRadius: {
        'xl':  '16px',
        '2xl': '20px',
        '3xl': '24px',
        '4xl': '32px',
      },

      boxShadow: {
        'card':      '0 1px 3px rgba(6,78,59,0.04)',
        'card-md':   '0 2px 8px rgba(6,78,59,0.06)',
        'card-lg':   '0 4px 16px rgba(6,78,59,0.08)',
        'soft':      '0 2px 8px rgba(6,78,59,0.04)',
        'emerald':   '0 4px 14px rgba(16,185,129,0.25)',
        'inner-soft':'inset 0 1px 2px rgba(6,78,59,0.04)',
      },

      animation: {
        'fade-in':      'fadeIn 0.5s ease-out',
        'slide-up':     'slideUp 0.5s ease-out',
        'slide-down':   'slideDown 0.4s ease-out',
        'scale-in':     'scaleIn 0.4s ease-out',
        'pulse-soft':   'pulseSoft 3s ease-in-out infinite',
      },

      keyframes: {
        fadeIn: {
          '0%':   { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%':   { transform: 'translateY(16px)', opacity: '0' },
          '100%': { transform: 'translateY(0)',     opacity: '1' },
        },
        slideDown: {
          '0%':   { transform: 'translateY(-8px)', opacity: '0' },
          '100%': { transform: 'translateY(0)',    opacity: '1' },
        },
        scaleIn: {
          '0%':   { transform: 'scale(0.96)', opacity: '0' },
          '100%': { transform: 'scale(1)',    opacity: '1' },
        },
        pulseSoft: {
          '0%, 100%': { opacity: '1' },
          '50%':      { opacity: '0.7' },
        },
      },
    },
  },
  plugins: [
    require('@tailwindcss/typography'),
  ],
}
