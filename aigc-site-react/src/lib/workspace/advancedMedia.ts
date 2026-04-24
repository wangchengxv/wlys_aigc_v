import type {
  GenerateAdvancedImageExtraRequest,
  GenerateAdvancedMediaRequest,
  GenerateAdvancedVideoOneLinkSeedanceExtraRequest,
  GenerateMode,
  ImageAdvancedCapability,
  VideoViduOptions,
} from '../../types'

export type ViduTri = '' | 'true' | 'false'

export type ViduFormState = {
  duration: string
  seed: string
  resolution: string
  movement_amplitude: string
  audio: ViduTri
  audio_type: string
  off_peak: ViduTri
  watermark: ViduTri
  wm_position: string
  wm_url: string
  voice_id: string
  is_rec: ViduTri
  bgm: ViduTri
  payload: string
  meta_data: string
  callback_url: string
}

export type ImageAdvancedFormState = {
  reference2imageUrl: string
  klingReferenceImages: string[]
  outpaintSourceImageUrl: string
  outpaintTop: string
  outpaintRight: string
  outpaintBottom: string
  outpaintLeft: string
  omniSourceImageUrl: string
  omniMode: string
  omniSubjectPrompt: string
}

export type WorkspaceAdvancedMediaPayload = {
  advancedMedia?: GenerateAdvancedMediaRequest
  videoReferenceImageUrl?: string
  videoViduOptions?: VideoViduOptions
}

type BuildWorkspaceAdvancedMediaPayloadParams = {
  mode: GenerateMode
  imageAdvancedCapability?: ImageAdvancedCapability
  imageAdvancedForm: ImageAdvancedFormState
  videoReferenceImageUrl: string
  finalVideoModel: string
  viduForm: ViduFormState
}

type WorkspaceAdvancedValidationParams = {
  needImg: boolean
  needVid: boolean
  imageAdvancedCapability?: ImageAdvancedCapability
  imageAdvancedForm: ImageAdvancedFormState
  finalVideoModel: string
  videoReferenceImageUrl: string
  allowVideoFirstFrameImage?: boolean
}

export function workspaceVideoNeedsFirstFrameImage(videoModel: string): boolean {
  const m = videoModel.trim().toLowerCase()
  if (!m) return false
  if (m.startsWith('vidu')) return true
  if (m === 'kling-v1' || m === 'kling-v1-6') return true
  if (m.includes('wan') && m.includes('i2v')) return true
  if (m.includes('moark')) return true
  return false
}

export function isValidWorkspaceReferenceImage(ref: string): boolean {
  const t = ref.trim()
  if (!t) return false
  if (t.startsWith('http://') || t.startsWith('https://')) return true
  return /^data:image\//i.test(t) && t.includes('base64')
}

export function isNonNegativeIntegerText(value: string): boolean {
  const t = value.trim()
  if (!t) return true
  return /^\d+$/.test(t)
}

export function parseNonNegativeIntegerText(value: string): number | undefined {
  const t = value.trim()
  if (!t) return undefined
  if (!/^\d+$/.test(t)) return undefined
  return Number(t)
}

export function isWorkspaceViduModelName(name: string): boolean {
  const m = name.trim().toLowerCase()
  if (!m) return false
  return m.startsWith('viduq') || m.startsWith('vidu')
}

export function pickDefaultVideoForImageToVideo(options: string[], serverDefault: string): string {
  const viduFirst = options.find((id) => isWorkspaceViduModelName(id))
  if (viduFirst) return viduFirst
  if (options.includes('viduq3-turbo')) return 'viduq3-turbo'
  return serverDefault || options[0] || 'doubao-seedance-2.0'
}

export function isWorkspaceOneLinkSeedanceModel(name: string): boolean {
  const m = name.trim().toLowerCase()
  if (!m) return false
  return m === 'doubao-seedance-1.5-pro' || m === 'doubao-seedance-2.0'
}

export function emptyViduForm(): ViduFormState {
  return {
    duration: '',
    seed: '',
    resolution: '',
    movement_amplitude: '',
    audio: '',
    audio_type: '',
    off_peak: '',
    watermark: '',
    wm_position: '',
    wm_url: '',
    voice_id: '',
    is_rec: '',
    bgm: '',
    payload: '',
    meta_data: '',
    callback_url: '',
  }
}

function triToBool(s: ViduTri): boolean | undefined {
  if (s === '') return undefined
  return s === 'true'
}

export function buildVideoViduOptions(form: ViduFormState): VideoViduOptions | undefined {
  const o: VideoViduOptions = {}
  const du = form.duration.trim()
  if (du !== '' && !Number.isNaN(Number(du))) o.duration = Number(du)
  const sd = form.seed.trim()
  if (sd !== '' && !Number.isNaN(Number(sd))) o.seed = Number(sd)
  if (form.resolution.trim()) o.resolution = form.resolution.trim()
  if (form.movement_amplitude.trim()) o.movement_amplitude = form.movement_amplitude.trim()
  const a = triToBool(form.audio)
  if (a !== undefined) o.audio = a
  if (form.audio_type.trim()) o.audio_type = form.audio_type.trim()
  const op = triToBool(form.off_peak)
  if (op !== undefined) o.off_peak = op
  const wm = triToBool(form.watermark)
  if (wm !== undefined) o.watermark = wm
  const wp = form.wm_position.trim()
  if (wp !== '' && !Number.isNaN(Number(wp))) o.wm_position = Number(wp)
  if (form.wm_url.trim()) o.wm_url = form.wm_url.trim()
  if (form.voice_id.trim()) o.voice_id = form.voice_id.trim()
  const ir = triToBool(form.is_rec)
  if (ir !== undefined) o.is_rec = ir
  const bg = triToBool(form.bgm)
  if (bg !== undefined) o.bgm = bg
  if (form.payload.trim()) o.payload = form.payload.trim()
  if (form.meta_data.trim()) o.meta_data = form.meta_data.trim()
  if (form.callback_url.trim()) o.callback_url = form.callback_url.trim()
  return Object.keys(o).length > 0 ? o : undefined
}

export function emptyImageAdvancedForm(): ImageAdvancedFormState {
  return {
    reference2imageUrl: '',
    klingReferenceImages: ['', '', '', ''],
    outpaintSourceImageUrl: '',
    outpaintTop: '',
    outpaintRight: '',
    outpaintBottom: '',
    outpaintLeft: '',
    omniSourceImageUrl: '',
    omniMode: '',
    omniSubjectPrompt: '',
  }
}

export function normalizeReferenceImages(values: string[]): string[] {
  return values.map((item) => item.trim()).filter(Boolean)
}

export function isWorkspaceKlingMultiReferenceModelName(name: string): boolean {
  const m = name.trim().toLowerCase()
  if (!m) return false
  return m === 'video-kling-v3' || (m.includes('kling') && m.includes('multi') && m.includes('reference'))
}

export function isWorkspaceOutpaintModelName(name: string): boolean {
  const m = name.trim().toLowerCase()
  if (!m) return false
  return m.includes('outpaint') || m.includes('out-paint') || m.includes('expand')
}

export function isWorkspaceOmniModelName(name: string): boolean {
  const m = name.trim().toLowerCase()
  if (!m) return false
  return m.includes('omni')
}

export function resolveImageAdvancedCapability(modelName: string): ImageAdvancedCapability | undefined {
  const m = modelName.trim().toLowerCase()
  if (!m) return undefined
  if (isWorkspaceOutpaintModelName(m)) return 'outpaint'
  if (isWorkspaceOmniModelName(m)) return 'omni'
  if (isWorkspaceKlingMultiReferenceModelName(m)) return 'kling_multi_reference'
  if (m.startsWith('vidu') || m.includes('reference2image')) return 'vidu_reference2image'
  return undefined
}

export function labelForImageAdvancedCapability(capability: ImageAdvancedCapability | undefined): string {
  switch (capability) {
    case 'vidu_reference2image':
      return 'Vidu reference2image'
    case 'kling_multi_reference':
      return 'Kling 多图参考生图'
    case 'outpaint':
      return '扩图'
    case 'omni':
      return 'Omni'
    default:
      return ''
  }
}

export function buildImageAdvancedExtra(
  capability: ImageAdvancedCapability | undefined,
  form: ImageAdvancedFormState,
): GenerateAdvancedImageExtraRequest | undefined {
  if (!capability) return undefined
  if (capability === 'vidu_reference2image') {
    const ref = form.reference2imageUrl.trim()
    if (!ref) return undefined
    return {
      capability,
      reference2image: {
        referenceImageUrl: ref,
        images: [ref],
      },
    }
  }
  if (capability === 'kling_multi_reference') {
    const refs = normalizeReferenceImages(form.klingReferenceImages)
    if (refs.length === 0) return undefined
    return {
      capability,
      klingMultiReference: {
        referenceImageUrls: refs,
        images: refs,
      },
    }
  }
  if (capability === 'outpaint') {
    const sourceImageUrl = form.outpaintSourceImageUrl.trim()
    const top = parseNonNegativeIntegerText(form.outpaintTop)
    const right = parseNonNegativeIntegerText(form.outpaintRight)
    const bottom = parseNonNegativeIntegerText(form.outpaintBottom)
    const left = parseNonNegativeIntegerText(form.outpaintLeft)
    if (!sourceImageUrl && top == null && right == null && bottom == null && left == null) return undefined
    return {
      capability,
      outpaint: {
        sourceImageUrl: sourceImageUrl || undefined,
        image: sourceImageUrl || undefined,
        top,
        right,
        bottom,
        left,
      },
    }
  }
  const sourceImageUrl = form.omniSourceImageUrl.trim()
  const mode = form.omniMode.trim()
  const subjectPrompt = form.omniSubjectPrompt.trim()
  if (!sourceImageUrl && !mode && !subjectPrompt) return undefined
  return {
    capability,
    omni: {
      sourceImageUrl: sourceImageUrl || undefined,
      image: sourceImageUrl || undefined,
      mode: mode || undefined,
      subjectPrompt: subjectPrompt || undefined,
    },
  }
}

export function getWorkspaceAdvancedValidationError({
  needImg,
  needVid,
  imageAdvancedCapability,
  imageAdvancedForm,
  finalVideoModel,
  videoReferenceImageUrl,
  allowVideoFirstFrameImage,
}: WorkspaceAdvancedValidationParams): string | undefined {
  if (needImg && imageAdvancedCapability === 'vidu_reference2image') {
    if (!isValidWorkspaceReferenceImage(imageAdvancedForm.reference2imageUrl)) {
      return 'Vidu reference2image 需填写 1 张参考图：可访问的 http(s) 图片地址，或 data:image/...;base64,...'
    }
  }
  if (needImg && imageAdvancedCapability === 'kling_multi_reference') {
    const refs = normalizeReferenceImages(imageAdvancedForm.klingReferenceImages)
    if (refs.length < 2 || refs.length > 4) {
      return 'Kling 多图参考生图需填写 2~4 张参考图'
    }
    if (!refs.every((item) => isValidWorkspaceReferenceImage(item))) {
      return 'Kling 多图参考生图的参考图必须为可访问的 http(s) 图片地址，或 data:image/...;base64,...'
    }
  }
  if (needImg && imageAdvancedCapability === 'outpaint') {
    if (!isValidWorkspaceReferenceImage(imageAdvancedForm.outpaintSourceImageUrl)) {
      return '扩图需填写原图：可访问的 http(s) 图片地址，或 data:image/...;base64,...'
    }
    const outpaintFields = [
      imageAdvancedForm.outpaintTop,
      imageAdvancedForm.outpaintRight,
      imageAdvancedForm.outpaintBottom,
      imageAdvancedForm.outpaintLeft,
    ]
    if (!outpaintFields.every((item) => isNonNegativeIntegerText(item))) {
      return '扩图边距 top/right/bottom/left 仅支持非负整数'
    }
    const totalExpand =
      (parseNonNegativeIntegerText(imageAdvancedForm.outpaintTop) ?? 0) +
      (parseNonNegativeIntegerText(imageAdvancedForm.outpaintRight) ?? 0) +
      (parseNonNegativeIntegerText(imageAdvancedForm.outpaintBottom) ?? 0) +
      (parseNonNegativeIntegerText(imageAdvancedForm.outpaintLeft) ?? 0)
    if (totalExpand <= 0) {
      return '扩图至少需要填写一个大于 0 的扩边值'
    }
  }
  if (needImg && imageAdvancedCapability === 'omni') {
    if (!isValidWorkspaceReferenceImage(imageAdvancedForm.omniSourceImageUrl)) {
      return 'Omni 需填写输入图：可访问的 http(s) 图片地址，或 data:image/...;base64,...'
    }
    if (!imageAdvancedForm.omniSubjectPrompt.trim()) {
      return 'Omni 需填写主体描述'
    }
  }
  if (needVid && workspaceVideoNeedsFirstFrameImage(finalVideoModel)) {
    if (allowVideoFirstFrameImage !== true) {
      return '该视频模型为图生视频模型，请切换到「图生视频」入口'
    }
    if (!isValidWorkspaceReferenceImage(videoReferenceImageUrl)) {
      return '图生视频需填写参考图：可访问的 http(s) 图片地址，或 data:image/...;base64,...（Moark / Vidu）'
    }
  }
  return undefined
}

export function buildWorkspaceAdvancedMediaPayload({
  mode,
  imageAdvancedCapability,
  imageAdvancedForm,
  videoReferenceImageUrl,
  finalVideoModel,
  viduForm,
}: BuildWorkspaceAdvancedMediaPayloadParams): WorkspaceAdvancedMediaPayload {
  const needImg = mode === 'image' || mode === 'both'
  const needVid = mode === 'video'
  const imageExtra = needImg ? buildImageAdvancedExtra(imageAdvancedCapability, imageAdvancedForm) : undefined
  const trimmedVideoReferenceImageUrl = needVid ? videoReferenceImageUrl.trim() || undefined : undefined
  const videoViduOptions = needVid && isWorkspaceViduModelName(finalVideoModel) ? buildVideoViduOptions(viduForm) : undefined
  const oneLinkSeedanceExtra: GenerateAdvancedVideoOneLinkSeedanceExtraRequest | undefined =
    needVid && isWorkspaceOneLinkSeedanceModel(finalVideoModel)
      ? {
          referenceImageUrls: trimmedVideoReferenceImageUrl ? [trimmedVideoReferenceImageUrl] : undefined,
        }
      : undefined

  const advancedMedia =
    imageExtra || trimmedVideoReferenceImageUrl || videoViduOptions || oneLinkSeedanceExtra
      ? {
          image:
            imageExtra
              ? {
                  referenceImageUrl:
                    imageAdvancedCapability === 'vidu_reference2image'
                      ? imageAdvancedForm.reference2imageUrl.trim() || undefined
                      : undefined,
                  extra: imageExtra,
                }
              : undefined,
          video:
            needVid && (trimmedVideoReferenceImageUrl || videoViduOptions || oneLinkSeedanceExtra)
              ? {
                  referenceImageUrl: trimmedVideoReferenceImageUrl,
                  viduOptions: videoViduOptions,
                  extra: oneLinkSeedanceExtra,
                }
              : undefined,
        }
      : undefined

  return {
    advancedMedia,
    videoReferenceImageUrl: trimmedVideoReferenceImageUrl,
    videoViduOptions,
  }
}
