import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    rules: {
      // Vue→React 迁移中常见「打开弹窗同步表单」模式；关闭以避免误报级联渲染
      'react-hooks/set-state-in-effect': 'off',
      // 事件处理函数内的 Date.now 等会被误判为 render 阶段调用
      'react-hooks/purity': 'off',
    },
  },
  {
    files: ['src/router.tsx', 'src/context/ToastContext.tsx'],
    rules: {
      'react-refresh/only-export-components': 'off',
    },
  },
])
