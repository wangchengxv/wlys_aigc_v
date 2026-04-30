export type VisualStyleMode = 'preset' | 'custom';

export interface VisualStyleOption {
  id: number;
  title: string;
  value: string;
  styleKey: string;
  styleTemplateId: string;
  visualStylePrompt: string;
}

export interface Project {
  id?: number;
  name: string;
  description?: string;
  coverImage?: string;
  userId?: number;
  status?: string;
  style?: string;
  styleKey?: string;
  styleTemplateId?: string;
  visualStylePrompt?: string;
  visualStyleMode?: VisualStyleMode;
  visualStyleLongTextMode?: boolean;
  customStyleText?: string;
  aspectRatio?: string;
  createTime?: string;
  updateTime?: string;
}

export interface ProjectConfig {
  id?: number;
  projectId: number;
  dialogModel?: string;
  imageModel?: string;
  videoModel?: string;
  audioModel?: string;
  script?: string;
  episodeCount?: number;
  batchModel?: string;
  batchRatio?: string;
  batchQuality?: string;
  batchMethod?: string;
}

export interface ProjectForm {
  name: string;
  description?: string;
  style?: string;
  styleKey?: string;
  styleTemplateId?: string;
  visualStylePrompt?: string;
  visualStyleMode?: VisualStyleMode;
  visualStyleLongTextMode?: boolean;
  customStyleText?: string;
  aspectRatio?: string;
  coverImage?: string;
}

export const styleOptions: VisualStyleOption[] = [
  {
    id: 1,
    title: '动漫日韩',
    value: 'anime_japanese',
    styleKey: 'anime_japanese',
    styleTemplateId: 'template-anime-japanese',
    visualStylePrompt:
      '动漫日韩风，线条干净，赛璐璐渲染，电影级光影。角色：{角色特征}；场景：{场景信息}；镜头：{镜头语言}；氛围：{情绪氛围}',
  },
  {
    id: 2,
    title: '3D-皮克斯卡通',
    value: '3d_pixar',
    styleKey: '3d_pixar',
    styleTemplateId: 'template-3d-pixar',
    visualStylePrompt:
      '3D 皮克斯卡通风，高饱和配色，体积光明显，材质细节丰富。角色：{角色特征}；场景：{场景信息}；镜头：{镜头语言}；氛围：{情绪氛围}',
  },
  {
    id: 3,
    title: '写实-真人',
    value: 'realistic',
    styleKey: 'realistic_live_action',
    styleTemplateId: 'template-realistic-live',
    visualStylePrompt:
      '写实真人风，真实肤质与材质，胶片级景深，光比对比强。角色：{角色特征}；场景：{场景信息}；镜头：{镜头语言}；氛围：{情绪氛围}',
  },
  {
    id: 4,
    title: '动漫-Q版可爱',
    value: 'anime_cute',
    styleKey: 'anime_chibi_cute',
    styleTemplateId: 'template-anime-chibi',
    visualStylePrompt:
      'Q版可爱动漫风，圆润造型，马卡龙色系，表情夸张灵动。角色：{角色特征}；场景：{场景信息}；镜头：{镜头语言}；氛围：{情绪氛围}',
  },
  {
    id: 5,
    title: '风格化-像素风',
    value: 'pixel_art',
    styleKey: 'pixel_art',
    styleTemplateId: 'template-pixel-art',
    visualStylePrompt:
      '像素复古风，低分辨率像素块，街机色板，游戏化构图。角色：{角色特征}；场景：{场景信息}；镜头：{镜头语言}；氛围：{情绪氛围}',
  },
];

export const aspectRatioOptions = [
  { label: '16:9', value: '16:9' },
  { label: '9:16', value: '9:16' },
  { label: '1:1', value: '1:1' },
];

export const aiModels = [
  { id: '1', name: 'GPT-5.4', description: '强大的对话生成模型' },
  { id: '2', name: 'GLM-5.1', description: '国产高性能语言模型' },
  { id: '3', name: 'Doubao-Seed-2.0-Pro', description: '字节跳动AI生成模型' },
];
