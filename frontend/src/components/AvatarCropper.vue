<template>
  <div class="avatar-cropper">
    <!-- 隐藏的文件输入 -->
    <input ref="fileInputRef" type="file" accept="image/jpeg,image/png,image/bmp,image/webp" style="display:none" @change="handleFileChange" />

    <!-- 选择文件区域 -->
    <div v-if="!imageSrc" class="select-area" @click="fileInputRef?.click()">
      <el-icon :size="32"><Plus /></el-icon>
      <span>点击选择图片</span>
      <p class="hint">JPG/PNG/BMP，不超过 2MB</p>
    </div>

    <!-- 裁剪区域 -->
    <div v-else class="crop-container">
      <div
        ref="cropAreaRef"
        class="crop-area"
        @mousedown="startDrag"
        @wheel.prevent="onWheel"
        @touchstart="startTouch"
        @touchmove="onTouchMove"
        @touchend="stopDrag"
      >
        <img
          :src="imageSrc"
          class="crop-img"
          :style="{
            transform: `translate(calc(-50% + ${offsetX}px), calc(-50% + ${offsetY}px)) scale(${scale})`,
          }"
          @dragstart.prevent
        />
        <!-- 1:1 裁剪框遮罩 -->
        <div class="crop-overlay"></div>
        <div class="crop-box">
          <div class="crop-grid"></div>
        </div>
      </div>

      <!-- 缩放滑块 -->
      <div class="zoom-bar">
        <el-icon @click="adjustZoom(-0.1)"><ZoomOut /></el-icon>
        <el-slider v-model="zoomPercent" :min="50" :max="300" :step="5" style="flex:1" @input="onSliderChange" />
        <el-icon @click="adjustZoom(0.1)"><ZoomIn /></el-icon>
      </div>

      <div class="action-bar">
        <el-button size="small" @click="resetCropper">重新选择</el-button>
        <el-button size="small" type="primary" :loading="cropping" @click="doCrop">
          <el-icon><Check /></el-icon> 确认裁剪
        </el-button>
      </div>
    </div>

    <!-- 进度提示 -->
    <div v-if="progressMsg" class="progress-tip">
      <el-icon class="is-loading"><Loading /></el-icon>
      {{ progressMsg }}
    </div>

    <!-- 预览 -->
    <div v-if="croppedPreview" class="preview-area">
      <p class="preview-label">裁剪预览</p>
      <img :src="croppedPreview" class="preview-img" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, ZoomIn, ZoomOut, Check, Loading } from '@element-plus/icons-vue'
import { compressImage, validateImage } from '@/utils/imageCompress'

const emit = defineEmits<{
  (e: 'cropped', blob: Blob, previewUrl: string): void
}>()

const fileInputRef = ref<HTMLInputElement | null>(null)
const cropAreaRef = ref<HTMLDivElement | null>(null)

const imageSrc = ref('')
const rawImage = ref<HTMLImageElement | null>(null)

// 交互状态
const offsetX = ref(0)
const offsetY = ref(0)
const scale = ref(1)
const zoomPercent = ref(100)
const cropping = ref(false)
const progressMsg = ref('')
const croppedPreview = ref('')

// 拖拽状态
let isDragging = false
let startX = 0
let startY = 0
let startOffsetX = 0
let startOffsetY = 0

// 触摸缩放
let lastTouchDist = 0

function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 校验
  const validation = validateImage(file, 2 * 1024 * 1024)
  if (!validation.ok) {
    ElMessage.error(validation.msg || '文件不合法')
    input.value = ''
    return
  }

  // 读取为 DataURL
  const reader = new FileReader()
  reader.onload = (ev) => {
    imageSrc.value = ev.target?.result as string
    croppedPreview.value = ''
    // 加载图片获取原始尺寸
    const img = new Image()
    img.onload = () => {
      rawImage.value = img
      // 自动适配初始缩放：让图片短边正好填满裁剪框
      fitInitialScale()
    }
    img.src = imageSrc.value
  }
  reader.readAsDataURL(file)
  input.value = ''
}

function fitInitialScale() {
  const cropArea = cropAreaRef.value
  if (!cropArea || !rawImage.value) return
  const areaSize = cropArea.clientWidth
  const img = rawImage.value
  // 取图片短边，让短边 = 裁剪框大小
  const shortSide = Math.min(img.naturalWidth, img.naturalHeight)
  if (shortSide > 0) {
    scale.value = areaSize / shortSide
    zoomPercent.value = Math.round(scale.value * 100)
  }
  offsetX.value = 0
  offsetY.value = 0
}

// ============ 拖拽移动 ============
function startDrag(e: MouseEvent) {
  isDragging = true
  startX = e.clientX
  startY = e.clientY
  startOffsetX = offsetX.value
  startOffsetY = offsetY.value
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', stopDrag)
}

function onDragMove(e: MouseEvent) {
  if (!isDragging) return
  offsetX.value = startOffsetX + (e.clientX - startX)
  offsetY.value = startOffsetY + (e.clientY - startY)
}

function stopDrag() {
  isDragging = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', stopDrag)
}

// ============ 触摸支持 ============
function startTouch(e: TouchEvent) {
  if (e.touches.length === 1) {
    isDragging = true
    startX = e.touches[0].clientX
    startY = e.touches[0].clientY
    startOffsetX = offsetX.value
    startOffsetY = offsetY.value
  } else if (e.touches.length === 2) {
    isDragging = false
    const dx = e.touches[0].clientX - e.touches[1].clientX
    const dy = e.touches[0].clientY - e.touches[1].clientY
    lastTouchDist = Math.sqrt(dx * dx + dy * dy)
  }
}

function onTouchMove(e: TouchEvent) {
  if (e.touches.length === 1 && isDragging) {
    e.preventDefault()
    offsetX.value = startOffsetX + (e.touches[0].clientX - startX)
    offsetY.value = startOffsetY + (e.touches[0].clientY - startY)
  } else if (e.touches.length === 2) {
    e.preventDefault()
    const dx = e.touches[0].clientX - e.touches[1].clientX
    const dy = e.touches[0].clientY - e.touches[1].clientY
    const dist = Math.sqrt(dx * dx + dy * dy)
    if (lastTouchDist > 0) {
      const delta = dist / lastTouchDist
      scale.value = Math.max(0.3, Math.min(5, scale.value * delta))
      zoomPercent.value = Math.round(scale.value * 100)
    }
    lastTouchDist = dist
  }
}

// ============ 滚轮缩放 ============
function onWheel(e: WheelEvent) {
  const delta = e.deltaY > 0 ? -0.05 : 0.05
  adjustZoom(delta)
}

// ============ 滑块缩放 ============
function onSliderChange(val: number) {
  scale.value = val / 100
}

function adjustZoom(delta: number) {
  scale.value = Math.max(0.3, Math.min(5, scale.value + delta))
  zoomPercent.value = Math.round(scale.value * 100)
}

// ============ 执行裁剪 ============
async function doCrop() {
  const cropArea = cropAreaRef.value
  const img = rawImage.value
  if (!cropArea || !img) return

  cropping.value = true
  progressMsg.value = '正在裁剪…'

  try {
    // 裁剪框大小 = 容器大小（1:1 正方形）
    const boxSize = cropArea.clientWidth
    const dpr = window.devicePixelRatio || 1
    const outputSize = Math.round(boxSize * dpr)  // 高清输出

    const canvas = document.createElement('canvas')
    canvas.width = outputSize
    canvas.height = outputSize
    const ctx = canvas.getContext('2d')!

    // 计算图片在裁剪框中的实际位置
    // img 经过 scale 变换后，translate(offsetX, offsetY)
    // 需要把裁剪框左上角对应的图片原始坐标算出来

    // 裁剪框中心点在容器坐标系中是 (boxSize/2, boxSize/2)
    // 图片中心点经过 translate 后在 (boxSize/2 + offsetX, boxSize/2 + offsetY)
    // 所以裁剪框中心相对于图片中心的偏移 = -offsetX, -offsetY
    // 转换回原始图片坐标：除以 scale
    const cropCenterX_orig = img.naturalWidth / 2 - offsetX.value / scale.value
    const cropCenterY_orig = img.naturalHeight / 2 - offsetY.value / scale.value

    // 裁剪框在原始图片中的大小
    const cropSize_orig = boxSize / scale.value

    // 源坐标
    const sx = cropCenterX_orig - cropSize_orig / 2
    const sy = cropCenterY_orig - cropSize_orig / 2
    const sw = cropSize_orig
    const sh = cropSize_orig

    // 白底（防止透明 PNG 变黑）
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, outputSize, outputSize)

    // 绘制
    ctx.drawImage(img, sx, sy, sw, sh, 0, 0, outputSize, outputSize)

    // 转 Blob
    const rawBlob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (b) => (b ? resolve(b) : reject(new Error('裁剪失败'))),
        'image/jpeg',
        0.92
      )
    })

    // 压缩（限制 500KB 以内，最大 512×512 足够）
    progressMsg.value = '正在压缩…'
    const compressedBlob = await compressImage(rawBlob, {
      maxSize: 500 * 1024,
      quality: 0.9,
      maxWidth: 512,
      maxHeight: 512,
      onProgress: (msg) => (progressMsg.value = msg),
    })

    // 生成预览
    croppedPreview.value = URL.createObjectURL(compressedBlob)

    progressMsg.value = ''
    ElMessage.success('裁剪完成')

    emit('cropped', compressedBlob, croppedPreview.value)
  } catch (err: any) {
    ElMessage.error(err?.message || '裁剪失败')
    progressMsg.value = ''
  } finally {
    cropping.value = false
  }
}

function resetCropper() {
  imageSrc.value = ''
  croppedPreview.value = ''
  rawImage.value = null
  offsetX.value = 0
  offsetY.value = 0
  scale.value = 1
  zoomPercent.value = 100
  progressMsg.value = ''
}

onMounted(() => {
  window.addEventListener('resize', fitInitialScale)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', fitInitialScale)
  stopDrag()
})
</script>

<style scoped>
.avatar-cropper { display: flex; flex-direction: column; align-items: center; gap: 14px; }

.select-area {
  width: 200px; height: 200px; border: 2px dashed #dcdfe6; border-radius: 12px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; cursor: pointer; transition: all 0.3s; color: #c0c4cc;
}
.select-area:hover { border-color: #409eff; color: #409eff; background: #f5f7fa; }
.select-area .hint { font-size: 11px; color: #c0c4cc; margin: 0; }

.crop-container { display: flex; flex-direction: column; align-items: center; gap: 12px; width: 100%; }

.crop-area {
  position: relative; width: 240px; height: 240px; overflow: hidden;
  border-radius: 8px; background: #1a1a1a; cursor: move; user-select: none;
}
.crop-img {
  position: absolute; top: 50%; left: 50%;
  transform-origin: center center;
  transform: translate(-50%, -50%);
  max-width: none; pointer-events: none;
}

/* 遮罩 + 1:1 裁剪框 */
.crop-overlay {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.5);
  pointer-events: none;
}
.crop-box {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 200px; height: 200px;
  border: 2px solid #fff;
  box-shadow: 0 0 0 9999px rgba(0,0,0,0.5);
  pointer-events: none;
}
.crop-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.3) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.3) 1px, transparent 1px);
  background-size: 33.33% 33.33%;
}

.zoom-bar {
  display: flex; align-items: center; gap: 8px; width: 240px;
}
.zoom-bar .el-icon { cursor: pointer; color: #606266; font-size: 16px; }
.zoom-bar .el-icon:hover { color: #409eff; }

.action-bar { display: flex; gap: 8px; }

.progress-tip {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: #409eff;
}

.preview-area { text-align: center; }
.preview-label { font-size: 12px; color: #909399; margin: 0 0 6px; }
.preview-img {
  width: 80px; height: 80px; border-radius: 8px;
  object-fit: cover; border: 2px solid #e4e7ed;
}
</style>
