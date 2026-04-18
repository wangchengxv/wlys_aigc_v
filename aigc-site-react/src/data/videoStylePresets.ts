import type { GlobalVisualStyleMode, StyleTemplate, VideoStylePreset } from '@/types'

export const VIDEO_STYLE_PRESETS: VideoStylePreset[] = [
  {
    id: 'oriental-ink-wash',
    category: '国风 / 东方美学',
    name: '水墨风',
    traits: '黑墨晕染、流动渐变、极简留白，充满东方写意意境',
    fullPrompt:
      '江南水乡夜景，乌篷船在江面缓缓划过，渔火点点，一轮明月挂在天边，桃花花瓣随流水飘落，黑墨在白底上晕染的水墨风格，流动的渐变，极简东方美学，镜头缓慢推进，柔光，8K高清，画面稳定无抖动，无模糊无畸形',
  },
  {
    id: 'oriental-classic-elegance',
    category: '国风 / 东方美学',
    name: '古风雅韵',
    traits: '米黄 + 墨绿 + 赭石的传统色调，中式古典场景，温婉雅致',
    fullPrompt:
      '25岁国风白衣女子在亭中品茶，桃花花瓣缓缓飘落，古风庭院，水墨意境，柔光慢镜，中式对称构图，无现代元素，米黄+墨绿+赭石的古风色调，中景慢推，1080P高清，画面稳定，无畸形无模糊',
  },
  {
    id: 'oriental-cyber-dunhuang',
    category: '国风 / 东方美学',
    name: '赛博敦煌',
    traits: '传统敦煌元素与赛博霓虹的融合，复古与未来的碰撞',
    fullPrompt:
      '赛博敦煌风格，飞天神女在霓虹光效中飞舞，反弹琵琶，全息敦煌壁画在背景闪烁，neon apsara，紫蓝霓虹灯光，传统敦煌配色与未来科技感结合，镜头环绕人物旋转，4K高清，无穿模',
  },
  {
    id: 'scifi-cyberpunk',
    category: '科幻 / 未来',
    name: '赛博朋克',
    traits: '紫蓝 + 品红的霓虹色调、高对比、全息元素，都市末世感',
    fullPrompt:
      "雨夜东京街头，全息广告牌闪烁'NEON DREAM'，穿机甲风夹克的年轻人抬头，瞳孔反射出蓝紫光斑，雨水在金属肩甲上滑落拉出细线，霓虹灯浸染，高对比，全息元素，都市末世感，镜头从远景缓慢推进到人物特写，冷色调，4K高清，无模糊",
  },
  {
    id: 'scifi-futurism',
    category: '科幻 / 未来',
    name: '未来主义',
    traits: '极简科技感、几何线条、冷调金属质感，未来极简美学',
    fullPrompt:
      '未来主义风格，纯白的未来太空舱内，宇航员操作着透明全息屏幕，几何线条的舱体设计，冷调金属质感，柔和的环境光，镜头从操作台缓慢拉远到全景，展现整个太空舱的宏大结构，8K高清，电影级质感',
  },
  {
    id: 'retro-hk',
    category: '复古怀旧',
    name: '复古港风',
    traits: '红橙 + 青蓝的对比色调、80 年代香港街头氛围，胶片颗粒感',
    fullPrompt:
      '夜晚港式茶餐厅，穿花衬衫的年轻男女坐在窗边对话，窗外霓虹招牌闪烁，雨水打在玻璃上留下水痕，红橙与青蓝对比色调，复古港风，柯达5219胶片颗粒感，柔光，中景镜头，缓慢推进，暖色调，9:16竖屏，画面稳定',
  },
  {
    id: 'retro-film',
    category: '复古怀旧',
    name: '复古胶片',
    traits: '胶片颗粒、略微减饱和、暖色调、柔焦边缘，怀旧生活感',
    fullPrompt:
      '夏日午后的乡村小路，女孩骑着自行车穿过麦田，风吹起她的裙摆，阳光透过树叶洒下斑驳的光影，复古胶片风格，胶片颗粒感，略微减饱和，暖色调，柔焦边缘，跟拍镜头，慢动作，怀旧氛围，16:9横屏',
  },
  {
    id: 'retro-8mm',
    category: '复古怀旧',
    name: '8mm 家庭录像',
    traits: '重颗粒、漏光、不稳定画面、褪色，老家庭录像的复古感',
    fullPrompt:
      '8mm家庭录像风格，90年代的家庭生日派对，孩子们围着蛋糕吹蜡烛，大人在旁边笑着拍照，重颗粒感，轻微漏光，画面轻微晃动，褪色的暖色调，回忆感，中景固定镜头，无多余修饰',
  },
  {
    id: 'anime-cel',
    category: '动画 / 卡通',
    name: '二次元动漫',
    traits: '赛璐珞风格、鲜艳色彩、夸张动态线条，日系动漫质感',
    fullPrompt:
      '二次元动画风格，魔法阵在角色脚下旋转展开，符文如弹幕环绕，蓄力到顶点就射出彩虹光束带星形尾迹，蓝发少女站在城市天台，晚风吹动她的校服裙摆，星星点点的光粒在她身边飞舞，赛璐珞风格，鲜艳色彩，夸张运动，动态线条，镜头环绕人物旋转，1080P高清，无穿模无畸形',
  },
  {
    id: 'anime-pixel',
    category: '动画 / 卡通',
    name: '像素风格',
    traits: '8bit 复古像素块、低分辨率质感，怀旧游戏风',
    fullPrompt:
      '在充满色彩的宇宙中，像素角色穿梭于形态各异、色调独特的星球之间。每个星球上都有奇异的地形和外星生物。近景特写镜头下，玩家角色站在画面中央，正在与一只友好的外星生物对话。画面上方有像素化的星际风暴和能量漩涡特效，缓缓旋转，带来动态感。整体风格复古而充满未来感，色彩鲜明跳跃，8bit像素风格，画面流畅',
  },
  {
    id: 'anime-puppet',
    category: '动画 / 卡通',
    name: '木偶动画',
    traits: '木偶质感、缓慢刻意的帧动画，复古定格动画感',
    fullPrompt:
      '在昏暗的维多利亚风格客厅里，花边窗帘轻轻飘动。毛毡和木制木偶围坐在圆桌旁，摇曳的烛光映照出它们的身影。一声低语震颤了瓷茶杯，画上的眼睛不安地转动。每个缓慢而刻意的木偶动画帧都让场面更加紧张。相机缓缓向右平移，展示木偶们的每一个细微动作，增强了诡异的氛围，定格动画风格，4K高清',
  },
  {
    id: 'anime-chibi',
    category: '动画 / 卡通',
    name: 'Q 版卡通',
    traits: '可爱的 Q 版人物、马卡龙色调，软萌治愈',
    fullPrompt:
      'Q版卡通人物，可爱的小猫拟人角色，在甜品店拿着冰淇淋蹦蹦跳跳，马卡龙色调，场景简洁，流畅动画，无穿模无畸形，暖光，中景镜头，画面稳定，治愈氛围，9:16竖屏',
  },
  {
    id: 'film-cinematic',
    category: '影视 / 专业影视',
    name: '电影感',
    traits: '宽银幕、浅景深、专业色彩分级，电影级质感',
    fullPrompt:
      '雪山日出，雪峰从云海中缓缓露出，第一缕阳光把山顶染成金色，山谷还在蓝影之中。镜头从云层缓慢升起，展现整个山脉的全景，广角镜头，史诗感，电影感色彩分级，2.39:1宽银幕，浅景深，8K高清，无抖动',
  },
  {
    id: 'film-documentary',
    category: '影视 / 专业影视',
    name: '纪录片',
    traits: '手持镜头感、自然光、观察性构图，真实纪实感',
    fullPrompt:
      '纪录片风格，藏区的牧民在草原上放牧，牦牛在草地上吃草，阳光透过云层洒下，自然光，手持轻微晃动的镜头，观察性构图，真实的生活细节，中景跟拍，16:9横屏，无修饰',
  },
  {
    id: 'film-noir',
    category: '影视 / 专业影视',
    name: '黑色电影',
    traits: '高对比黑白、百叶窗阴影、低调照明，悬疑复古感',
    fullPrompt:
      '黑色电影风格，昏暗的侦探办公室，侦探坐在办公桌前，百叶窗的阴影在他脸上切割出条纹，台灯的暖光照亮桌上的文件，高对比黑白，低调照明，悬疑氛围，特写镜头，缓慢推进，无多余色彩',
  },
  {
    id: 'film-commercial',
    category: '影视 / 专业影视',
    name: '商业广告',
    traits: '干净明亮、精确布光、丝滑运镜，产品展示质感',
    fullPrompt:
      '高端手表360°缓慢旋转展示，细节特写，纯白简约背景，柔和顶光，高级质感，4K高清，丝滑运镜，适合电商主图，无模糊无抖动',
  },
  {
    id: 'art-oil',
    category: '艺术手绘',
    name: '油画质感',
    traits: '可见笔触、厚涂纹理，古典艺术质感',
    fullPrompt:
      '印象派油画风格，黄昏的森林里，狐狸在林间缓缓穿行，身形仿佛正在溶解，与背景的模糊森林色彩无缝融合。狐狸轮廓柔和，毛色为淡赭石与灰棕混合，眼神神秘而警觉。画面中可见斑驳的光影透过树冠洒落，营造出一种梦幻、飘渺的氛围，阳光以晕染方式呈现，可见的油画笔触纹理，丰富的厚涂感，跟随镜头，4K高清',
  },
  {
    id: 'art-watercolor',
    category: '艺术手绘',
    name: '水彩风格',
    traits: '透明晕染、柔和色彩、水痕纹理，清新文艺',
    fullPrompt:
      '水彩风格，春日的樱花林，花瓣随风飘落，女孩在林间漫步，透明的水彩晕染效果，柔和的马卡龙色彩，水痕纹理，清新文艺，镜头缓慢推进，柔光，治愈氛围，1080P高清',
  },
  {
    id: 'misc-vaporwave',
    category: '其他特色',
    name: '蒸汽波',
    traits: '复古未来主义、粉紫蓝霓虹、复古雕塑、网格背景，80 年代复古科技风',
    fullPrompt:
      '蒸汽波风格，复古希腊雕像站在紫色网格背景前，粉蓝霓虹灯光环绕，老式CRT显示器闪烁着复古图案，棕榈树的剪影，80年代复古科技感，aesthetic statue, purple grid，镜头缓慢旋转，高饱和色彩，画面流畅',
  },
  {
    id: 'misc-nordic',
    category: '其他特色',
    name: '北欧清新',
    traits: '白灰 + 浅蓝绿的柔和色调、极简干净，治愈清新',
    fullPrompt:
      '春日公园野餐，白色餐布上摆放着马卡龙和柠檬水，樱花花瓣缓缓飘落，阳光透过樱花树洒下柔光，白灰+浅蓝绿的北欧清新色调，干净明亮的画面，柔光，中景固定镜头，治愈氛围，莫兰迪低饱和，9:16竖屏',
  },
  {
    id: 'misc-gothic',
    category: '其他特色',
    name: '暗黑哥特',
    traits: '高对比暗调、穹顶建筑、蜡烛光源，神秘暗黑氛围',
    fullPrompt:
      '暗黑哥特风格，古老的哥特式教堂穹顶下，摇曳的蜡烛照亮昏暗的走廊，阴影在石墙上拉长，穿着黑色长裙的女人缓缓走过，vaulted ceiling, candle，高对比暗调，神秘氛围，镜头缓慢推进，冷色调，无多余光线',
  },
  {
    id: 'misc-pop-art',
    category: '其他特色',
    name: '波普艺术',
    traits: '半色调网点、鲜艳撞色，流行艺术风格',
    fullPrompt:
      '波普艺术风格，街头的涂鸦墙，鲜艳的撞色图案，半色调网点纹理，矿物颜料质感，卡通化的人物形象，色彩明快，镜头环绕展示，高饱和，4K高清',
  },
]

const PRESET_BY_ID = new Map(VIDEO_STYLE_PRESETS.map((p) => [p.id, p]))

function toStyleTemplate(preset: VideoStylePreset): StyleTemplate {
  return {
    templateId: preset.id,
    scope: 'SYSTEM',
    name: preset.name,
    category: preset.category,
    traits: preset.traits,
    fullPrompt: preset.fullPrompt,
    styleKey: null,
    ownerId: null,
    ownerName: null,
    orgUnitId: null,
    courseId: null,
    enabled: true,
    createdAt: '1970-01-01T00:00:00.000Z',
    updatedAt: '1970-01-01T00:00:00.000Z',
  }
}

export const FALLBACK_STYLE_TEMPLATES: StyleTemplate[] = VIDEO_STYLE_PRESETS.map(toStyleTemplate)

export function getPresetById(
  id: string,
  templates: readonly StyleTemplate[] = FALLBACK_STYLE_TEMPLATES,
): StyleTemplate | undefined {
  const matched = templates.find((item) => item.templateId === id)
  if (matched) return matched
  const fallback = PRESET_BY_ID.get(id)
  return fallback ? toStyleTemplate(fallback) : undefined
}

export function presetDescriptor(p: Pick<StyleTemplate, 'name' | 'traits'>): string {
  const traits = p.traits?.trim()
  return traits ? `${p.name}：${traits}` : p.name
}

const FALLBACK_STYLE = '电影感写实'

export type GlobalStyleSlice = {
  visualStyleMode: GlobalVisualStyleMode
  visualStylePresetId: string | null
  customVisualStyle: string
  visualStyleLongTextMode: boolean
}

export function resolveVisualStyleForProject(
  s: GlobalStyleSlice,
  templates: readonly StyleTemplate[] = FALLBACK_STYLE_TEMPLATES,
): string {
  if (s.visualStyleMode === 'custom') {
    const t = s.customVisualStyle.trim()
    return t || FALLBACK_STYLE
  }
  const id = s.visualStylePresetId
  if (!id) return FALLBACK_STYLE
  const preset = getPresetById(id, templates)
  if (!preset) return FALLBACK_STYLE
  if (s.visualStyleLongTextMode) {
    return preset.fullPrompt.trim() || presetDescriptor(preset)
  }
  return presetDescriptor(preset)
}

/** 新建项目页「全局偏好」一行中的简短展示 */
export function styleSummaryShort(
  s: GlobalStyleSlice,
  templates: readonly StyleTemplate[] = FALLBACK_STYLE_TEMPLATES,
): string {
  if (s.visualStyleMode === 'custom') {
    const t = s.customVisualStyle.trim()
    if (!t) return '自定义（未填）'
    return t.length > 28 ? `${t.slice(0, 28)}…` : t
  }
  const p = s.visualStylePresetId ? getPresetById(s.visualStylePresetId, templates) : null
  if (!p) return '预设（未选）'
  const long = s.visualStyleLongTextMode ? ' · 长文本' : ''
  return `预设：${p.name}${long}`
}

export function groupPresetsByCategory(
  templates: readonly StyleTemplate[] = FALLBACK_STYLE_TEMPLATES,
): { category: string; presets: StyleTemplate[] }[] {
  const order: string[] = []
  const map = new Map<string, StyleTemplate[]>()
  for (const p of templates) {
    const category = p.category?.trim() || '未分类'
    if (!map.has(category)) {
      order.push(category)
      map.set(category, [])
    }
    map.get(category)!.push(p)
  }
  return order.map((category) => ({ category, presets: map.get(category)! }))
}
