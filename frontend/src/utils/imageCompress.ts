/**
 * 图片压缩工具 —— 使用浏览器原生 Canvas API
 *
 * 功能：
 *   1. 在用户选择图片后、正式上传服务器之前，自动对图片进行压缩处理
 *   2. 支持自定义目标大小（例如压缩后不超过 500KB）或自定义压缩质量（如 0.8）
 *   3. 支持自定义最大宽高限制（超过 1920×1080 自动等比缩放）
 *   4. 异步处理（async/await），提供 onProgress 回调用于展示 Loading 状态
 *   5. 输出标准的 File 或 Blob 对象，无缝对接现有上传接口
 *
 * 不引入任何第三方依赖，纯 Canvas 实现。
 */

export interface CompressOptions {
  /** 目标最大体积（字节），默认 500KB。压缩会从 quality 起步逐步降低质量直到满足或到达最低质量 */
  maxSize?: number
  /** 初始压缩质量 0~1，默认 0.8 */
  quality?: number
  /** 最低压缩质量 0~1，默认 0.3（低于此值不再降低，直接返回） */
  minQuality?: number
  /** 最大宽度，默认 1920 */
  maxWidth?: number
  /** 最大高度，默认 1080 */
  maxHeight?: number
  /** 输出类型，默认 image/jpeg（jpeg 压缩率最高） */
  mimeType?: string
  /** 进度回调，可用于 UI 展示 Loading */
  onProgress?: (msg: string) => void
}

/**
 * 从 File / Blob 加载为 HTMLImageElement
 */
function loadImage(file: File | Blob): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片加载失败'))
    }
    img.src = url
  })
}

/**
 * 计算等比缩放后的宽高
 */
function calcSize(origW: number, origH: number, maxW: number, maxH: number) {
  let w = origW
  let h = origH
  if (maxW > 0 && w > maxW) {
    h = Math.round((h * maxW) / w)
    w = maxW
  }
  if (maxH > 0 && h > maxH) {
    w = Math.round((w * maxH) / h)
    h = maxH
  }
  return { w, h }
}

/**
 * 将 canvas 转为 Blob
 */
function canvasToBlob(canvas: HTMLCanvasElement, quality: number, mimeType: string): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) resolve(blob)
        else reject(new Error('Canvas 转 Blob 失败'))
      },
      mimeType,
      quality
    )
  })
}

/**
 * 压缩图片核心函数
 *
 * @param file 原始 File 或 Blob
 * @param options 压缩选项
 * @returns 压缩后的 Blob
 */
export async function compressImage(
  file: File | Blob,
  options: CompressOptions = {}
): Promise<Blob> {
  const {
    maxSize = 500 * 1024,        // 默认 500KB
    quality = 0.8,                // 默认初始质量
    minQuality = 0.3,             // 默认最低质量
    maxWidth = 1920,              // 默认最大宽度
    maxHeight = 1080,             // 默认最大高度
    mimeType = 'image/jpeg',      // 默认输出 JPEG
    onProgress,
  } = options

  onProgress?.('正在加载图片…')
  const img = await loadImage(file)
  const origW = img.naturalWidth || img.width
  const origH = img.naturalHeight || img.height

  // 等比缩放
  const { w, h } = calcSize(origW, origH, maxWidth, maxHeight)

  onProgress?.('正在压缩图片…')
  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')!
  // 白底（处理 PNG 透明背景转 JPEG 变黑的问题）
  if (mimeType === 'image/jpeg') {
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, w, h)
  }
  ctx.drawImage(img, 0, 0, w, h)

  // 逐步降低质量直到满足目标大小
  let currentQuality = quality
  let blob = await canvasToBlob(canvas, currentQuality, mimeType)

  while (blob.size > maxSize && currentQuality > minQuality) {
    currentQuality = Math.max(minQuality, currentQuality - 0.1)
    onProgress?.(`正在压缩图片… ${(currentQuality * 100).toFixed(0)}%`)
    blob = await canvasToBlob(canvas, currentQuality, mimeType)
  }

  // 如果仍然超标且尺寸可以再降，进一步缩小宽高
  let finalW = w
  let finalH = h
  let safetyCount = 0
  while (blob.size > maxSize && safetyCount < 5) {
    safetyCount++
    finalW = Math.round(finalW * 0.8)
    finalH = Math.round(finalH * 0.8)
    const c2 = document.createElement('canvas')
    c2.width = finalW
    c2.height = finalH
    const ctx2 = c2.getContext('2d')!
    if (mimeType === 'image/jpeg') {
      ctx2.fillStyle = '#ffffff'
      ctx2.fillRect(0, 0, finalW, finalH)
    }
    ctx2.drawImage(img, 0, 0, finalW, finalH)
    blob = await canvasToBlob(c2, currentQuality, mimeType)
    onProgress?.(`正在缩小尺寸… ${finalW}×${finalH}`)
  }

  onProgress?.('压缩完成')
  return blob
}

/**
 * 压缩并返回 Base64 字符串
 */
export async function compressImageToBase64(
  file: File | Blob,
  options: CompressOptions = {}
): Promise<string> {
  const blob = await compressImage(file, options)
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
}

/**
 * 校验图片文件大小和类型
 *
 * @param file 文件
 * @param maxSize 最大字节数，默认 2MB
 * @param accept 允许的类型，默认 ['image/jpeg','image/png','image/bmp','image/webp']
 * @returns { ok: boolean, msg?: string }
 */
export function validateImage(
  file: File,
  maxSize: number = 2 * 1024 * 1024,
  accept: string[] = ['image/jpeg', 'image/png', 'image/bmp', 'image/webp']
): { ok: boolean; msg?: string } {
  if (!accept.includes(file.type)) {
    return { ok: false, msg: `仅支持 ${accept.map(t => t.split('/')[1].toUpperCase()).join('/')} 格式` }
  }
  if (file.size > maxSize) {
    const mb = (maxSize / 1024 / 1024).toFixed(0)
    return { ok: false, msg: `图片不能超过 ${mb}MB` }
  }
  return { ok: true }
}
