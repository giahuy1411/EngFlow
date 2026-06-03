/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Be Vietnam Pro"', 'sans-serif'],
      },
      colors: {
        background: '#F0F0F0',
        foreground: '#121212',
        'primary-red': '#D02020',
        'primary-blue': '#1040C0',
        'primary-yellow': '#F0C020',
        border: '#121212',
        muted: '#E0E0E0'
      },
      boxShadow: {
        'hard-sm': '4px 4px 0px 0px rgba(18, 18, 18, 1)',
        'hard-md': '6px 6px 0px 0px rgba(18, 18, 18, 1)',
        'hard-lg': '8px 8px 0px 0px rgba(18, 18, 18, 1)',
      }
    },
  },
  plugins: [],
}
