import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildImageAdvancedExtra,
  buildWorkspaceAdvancedMediaPayload,
  emptyImageAdvancedForm,
  emptyViduForm,
  getWorkspaceAdvancedValidationError,
  resolveImageAdvancedCapability,
} from './advancedMedia.ts'

test('按模型名识别工作台图片高级能力', () => {
  assert.equal(resolveImageAdvancedCapability('vidu-reference2image-v1'), 'vidu_reference2image')
  assert.equal(resolveImageAdvancedCapability('video-kling-v3'), 'kling_multi_reference')
  assert.equal(resolveImageAdvancedCapability('seededit-outpaint'), 'outpaint')
  assert.equal(resolveImageAdvancedCapability('doubao-omni-gen'), 'omni')
  assert.equal(resolveImageAdvancedCapability('doubao-seedream-5-0-260128'), undefined)
})

test('Kling 多图参考字段会裁剪空白并序列化为后端兼容结构', () => {
  const form = {
    ...emptyImageAdvancedForm(),
    klingReferenceImages: [' https://img.example/a.png ', '', 'https://img.example/b.png', '   '],
  }

  assert.deepEqual(buildImageAdvancedExtra('kling_multi_reference', form), {
    capability: 'kling_multi_reference',
    klingMultiReference: {
      referenceImageUrls: ['https://img.example/a.png', 'https://img.example/b.png'],
      images: ['https://img.example/a.png', 'https://img.example/b.png'],
    },
  })
})

test('扩图校验会拦截无效边距，避免明显错误请求提交', () => {
  const form = {
    ...emptyImageAdvancedForm(),
    outpaintSourceImageUrl: 'https://img.example/source.png',
    outpaintTop: '0',
    outpaintRight: 'abc',
    outpaintBottom: '0',
    outpaintLeft: '0',
  }

  assert.equal(
    getWorkspaceAdvancedValidationError({
      needImg: true,
      needVid: false,
      imageAdvancedCapability: 'outpaint',
      imageAdvancedForm: form,
      finalVideoModel: '',
      videoReferenceImageUrl: '',
    }),
    '扩图边距 top/right/bottom/left 仅支持非负整数',
  )
})

test('Vidu reference2image 会序列化 advancedMedia.image.referenceImageUrl 与 extra', () => {
  const form = {
    ...emptyImageAdvancedForm(),
    reference2imageUrl: ' https://img.example/ref.png ',
  }

  assert.deepEqual(
    buildWorkspaceAdvancedMediaPayload({
      mode: 'image',
      imageAdvancedCapability: 'vidu_reference2image',
      imageAdvancedForm: form,
      videoReferenceImageUrl: '',
      finalVideoModel: '',
      viduForm: emptyViduForm(),
    }),
    {
      advancedMedia: {
        image: {
          referenceImageUrl: 'https://img.example/ref.png',
          extra: {
            capability: 'vidu_reference2image',
            reference2image: {
              referenceImageUrl: 'https://img.example/ref.png',
              images: ['https://img.example/ref.png'],
            },
          },
        },
        video: undefined,
      },
      videoReferenceImageUrl: undefined,
      videoViduOptions: undefined,
    },
  )
})

test('Vidu 图生视频会同时生成结构化 advancedMedia 与兼容旧字段', () => {
  const viduForm = {
    ...emptyViduForm(),
    duration: '5',
    audio: 'true',
    seed: '42',
  }

  assert.deepEqual(
    buildWorkspaceAdvancedMediaPayload({
      mode: 'video',
      imageAdvancedCapability: undefined,
      imageAdvancedForm: emptyImageAdvancedForm(),
      videoReferenceImageUrl: ' https://img.example/first-frame.png ',
      finalVideoModel: 'viduq3-standard',
      viduForm,
    }),
    {
      advancedMedia: {
        image: undefined,
        video: {
          referenceImageUrl: 'https://img.example/first-frame.png',
          viduOptions: {
            duration: 5,
            audio: true,
            seed: 42,
          },
        },
      },
      videoReferenceImageUrl: 'https://img.example/first-frame.png',
      videoViduOptions: {
        duration: 5,
        audio: true,
        seed: 42,
      },
    },
  )
})

test('非图生视频入口会拦截需参考图的视频模型并引导切换入口', () => {
  assert.equal(
    getWorkspaceAdvancedValidationError({
      needImg: false,
      needVid: true,
      imageAdvancedCapability: undefined,
      imageAdvancedForm: emptyImageAdvancedForm(),
      finalVideoModel: 'viduq3-standard',
      videoReferenceImageUrl: '',
      allowVideoFirstFrameImage: false,
    }),
    '该视频模型为图生视频模型，请切换到「图生视频」入口',
  )
})

test('图生视频入口在缺少参考图时会要求补齐参考图', () => {
  assert.equal(
    getWorkspaceAdvancedValidationError({
      needImg: false,
      needVid: true,
      imageAdvancedCapability: undefined,
      imageAdvancedForm: emptyImageAdvancedForm(),
      finalVideoModel: 'viduq3-standard',
      videoReferenceImageUrl: '',
      allowVideoFirstFrameImage: true,
    }),
    '图生视频需填写参考图：可访问的 http(s) 图片地址，或 data:image/...;base64,...（Moark / Vidu）',
  )
})
