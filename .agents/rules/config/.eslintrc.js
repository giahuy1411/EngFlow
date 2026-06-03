// .eslintrc.js — English Learning Platform (Vue.js 3)
// Install: npm install -D eslint eslint-plugin-vue @vue/eslint-config-prettier
//          npm install -D @rushstack/eslint-patch

require('@rushstack/eslint-patch/modern-module-resolution')

module.exports = {
  root: true,

  env: {
    browser: true,
    es2022: true,
    node: true,
  },

  extends: [
    'plugin:vue/vue3-recommended',     // Vue 3 recommended rules
    'eslint:recommended',
    '@vue/eslint-config-prettier',     // Disable ESLint rules conflicting with Prettier
  ],

  parserOptions: {
    ecmaVersion: 2022,
    sourceType: 'module',
  },

  rules: {
    // ============ VUE RULES ============

    // Bắt buộc component name multi-word (tránh clash với HTML elements)
    'vue/multi-word-component-names': 'error',

    // Thứ tự thuộc tính trong component
    'vue/order-in-components': 'error',

    // Bắt buộc key trên v-for
    'vue/require-v-for-key': 'error',

    // Cấm v-if và v-for trên cùng element
    'vue/no-use-v-if-with-v-for': 'error',

    // Props phải có kiểu dữ liệu
    'vue/require-prop-types': 'error',

    // Prop default values
    'vue/require-default-prop': 'warn',

    // Không có unused variable trong template
    'vue/no-unused-vars': 'error',

    // Cấm dùng v-html không cần thiết (XSS risk)
    'vue/no-v-html': 'warn',

    // Thứ tự attributes trong template
    'vue/attributes-order': ['warn', {
      order: [
        'DEFINITION',       // is, v-is
        'LIST_RENDERING',   // v-for
        'CONDITIONALS',     // v-if, v-else-if, v-else, v-show
        'RENDER_MODIFIERS', // v-pre, v-once
        'UNIQUE',           // ref, key
        'TWO_WAY_BINDING',  // v-model
        'OTHER_DIRECTIVES',
        'OTHER_ATTR',       // props, custom attrs
        'EVENTS',           // @click, v-on
        'CONTENT',          // v-html, v-text
      ],
    }],

    // Component name casing trong template
    'vue/component-name-in-template-casing': ['error', 'PascalCase'],

    // Prop casing
    'vue/prop-name-casing': ['error', 'camelCase'],

    // Emit casing
    'vue/custom-event-name-casing': ['error', 'kebab-case'],

    // Self-closing tags
    'vue/html-self-closing': ['warn', {
      html: { void: 'always', normal: 'never', component: 'always' },
      svg: 'always',
      math: 'always',
    }],

    // ============ JS RULES ============

    // Cấm console.log (dùng Vue DevTools)
    'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'warn',

    // Cấm debugger
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'warn',

    // Cấm biến unused
    'no-unused-vars': ['error', {
      vars: 'all',
      args: 'after-used',
      ignoreRestSiblings: true,
    }],

    // Dùng const khi không reassign
    'prefer-const': 'error',

    // Dùng === thay vì ==
    'eqeqeq': ['error', 'always'],

    // Cấm var — dùng let/const
    'no-var': 'error',

    // Template literals thay vì string concat
    'prefer-template': 'warn',

    // Không dùng alert/confirm/prompt
    'no-alert': 'warn',

    // Không eval
    'no-eval': 'error',

    // Không có code sau return
    'no-unreachable': 'error',

    // Camelcase cho variable names
    'camelcase': ['warn', { properties: 'never' }],
  },

  // Ignore files
  ignorePatterns: [
    'dist/',
    'node_modules/',
    'public/',
    '*.min.js',
  ],
}
