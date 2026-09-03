/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        geo: {
          bg: '#FFFDF5',
          fg: '#1E293B',
          muted: '#F1F5F9',
          'muted-fg': '#64748B',
          accent: '#8B5CF6',
          'accent-fg': '#FFFFFF',
          secondary: '#F472B6',
          tertiary: '#FBBF24',
          quaternary: '#34D399',
          border: '#E2E8F0',
          input: '#FFFFFF',
          card: '#FFFFFF',
          ring: '#8B5CF6',
        },
        // Legacy aliases for backward compatibility during migration
        // audit-v5: playful.* block removed — zero usages across src (grep verified).
        accent: '#8B5CF6',
        secondary: '#F472B6',
        tertiary: '#FBBF24',
        quaternary: '#34D399',
        background: '#FFFDF5',
        foreground: '#1E293B',
        muted: '#F1F5F9',
        'muted-foreground': '#64748B',
        border: '#E2E8F0',
        input: '#FFFFFF',
        card: '#FFFFFF',
        ring: '#8B5CF6',
        // audit-v5 fix: 55 usages of bg-danger/10, text-success, border-warning...
        // existed across admin/user views but these colors were never defined in
        // Tailwind (only as --geo-* CSS vars) → every utility silently resolved
        // to nothing. Values mirror design-system.css --geo-danger/-warning/-success.
        danger: '#E11D48',
        warning: '#F59E0B',
        success: '#059669',
      },
      fontFamily: {
        sans: ['"Be Vietnam Pro"', 'system-ui', 'sans-serif'],
        heading: ['"Be Vietnam Pro"', 'system-ui', 'sans-serif'],
        // audit-v5: Be Vietnam Pro tuyệt đối — font-mono không còn trỏ hệ mono
        // riêng; giữ class hợp lệ nhưng resolve về BVP (single-family design system).
        mono: ['"Be Vietnam Pro"', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        // Major Third scale (1.25)
        xs: ['0.75rem', { lineHeight: '1rem' }],
        sm: ['0.875rem', { lineHeight: '1.25rem' }],
        base: ['1rem', { lineHeight: '1.75rem' }],
        lg: ['1.125rem', { lineHeight: '1.75rem' }],
        xl: ['1.25rem', { lineHeight: '1.75rem' }],
        '2xl': ['1.5rem', { lineHeight: '1.5rem' }],
        '3xl': ['1.875rem', { lineHeight: '2rem' }],
        '4xl': ['2.25rem', { lineHeight: '2.25rem' }],
        '5xl': ['3rem', { lineHeight: '1.1' }],
        '6xl': ['3.75rem', { lineHeight: '1.1' }],
      },
      borderRadius: {
        // audit-v5: removed duplicate `blob` key (0.5rem was silently overridden
        // by '24px 24px 24px 0' below); blob-2/blob-3 kept as the real shapes.
        'blob': '24px 24px 24px 0',
        'blob-sm': '0.375rem',
        'blob-lg': '0.75rem',
        'blob-xl': '1rem',
        'blob-speech': '1rem 1rem 1rem 0.25rem',
        'blob-arch': '9999px 9999px 0 0',
        sm: '8px',
        md: '16px',
        lg: '24px',
        full: '9999px',
        'blob-2': '30% 70% 70% 30% / 30% 30% 70% 70%',
        'blob-3': '9999px 9999px 0 0',
      },
      boxShadow: {
        'pop': '4px 4px 0px 0px #1E293B',
        'pop-sm': '2px 2px 0px 0px #1E293B',
        'pop-lg': '6px 6px 0px 0px #1E293B',
        'pop-xl': '8px 8px 0px 0px #E2E8F0',
        'pop-hover': '6px 6px 0px 0px #1E293B',
        'pop-active': '2px 2px 0px 0px #1E293B',
        'pop-accent': '4px 4px 0px 0px #8B5CF6',
        'pop-featured': '8px 8px 0px 0px #F472B6',
        'pop-pink': '4px 4px 0px 0px #F472B6',
        'inner-pop': 'inset 2px 2px 0px 0px #1E293B',
      },
      borderWidth: {
        DEFAULT: '2px',
      },
      transitionTimingFunction: {
        'bounce': 'cubic-bezier(0.34, 1.56, 0.64, 1)',
      },
      keyframes: {
        wiggle: {
          '0%, 100%': { transform: 'rotate(0deg)' },
          '25%': { transform: 'rotate(3deg)' },
          '75%': { transform: 'rotate(-3deg)' },
        },
        'pop-in': {
          '0%': { transform: 'scale(0)', opacity: '0' },
          '60%': { transform: 'scale(1.1)' },
          '100%': { transform: 'scale(1)', opacity: '1' },
        },
        marquee: {
          '0%': { transform: 'translateX(0)' },
          '100%': { transform: 'translateX(-50%)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-10px)' },
        },
      },
      animation: {
        wiggle: 'wiggle 0.3s ease-in-out',
        'pop-in': 'pop-in 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards',
        marquee: 'marquee 30s linear infinite',
        float: 'float 3s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
