# 计划：将全局设定添加到左侧侧边栏

## 目标
在左侧侧边栏中，将"全局设定"菜单项放置在"首页"下方、"创作工作台"上方。

## 当前结构分析
`AppShellNav` 组件 (`src/components/layout/AppShellNav.tsx`) 中的 `SHELL_NAV` 数组定义了侧边栏菜单结构。

当前 `workspace` 组（工作台）结构：
```
- 首页 (/, patterns: ['/'])
- 创作工作台 (/workspace, patterns: [...])
- 无限画布 (/canvas)
- 历史记录 (/history)
```

## 修改方案
在 `workspace` 组的 `items` 数组中，在"首页"和"创作工作台"之间添加"全局设定"菜单项：
```typescript
{ label: '全局设定', to: '/global-settings' }
```

## 修改文件
- `src/components/layout/AppShellNav.tsx` - 第 53-63 行的 `SHELL_NAV` 定义

## 实施步骤
1. 打开 `src/components/layout/AppShellNav.tsx`
2. 找到 `SHELL_NAV` 中 `workspace` 组的 `items` 数组
3. 在 `{ label: '首页', ... }` 之后、`{ label: '创作工作台', ... }` 之前插入新项 `{ label: '全局设定', to: '/global-settings' }`
