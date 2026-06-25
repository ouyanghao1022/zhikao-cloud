<template>
  <div class="page-container">
    <div class="page-header">
      <h2>📖 错题本</h2>
      <span class="hint" v-if="list.length">共 {{ total }} 道错题</span>
    </div>

    <el-tabs v-model="activeTab" class="wb-tabs">
      <!-- ============ Tab 1: 今日复习计划 ============ -->
      <el-tab-pane label="📅 今日复习计划" name="review">
        <el-card v-loading="reviewLoading">
          <template #header>
            <div class="review-header">
              <span>今日复习计划</span>
              <el-button size="small" @click="loadReviewPlan">刷新</el-button>
            </div>
          </template>

          <div v-if="reviewPlan" class="review-stats">
            <div class="stat-item">
              <span class="stat-num">{{ reviewPlan.totalCount ?? 0 }}</span>
              <span class="stat-label">应复习</span>
            </div>
            <div class="stat-item stat-ok">
              <span class="stat-num">{{ reviewPlan.doneCount ?? 0 }}</span>
              <span class="stat-label">已完成</span>
            </div>
            <div class="stat-item stat-err">
              <span class="stat-num">{{ reviewPlan.pendingCount ?? 0 }}</span>
              <span class="stat-label">待复习</span>
            </div>
          </div>

          <el-divider v-if="reviewPlan?.pendingNotes?.length" />

          <div v-if="reviewPlan?.pendingNotes?.length" class="review-list">
            <div v-for="note in reviewPlan.pendingNotes" :key="note.id" class="review-note-item">
              <div class="rn-left">
                <el-tag :type="typeTag(note.questionType)" size="small">{{ typeLabel(note.questionType) }}</el-tag>
                <span class="rn-title">{{ note.title }}</span>
              </div>
              <el-button type="primary" size="small" :loading="markingId === note.id" @click="markReviewed(note)">
                标记已复习
              </el-button>
            </div>
          </div>
          <el-empty v-else description="今日无待复习错题，继续保持！" />
        </el-card>
      </el-tab-pane>

      <!-- ============ Tab 2: 错题列表 ============ -->
      <el-tab-pane label="📚 错题列表" name="list">
        <el-card v-if="list.length > 0">
          <div class="filter-bar">
            <el-select v-model="masterFilter" placeholder="掌握状态" clearable style="width:140px" @change="loadData">
              <el-option label="未掌握" :value="0" />
              <el-option label="已掌握" :value="2" />
            </el-select>
          </div>

          <el-table :data="list" v-loading="loading" style="width:100%">
            <el-table-column type="index" label="序号" width="55" />
            <el-table-column label="题目内容" min-width="260">
              <template #default="{ row }">
                <el-button type="primary" link @click="openDetail(row)">{{ row.title }}</el-button>
              </template>
            </el-table-column>
            <el-table-column label="题型" width="70">
              <template #default="{ row }">
                <el-tag :type="typeTag(row.questionType)" size="small">{{ typeLabel(row.questionType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="你的答案" width="120">
              <template #default="{ row }">
                <span class="wrong-answer">{{ row.wrongAnswer || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="正确答案" width="120">
              <template #default="{ row }">
                <span class="correct-answer">{{ row.analysisLocked ? '🔒 未结束' : (row.correctAnswer || '-') }}</span>
              </template>
            </el-table-column>
            <el-table-column label="标签" min-width="160">
              <template #default="{ row }">
                <div class="note-tags-cell">
                  <el-tag v-for="t in noteTagsMap[row.id] || []" :key="t" size="small" style="margin-right:4px;margin-bottom:2px">
                    {{ t }}
                  </el-tag>
                  <el-popover trigger="click" :width="240" @show="onTagPopoverShow(row)">
                    <template #reference>
                      <el-button size="small" link type="primary">+ 打标签</el-button>
                    </template>
                    <div class="tag-pop">
                      <div class="tag-pop-title">给错题打标签</div>
                      <el-input v-model="tagInput" placeholder="多个标签用逗号分隔" size="small" @keyup.enter="doTagWrongNote(row)" />
                      <div class="tag-pop-btn">
                        <el-button type="primary" size="small" @click="doTagWrongNote(row)">保存</el-button>
                      </div>
                    </div>
                  </el-popover>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="掌握" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.masterStatus===2?'success':'warning'" size="small">
                  {{ row.masterStatus===2?'已掌握':'未掌握' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.masterStatus!==2" type="primary" link size="small" @click="markMastered(row)">
                  标记掌握
                </el-button>
                <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination v-if="total>0" v-model:current-page="current" v-model:page-size="size" :total="total"
            layout="total, prev, pager, next" @current-change="loadData" style="margin-top:16px;justify-content:flex-end" />
        </el-card>

        <el-empty v-else description="暂无错题记录">
          <el-button type="primary" @click="$router.push('/exam')">去参加考试</el-button>
        </el-empty>
      </el-tab-pane>
    </el-tabs>

    <!-- 题目详情弹窗 -->
    <el-dialog v-model="detailVisible" title="题目详情" width="620px" @closed="detailQuestion=null">
      <template v-if="detailQuestion">
        <div class="detail-section">
          <div class="detail-meta">
            <el-tag :type="typeTag(detailQuestion.questionType)" size="small">{{ typeLabel(detailQuestion.questionType) }}</el-tag>
            <el-tag :type="diffTag(detailQuestion.difficulty)" size="small" style="margin-left:8px">{{ diffLabel(detailQuestion.difficulty) }}</el-tag>
          </div>
          <div class="detail-title">{{ detailQuestion.title }}</div>
          <div class="detail-content" v-if="detailQuestion.content" v-html="detailQuestion.content"></div>
        </div>

        <!-- 选项（单选题/多选题） -->
        <div v-if="detailOptions.length > 0" class="detail-options">
          <div v-for="opt in detailOptions" :key="opt.optionLabel" class="option-item"
            :class="{ 'is-correct': opt.isCorrect, 'is-wrong-user': isWrongOption(opt.optionLabel) }">
            <span class="option-label">{{ opt.optionLabel }}.</span>
            <span class="option-text">{{ opt.optionContent }}</span>
            <el-tag v-if="opt.isCorrect" type="success" size="small" class="option-badge">正确答案</el-tag>
          </div>
        </div>

        <!-- 答案对比 -->
        <div class="answer-compare">
          <div class="answer-row wrong-row">
            <span class="answer-label">❌ 你的答案：</span>
            <span class="answer-value wrong-value">{{ detailRow?.wrongAnswer || '未作答' }}</span>
          </div>
          <div class="answer-row correct-row">
            <span class="answer-label">✅ 正确答案：</span>
            <span class="answer-value correct-value">{{ detailRow?.correctAnswer || '-' }}</span>
          </div>
        </div>

        <!-- 解析 -->
        <div v-if="detailQuestion.answerAnalysis" class="detail-analysis">
          <div class="analysis-title">📝 题目解析</div>
          <div class="analysis-content">{{ detailQuestion.answerAnalysis }}</div>
        </div>

        <!-- 错题标签（详情内） -->
        <div class="detail-tags">
          <div class="dt-title">🏷️ 错题标签</div>
          <div class="dt-tags">
            <el-tag v-for="t in detailNoteTags" :key="t" size="small" style="margin-right:4px;margin-bottom:4px">{{ t }}</el-tag>
            <span v-if="!detailNoteTags.length" class="dt-empty">暂无标签</span>
          </div>
          <div class="dt-input-row">
            <el-input v-model="detailTagInput" placeholder="多个标签用逗号分隔" size="small" style="width:300px" @keyup.enter="doDetailTag" />
            <el-button type="primary" size="small" @click="doDetailTag">打标签</el-button>
          </div>
        </div>
      </template>
      <div v-else v-loading="detailLoading" style="min-height:120px">
        <template v-if="!detailLoading && detailRow?.analysisLocked">
          <div class="detail-title">{{ detailRow?.title }}</div>
          <div class="answer-compare">
            <div class="answer-row wrong-row">
              <span class="answer-label">❌ 你的答案：</span>
              <span class="answer-value wrong-value">{{ detailRow?.wrongAnswer || '未作答' }}</span>
            </div>
          </div>
          <div class="detail-analysis" style="background:#fff7e6">
            <div class="analysis-title" style="color:#e6a23c">🔒 考试未结束</div>
            <div class="analysis-content">考试结束后方可查看正确答案与解析</div>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  getWrongBookList,
  updateMasterStatus,
  deleteWrongNote,
  getTodayReviewPlan,
  markWrongNoteReviewed,
  tagWrongNote,
  getWrongNoteTags
} from '@/api/wrongbook'
import { getQuestionDetail } from '@/api/question'
import { ElMessage, ElMessageBox } from 'element-plus'

// ==================== Tab 切换 ====================
const activeTab = ref('review')

// ==================== 今日复习计划 ====================
const reviewLoading = ref(false)
const reviewPlan = ref<any>(null)
const markingId = ref<number | null>(null)

async function loadReviewPlan() {
  reviewLoading.value = true
  try {
    const res: any = await getTodayReviewPlan()
    reviewPlan.value = res.data || null
  } catch (e) {
    reviewPlan.value = null
    ElMessage.error('加载复习计划失败')
  } finally {
    reviewLoading.value = false
  }
}

async function markReviewed(note: any) {
  markingId.value = note.id
  try {
    await markWrongNoteReviewed(note.id)
    ElMessage.success('已标记复习完成')
    await loadReviewPlan()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '操作失败')
  } finally {
    markingId.value = null
  }
}

// ==================== 错题列表 ====================
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const masterFilter = ref<number | undefined>(undefined)

// 每条错题的标签：noteId -> string[]
const noteTagsMap = reactive<Record<number, string[]>>({})
const tagInput = ref('')

async function loadNoteTags(noteId: number) {
  try {
    const res: any = await getWrongNoteTags(noteId)
    noteTagsMap[noteId] = res.data || []
  } catch {
    noteTagsMap[noteId] = []
  }
}

function onTagPopoverShow(row: any) {
  tagInput.value = (noteTagsMap[row.id] || []).join(', ')
  if (!noteTagsMap[row.id]) loadNoteTags(row.id)
}

async function doTagWrongNote(row: any) {
  const tags = tagInput.value.split(/[,，]/).map(s => s.trim()).filter(Boolean)
  if (!tags.length) {
    ElMessage.warning('请输入至少一个标签')
    return
  }
  try {
    await tagWrongNote(row.id, tags)
    ElMessage.success('标签已更新')
    await loadNoteTags(row.id)
    tagInput.value = ''
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '打标签失败')
  }
}

// ==================== 详情弹窗 ====================
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRow = ref<any>(null)
const detailQuestion = ref<any>(null)
const detailOptions = ref<any[]>([])
const detailNoteTags = ref<string[]>([])
const detailTagInput = ref('')

const typeMap: Record<number, string> = { 1: '单选', 2: '多选', 3: '填空', 4: '简答', 5: '判断' }
const diffMap: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }
function typeLabel(t: number) { return typeMap[t] || '未知' }
function typeTag(t: number) { return ['', '', 'success', 'warning', 'danger', 'info'][t] || '' }
function diffLabel(d: number) { return diffMap[d] || '未知' }
function diffTag(d: number) { return ['', 'success', 'warning', 'danger'][d] || '' }

async function loadData() {
  loading.value = true
  try {
    const params: any = { current: current.value, size: size.value }
    if (masterFilter.value !== undefined) params.masterStatus = masterFilter.value
    const res = await getWrongBookList(params)
    if (res.data) {
      list.value = res.data.records || []
      total.value = res.data.total || 0
      // 加载每条错题的标签
      list.value.forEach((row: any) => {
        loadNoteTags(row.id)
      })
    }
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function openDetail(row: any) {
  detailVisible.value = true
  detailRow.value = row
  detailLoading.value = true
  detailQuestion.value = null
  detailOptions.value = []
  detailNoteTags.value = []
  detailTagInput.value = ''
  try {
    if (!row.analysisLocked) {
      const res = await getQuestionDetail(row.questionId)
      if (res.data) {
        detailQuestion.value = res.data.question || null
        detailOptions.value = res.data.options || []
      }
    }
  } catch (e) {
    ElMessage.error('获取题目详情失败')
  } finally {
    detailLoading.value = false
  }
  // 并行加载错题标签
  try {
    const tres: any = await getWrongNoteTags(row.id)
    detailNoteTags.value = tres.data || []
    detailTagInput.value = detailNoteTags.value.join(', ')
  } catch {
    detailNoteTags.value = []
  }
}

async function doDetailTag() {
  if (!detailRow.value) return
  const tags = detailTagInput.value.split(/[,，]/).map(s => s.trim()).filter(Boolean)
  if (!tags.length) {
    ElMessage.warning('请输入至少一个标签')
    return
  }
  try {
    await tagWrongNote(detailRow.value.id, tags)
    ElMessage.success('标签已更新')
    detailNoteTags.value = tags
    noteTagsMap[detailRow.value.id] = tags
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '打标签失败')
  }
}

function isWrongOption(label: string): boolean {
  if (!detailRow.value) return false
  const ans = detailRow.value.wrongAnswer || ''
  const labels = ans.split(',').map((s: string) => s.trim())
  return labels.includes(label)
}

async function markMastered(row: any) {
  try {
    await updateMasterStatus(row.id, 2)
    ElMessage.success('已标记为掌握')
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除此错题记录？', '确认', { type: 'warning' })
    await deleteWrongNote(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* cancelled */ }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadReviewPlan()
  loadData()
})
</script>

<style scoped>
.page-container { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; }
.hint { color: #909399; font-size: 14px; }
.wb-tabs { background: #fff; border-radius: 8px; padding: 8px 16px; }

/* 复习计划 */
.review-header { display: flex; justify-content: space-between; align-items: center; }
.review-stats { display: flex; gap: 16px; margin-bottom: 8px; }
.stat-item { flex: 1; text-align: center; padding: 16px 12px; border-radius: 10px; background: #f5f7fa; }
.stat-ok { background: #f0f9eb; }
.stat-err { background: #fef0f0; }
.stat-num { display: block; font-size: 28px; font-weight: 700; color: #303133; }
.stat-ok .stat-num { color: #67c23a; }
.stat-err .stat-num { color: #f56c6c; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; display: block; }

.review-list { display: flex; flex-direction: column; gap: 8px; }
.review-note-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; background: #f5f7fa; border-radius: 8px; }
.rn-left { display: flex; align-items: center; gap: 8px; }
.rn-title { font-size: 14px; color: #303133; }

/* 标签单元格 */
.note-tags-cell { display: flex; flex-wrap: wrap; align-items: center; gap: 2px; }
.tag-pop { padding: 4px; }
.tag-pop-title { font-size: 13px; color: #303133; margin-bottom: 8px; }
.tag-pop-btn { margin-top: 8px; text-align: right; }

.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.wrong-answer { color: #e6a23c; font-weight: 500; }
.correct-answer { color: #67c23a; font-weight: 500; }

/* Detail dialog */
.detail-section { margin-bottom: 16px; }
.detail-meta { margin-bottom: 12px; }
.detail-title { font-size: 16px; font-weight: 600; line-height: 1.6; margin-bottom: 8px; }
.detail-content { color: #606266; line-height: 1.7; margin-bottom: 8px; }
.detail-options { margin: 12px 0; }
.option-item { padding: 8px 12px; margin-bottom: 6px; border-radius: 6px; background: #f5f7fa; display: flex; align-items: flex-start; gap: 8px; }
.option-item.is-correct { background: #f0f9eb; border: 1px solid #b3e19d; }
.option-item.is-wrong-user:not(.is-correct) { background: #fef0f0; border: 1px solid #fab6b6; }
.option-label { font-weight: 600; min-width: 24px; color: #409eff; }
.option-badge { flex-shrink: 0; margin-left: auto; }
.answer-compare { margin: 16px 0; padding: 12px; background: #f5f7fa; border-radius: 8px; }
.answer-row { display: flex; align-items: center; padding: 4px 0; }
.answer-label { font-size: 14px; min-width: 110px; }
.answer-value { font-size: 15px; font-weight: 600; }
.wrong-value { color: #e6a23c; }
.correct-value { color: #67c23a; }
.detail-analysis { margin-top: 16px; padding: 12px; background: #ecf5ff; border-radius: 8px; }
.analysis-title { font-weight: 600; margin-bottom: 6px; color: #409eff; }
.analysis-content { color: #606266; line-height: 1.7; white-space: pre-wrap; }

/* 详情内标签 */
.detail-tags { margin-top: 16px; padding: 12px; background: #fdf6ec; border-radius: 8px; }
.dt-title { font-weight: 600; margin-bottom: 8px; color: #e6a23c; }
.dt-tags { margin-bottom: 8px; }
.dt-empty { color: #909399; font-size: 13px; }
.dt-input-row { display: flex; gap: 8px; align-items: center; }
</style>
