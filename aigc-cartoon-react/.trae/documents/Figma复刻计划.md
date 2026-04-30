# 漫剧工作流 - Figma像素级前端复刻计划

## 一、设计分析

从Figma获取到的设计数据包含以下主要页面和组件：

### 页面结构
1. **Frame 12 (首页/项目列表)** - 项目列表页面，包含logo、导航栏、项目卡片、新建按钮
2. **Frame 16 (创作页面)** - 剧本创作工作区，包含顶部导航（全局设定→剧本→主体→分镜→剪辑成片）和输入区域
3. **Frame 14 (模型配置)** - 全局设定页面，包含项目概况、统计信息和模型配置
4. **Frame 13 (新建项目弹窗)** - 新建项目表单
5. **Frame 21 (批量生成弹窗)** - 批量生成设置弹窗

### 设计特点
- **配色方案**:
  - 背景色: #000000 (纯黑) 用于创作页面，#FFFFFF (白色) 用于首页
  - 强调色: #2B6EFF (蓝色按钮), #FFFFFF (文字)
  - 文字色: #FFFFFF (白), #656565 (灰), #AFAFAF (浅灰)
  - 卡片色: #232323 (深灰背景), #313131 (中等灰)

- **字体**:
  - Logo: Lobster, 28px
  - 标题: Alibaba PuHuiTi 2.0 Bold, 24px
  - 正文: Alibaba PuHuiTi 2.0 Regular, 18-28px

- **布局**: 1920x1080 基准，采用居中布局，间距68px

---

## 二、实现计划

### 阶段1: 基础架构重构
- [ ] 更新 `App.tsx` 路由配置，适配多页面结构
- [ ] 创建 `BasicLayout.tsx` 导航组件，包含logo和顶部导航
- [ ] 更新 `global.css` 全局样式（字体、基础重置）
- [ ] 更新 `BasicLayout.css` 布局样式

### 阶段2: 首页实现 (ProjectListPage)
- [ ] 创建 `src/pages/ProjectListPage.tsx` - 项目列表主页面
- [ ] 实现顶部导航组件（首页/项目/创作/资产库/配置中心）
- [ ] 实现项目列表区域（所有项目标题、项目卡片）
- [ ] 实现新建项目按钮
- [ ] 实现项目卡片组件（封面图、项目名、创建时间、操作按钮）

### 阶段3: 创作页面实现 (CreationPage)
- [ ] 创建 `src/pages/CreationPage.tsx` - 剧本创作页面
- [ ] 实现工作流进度导航（全局设定→剧本→主体→分镜→剪辑成片）
- [ ] 实现输入区域（告诉导演输入框、上传剧本按钮）
- [ ] 实现模型选择和集数设置下拉框

### 阶段4: 全局设定/模型配置页面 (GlobalConfigPage)
- [ ] 创建 `src/pages/GlobalConfigPage.tsx` - 全局设定页面
- [ ] 实现项目概况展示
- [ ] 实现统计卡片（角色、场景、道具、剧情结构数量）
- [ ] 实现模型配置区域（对话模型、图片模型、视频模型、音频模型）
- [ ] 实现AI模型选择卡片列表

### 阶段5: 弹窗组件实现
- [ ] 创建 `src/components/CreateProjectModal.tsx` - 新建项目弹窗
- [ ] 实现表单内容（项目名称、项目描述、画面比例选择、视觉风格、项目封面上传）
- [ ] 创建 `src/components/BatchGenerateModal.tsx` - 批量生成弹窗
- [ ] 实现配置项（模型选择、比例、质量、生成方式）

---

## 三、技术实现要点

### 路由配置
```
/                   -> 项目列表页 (ProjectListPage)
/creation           -> 创作页面 (CreationPage)
/creation/global    -> 全局设定/模型配置页 (GlobalConfigPage)
```

### 样式规范
- 使用CSS变量管理颜色主题
- 遵循Figma设计中的尺寸规范
- 响应式布局适配不同屏幕尺寸

### 组件结构
```
App
├── BasicLayout (导航+内容)
│   ├── Header
│   │   ├── Logo
│   │   └── Navigation
│   └── Content
│       ├── ProjectListPage
│       ├── CreationPage
│       └── GlobalConfigPage
└── Modals (弹窗层)
    ├── CreateProjectModal
    └── BatchGenerateModal
```

---

## 四、实现顺序

1. 先实现基础布局和路由
2. 实现首页完整功能
3. 实现创作页面
4. 实现全局设定页面
5. 最后实现弹窗组件
6. 测试验证各页面功能