<template>
  <div class="favorite-page">
    <div class="page-header">
      <h2>我的收藏夹</h2>
    </div>

    <div class="favorite-body">
      <!-- 左侧收藏夹列表 -->
      <div class="folder-sidebar">
        <div class="folder-header">
          <span>收藏夹列表</span>
          <el-button type="primary" size="small" :icon="Plus" @click="showCreateDialog = true">
            新建
          </el-button>
        </div>
        <div class="folder-list" v-loading="folderLoading">
          <div
            v-for="folder in folders"
            :key="folder.id"
            class="folder-item"
            :class="{ active: selectedFolderId === folder.id }"
            @click="selectFolder(folder.id)"
          >
            <div class="folder-info">
              <span class="folder-icon">📁</span>
              <span class="folder-name">{{ folder.folderName }}</span>
            </div>
            <div class="folder-actions" @click.stop>
              <el-button :icon="Edit" size="small" text @click="editFolder(folder)" />
              <el-button :icon="Delete" size="small" text type="danger" @click="confirmDeleteFolder(folder)" />
            </div>
          </div>
          <div v-if="folders.length === 0 && !folderLoading" class="empty-tip">
            暂无收藏夹，点击新建创建
          </div>
        </div>
      </div>

      <!-- 右侧收藏项列表 -->
      <div class="items-content">
        <div class="items-header">
          <span>收藏内容</span>
          <el-button
            type="primary"
            size="small"
            :icon="Plus"
            :disabled="!selectedFolderId"
            @click="showAddDialog = true"
          >
            {{ selectedFolderId ? '添加收藏' : '请先选择左侧收藏夹' }}
          </el-button>
        </div>
        <el-table :data="items" v-loading="itemsLoading" style="width:100%" stripe>
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag :type="typeTagType(row.itemType)" size="small">
                {{ typeLabel(row.itemType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="itemTitle" label="标题" min-width="200">
            <template #default="{ row }">
              <span class="item-title" @click="viewDetail(row)">{{ row.itemTitle || '未命名' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="note" label="笔记" min-width="150">
            <template #default="{ row }">
              <div class="note-cell">
                <span v-if="!row.editing" class="note-text">{{ row.note || '点击添加笔记...' }}</span>
                <el-input
                  v-else
                  v-model="row.editNote"
                  size="small"
                  @blur="saveNote(row)"
                  @keyup.enter="saveNote(row)"
                />
                <el-button
                  :icon="Edit"
                  size="small"
                  text
                  @click="startEditNote(row)"
                  v-if="!row.editing"
                />
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="收藏时间" width="170">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button
                type="danger"
                size="small"
                link
                :icon="Delete"
                @click="confirmRemoveItem(row)"
              >
                取消收藏
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper" v-if="total > pageSize">
          <el-pagination
            v-model:current-page="currentPage"
            :total="total"
            :page-size="pageSize"
            layout="total, prev, pager, next"
            @current-change="loadItems"
          />
        </div>
      </div>
    </div>

    <!-- 创建/编辑收藏夹对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingFolder ? '编辑收藏夹' : '新建收藏夹'"
      width="420px"
      @closed="resetFolderForm"
    >
      <el-form :model="folderForm" label-width="70px">
        <el-form-item label="名称">
          <el-input v-model="folderForm.folderName" placeholder="输入收藏夹名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="folderForm.description"
            type="textarea"
            :rows="2"
            placeholder="输入描述（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="submitFolder" :loading="folderSubmitting">
          {{ editingFolder ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加收藏项对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="添加收藏"
      width="480px"
      @closed="resetItemForm"
    >
      <el-form :model="itemForm" label-width="70px">
        <el-form-item label="类型">
          <el-select v-model="itemForm.itemType" placeholder="选择类型">
            <el-option label="题目" :value="1" />
            <el-option label="试卷" :value="2" />
            <el-option label="学习资料" :value="3" />
            <el-option label="视频链接" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="itemForm.itemTitle" placeholder="输入标题" />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="itemForm.itemUrl" placeholder="输入链接地址（可选）" />
        </el-form-item>
        <el-form-item label="关联ID">
          <el-input-number v-model="itemForm.itemId" :min="0" controls-position="right" placeholder="题目/试卷ID（可选）" style="width:100%" />
        </el-form-item>
        <el-form-item label="笔记">
          <el-input
            v-model="itemForm.note"
            type="textarea"
            :rows="2"
            placeholder="添加笔记（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAddItem" :loading="itemSubmitting">
          添加
        </el-button>
      </template>
    </el-dialog>

    <!-- 题目详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="题目详情"
      width="680px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detailQuestion">
          <!-- 题目标签 -->
          <div class="detail-tags">
            <el-tag size="small" :type="detailTypeColor(detailQuestion.questionType)">
              {{ detailTypeLabel(detailQuestion.questionType) }}
            </el-tag>
            <el-tag size="small" effect="plain">{{ detailDiffLabel(detailQuestion.difficulty) }}</el-tag>
          </div>

          <!-- 题干 -->
          <div class="detail-title">{{ detailQuestion.title }}</div>

          <!-- 选项（单选/多选） -->
          <div v-if="detailOptions.length > 0" class="detail-options">
            <div v-for="opt in detailOptions" :key="opt.optionLabel" class="detail-option"
                 :class="{ 'is-correct': opt.isCorrect === 1 }">
              <span class="detail-opt-label">{{ opt.optionLabel }}</span>
              <span class="detail-opt-content">{{ opt.optionContent }}</span>
              <span v-if="opt.isCorrect === 1" class="detail-correct-tag">正确答案</span>
            </div>
          </div>

          <!-- 填空/简答/判断答案 -->
          <div v-else class="detail-answer">
            <span class="detail-answer-label">正确答案：</span>
            <el-tag type="success" size="large">{{ detailQuestion.answer }}</el-tag>
          </div>

          <!-- 解析 -->
          <div v-if="detailQuestion.answerAnalysis" class="detail-analysis">
            <div class="detail-analysis-label">解析</div>
            <div class="detail-analysis-text">{{ detailQuestion.answerAnalysis }}</div>
          </div>

          <!-- 收藏笔记 -->
          <div v-if="detailNote" class="detail-note">
            <div class="detail-note-label">我的笔记</div>
            <div class="detail-note-text">{{ detailNote }}</div>
          </div>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="goToPractice(detailItem)">去练习</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'
import {
  getFavoriteFolders,
  createFavoriteFolder,
  updateFavoriteFolder,
  deleteFavoriteFolder,
  getFavoriteItems,
  addFavoriteItem,
  removeFavoriteItem,
  updateFavoriteNote
} from '@/api/favorite'
import type { FavoriteFolder, FavoriteItem } from '@/api/favorite'
import dayjs from 'dayjs'

const router = useRouter()

// 题目详情弹窗
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailQuestion = ref<any>(null)
const detailOptions = ref<any[]>([])
const detailNote = ref('')
const detailItem = ref<any>(null)

// 收藏夹
const folders = ref<FavoriteFolder[]>([])
const folderLoading = ref(false)
const selectedFolderId = ref<number | undefined>(undefined)

// 收藏项
const items = ref<(FavoriteItem & { editing?: boolean; editNote?: string })[]>([])
const itemsLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 收藏夹对话框
const showCreateDialog = ref(false)
const editingFolder = ref<FavoriteFolder | null>(null)
const folderForm = ref({ folderName: '', description: '' })
const folderSubmitting = ref(false)

// 收藏项对话框
const showAddDialog = ref(false)
const itemForm = ref({
  itemType: 1,
  itemTitle: '',
  itemUrl: '',
  itemId: undefined as number | undefined,
  note: ''
})
const itemSubmitting = ref(false)

function typeTagType(type: number) {
  return ['', '', 'success', 'warning', 'danger'][type] || ''
}
function typeLabel(type: number) {
  return ['', '题目', '试卷', '资料', '链接'][type] || '其他'
}
function detailTypeLabel(t: number) { return ['', '单选', '多选', '判断', '填空', '简答'][t] || '' }
function detailTypeColor(t: number) { return ['', '', 'warning', 'success', 'info', 'danger'][t] || '' }
function detailDiffLabel(d: number) { return ['', '初', '中', '高'][d] || '' }
function formatDate(date: string) {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '--'
}

async function loadFolders() {
  folderLoading.value = true
  try {
    const res = await getFavoriteFolders()
    folders.value = res.data || []
    // 自动选中第一个收藏夹
    if (folders.value.length > 0 && !selectedFolderId.value) {
      selectFolder(folders.value[0].id)
    } else if (folders.value.length === 0) {
      // 自动创建默认收藏夹
      try {
        await createFavoriteFolder({ folderName: '默认收藏夹', description: '系统自动创建' })
        const res2 = await getFavoriteFolders()
        folders.value = res2.data || []
        if (folders.value.length > 0) {
          selectFolder(folders.value[0].id)
        }
      } catch { /* ignore */ }
    }
  } finally {
    folderLoading.value = false
  }
}

async function loadItems() {
  if (!selectedFolderId.value) {
    items.value = []
    return
  }
  itemsLoading.value = true
  try {
    const res = await getFavoriteItems({
      folderId: selectedFolderId.value,
      current: currentPage.value,
      size: pageSize.value
    })
    items.value = (res.data?.records || []).map((item: any) => ({
      ...item,
      editing: false,
      editNote: item.note || ''
    }))
    total.value = res.data?.total || 0
  } finally {
    itemsLoading.value = false
  }
}

function selectFolder(id: number) {
  selectedFolderId.value = id
  currentPage.value = 1
  loadItems()
}

// 收藏夹操作
function editFolder(folder: FavoriteFolder) {
  editingFolder.value = folder
  folderForm.value = { folderName: folder.folderName, description: folder.description || '' }
  showCreateDialog.value = true
}

function resetFolderForm() {
  editingFolder.value = null
  folderForm.value = { folderName: '', description: '' }
}

async function submitFolder() {
  if (!folderForm.value.folderName.trim()) {
    ElMessage.warning('请输入收藏夹名称')
    return
  }
  folderSubmitting.value = true
  try {
    if (editingFolder.value) {
      await updateFavoriteFolder(editingFolder.value.id, folderForm.value)
      ElMessage.success('更新成功')
    } else {
      await createFavoriteFolder(folderForm.value)
      ElMessage.success('创建成功')
    }
    showCreateDialog.value = false
    loadFolders()
  } finally {
    folderSubmitting.value = false
  }
}

async function confirmDeleteFolder(folder: FavoriteFolder) {
  try {
    await ElMessageBox.confirm(`确定要删除收藏夹「${folder.folderName}」吗？收藏项也将被删除。`, '删除确认', {
      type: 'warning'
    })
    await deleteFavoriteFolder(folder.id)
    ElMessage.success('删除成功')
    if (selectedFolderId.value === folder.id) {
      selectedFolderId.value = undefined
      items.value = []
    }
    loadFolders()
  } catch {
    // 取消
  }
}

// 收藏项操作
function resetItemForm() {
  itemForm.value = { itemType: 1, itemTitle: '', itemUrl: '', itemId: undefined, note: '' }
}

async function submitAddItem() {
  if (!itemForm.value.itemTitle.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  itemSubmitting.value = true
  try {
    await addFavoriteItem({
      folderId: selectedFolderId.value!,
      itemType: itemForm.value.itemType,
      itemTitle: itemForm.value.itemTitle,
      itemUrl: itemForm.value.itemUrl,
      itemId: (itemForm.value.itemId && itemForm.value.itemId > 0) ? itemForm.value.itemId : null,
      note: itemForm.value.note
    })
    ElMessage.success('收藏成功')
    showAddDialog.value = false
    loadItems()
  } finally {
    itemSubmitting.value = false
  }
}

async function confirmRemoveItem(item: any) {
  try {
    await ElMessageBox.confirm('确定取消收藏？', '提示', { type: 'warning' })
    await removeFavoriteItem(item.id)
    ElMessage.success('已取消收藏')
    loadItems()
  } catch {
    // 取消
  }
}

function startEditNote(item: any) {
  item.editing = true
  item.editNote = item.note || ''
}

async function saveNote(item: any) {
  item.editing = false
  const newNote = item.editNote || ''
  if (newNote === (item.note || '')) return
  try {
    await updateFavoriteNote(item.id, newNote)
    item.note = newNote
  } catch {
    // 失败回退
  }
}

/** 解析 content JSON 字段中的选项（Excel 导入题目） */
function parseContentOptions(content: string | undefined): any[] {
  if (!content) return []
  try {
    const raw = JSON.parse(content)
    if (!Array.isArray(raw)) return []
    return raw.map((opt: any) => ({
      optionLabel: opt.label || opt.optionLabel || '',
      optionContent: opt.content || opt.optionContent || '',
      isCorrect: 0
    }))
  } catch { return [] }
}

/** 点击标题 → 弹窗展示题目详情 */
async function viewDetail(item: any) {
  if (item.itemType !== 1 || !item.itemId) return

  detailItem.value = item
  detailVisible.value = true
  detailLoading.value = true
  detailQuestion.value = null
  detailOptions.value = []
  detailNote.value = item.note || ''

  try {
    const res = await request.get(`/question/detail/${item.itemId}`)
    const q = res.data?.question
    if (!q) { ElMessage.error('题目不存在'); detailVisible.value = false; return }

    detailQuestion.value = q

    // 优先从 question_option 表获取选项，为空则从 content JSON 解析
    let opts = res.data?.options || []
    if (opts.length === 0 && q.content) {
      opts = parseContentOptions(q.content)
      // 标记正确答案
      if (q.answer) {
        opts = opts.map((o: any) => ({
          ...o,
          isCorrect: o.optionLabel.trim() === (q.answer || '').trim() ? 1 : 0
        }))
      }
    }
    detailOptions.value = opts
  } catch {
    ElMessage.error('加载题目详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

/** 弹窗中「去练习」按钮 → 跳转到题库练习页面 */
function goToPractice(item: any) {
  detailVisible.value = false
  if (item.itemType === 1) {
    router.push('/question')
  }
}

onMounted(() => {
  loadFolders()
})
</script>

<style scoped>
.favorite-page { padding: 24px; max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 22px; }

.favorite-body { display: flex; gap: 20px; }

/* 左侧收藏夹 */
.folder-sidebar {
  width: 260px; flex-shrink: 0;
  background: #fff; border-radius: 10px; padding: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  max-height: 70vh; overflow-y: auto;
}
.folder-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 12px; font-weight: 600; font-size: 15px;
}
.folder-list { min-height: 100px; }
.folder-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 12px; border-radius: 8px; cursor: pointer;
  transition: all 0.2s; margin-bottom: 4px;
}
.folder-item:hover { background: #f5f7fa; }
.folder-item.active { background: #ecf5ff; color: #409EFF; }
.folder-info { display: flex; align-items: center; gap: 8px; flex: 1; overflow: hidden; }
.folder-icon { font-size: 18px; flex-shrink: 0; }
.folder-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }
.folder-actions { opacity: 0; transition: opacity 0.2s; }
.folder-item:hover .folder-actions { opacity: 1; }
.empty-tip { color: #909399; text-align: center; padding: 20px 0; font-size: 13px; }

/* 右侧内容 */
.items-content {
  flex: 1; min-width: 0;
  background: #fff; border-radius: 10px; padding: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.items-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; font-weight: 600; font-size: 15px;
}
.item-title { cursor: pointer; color: #409EFF; }
.item-title:hover { text-decoration: underline; }
.note-cell { display: flex; align-items: center; gap: 6px; }
.note-text { color: #909399; cursor: pointer; font-size: 13px; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 16px; }

/* 题目详情弹窗 */
.detail-body { min-height: 120px; }
.detail-tags { display: flex; gap: 8px; margin-bottom: 16px; }
.detail-title { font-size: 16px; color: #303133; line-height: 1.7; margin-bottom: 20px; white-space: pre-wrap; word-break: break-word; }
.detail-options { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.detail-option {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; border-radius: 8px;
  border: 1.5px solid #e8e8e8;
  background: #fafafa;
}
.detail-option.is-correct { border-color: #67c23a; background: #f0f9eb; }
.detail-opt-label {
  width: 28px; height: 28px; border-radius: 50%;
  background: #e8e8e8; display: flex; align-items: center;
  justify-content: center; font-size: 13px; font-weight: 600; flex-shrink: 0;
}
.detail-option.is-correct .detail-opt-label { background: #67c23a; color: #fff; }
.detail-opt-content { font-size: 14px; color: #606266; flex: 1; }
.detail-correct-tag { font-size: 12px; color: #67c23a; font-weight: 500; flex-shrink: 0; }
.detail-answer { margin-bottom: 16px; font-size: 15px; color: #303133; }
.detail-answer-label { font-weight: 500; margin-right: 8px; }
.detail-analysis { background: #f5f7fa; border-radius: 8px; padding: 14px; margin-bottom: 12px; }
.detail-analysis-label { font-weight: 600; font-size: 14px; color: #303133; margin-bottom: 6px; }
.detail-analysis-text { font-size: 13px; color: #606266; line-height: 1.6; white-space: pre-wrap; }
.detail-note { background: #fef9e7; border-radius: 8px; padding: 14px; }
.detail-note-label { font-weight: 600; font-size: 14px; color: #303133; margin-bottom: 6px; }
.detail-note-text { font-size: 13px; color: #606266; line-height: 1.6; }
</style>
