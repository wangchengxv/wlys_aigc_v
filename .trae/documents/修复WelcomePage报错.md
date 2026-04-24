# 修复 WelcomePage.tsx 报错计划

## 问题分析

文件路径：`aigc-site-react/src/pages/WelcomePage.tsx`

### 错误 1（必须修复 - Error）
- **位置**: 第125行
- **问题**: `<button>` 元素使用了 `loading={socialLoading}` 属性，但原生 HTML button 不支持此属性
- **解决方案**: 将原生 `<button>` 替换为项目中已有的 `AppButton` 组件（该组件支持 `loading` 属性）

### 错误 2（建议修复 - Hint）
- **位置**: 第1行和第31行
- **问题**: `FormEvent` 已弃用
- **解决方案**: 改为使用 `React.FormEvent`

## 实施步骤

1. **替换 social 按钮组件**
   - 将 `<button type="button" className="welcome-social__btn" loading={socialLoading} ...>` 替换为 `<AppButton variant="secondary" ...>`
   - 使用 `AppButton` 组件替代原生 button，确保 `loading` 属性正常工作

2. **更新类型导入**
   - 将 `import { useState, type FormEvent } from 'react'` 改为 `import { useState, type ReactElement } from 'react'`
   - 或保留 FormEvent 但使用 `React.FormEvent` 类型

3. **验证修复**
   - 运行 TypeScript 检查确保无错误
   - 检查构建是否成功

## 预期结果
- 消除所有 Error 和 Hint
- 保持原有功能和 UI 效果不变
