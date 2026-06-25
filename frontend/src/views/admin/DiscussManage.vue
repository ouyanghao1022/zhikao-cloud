<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>💬 讨论区管理</h2>
      <el-button @click="loadAll" :loading="loading"><el-icon><Refresh /></el-icon> 刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ========== Tab 1: 概览统计 ========== -->
      <el-tab-pane label="📊 概览统计" name="stats">
        <el-row :gutter="16" v-loading="loading">
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon" style="background:#409eff">📝</div>
              <div class="stat-info">
                <div class="stat-num">{{ stats.postStats?.total || 0 }}</div>
                <div class="stat-label">帖子总数</div>
                <div class="stat-sub">正常 {{ stats.postStats?.normal || 0 }} · 已删 {{ stats.postStats?.deleted || 0 }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon" style="background:#67c23a">💬</div>
              <div class="stat-info">
                <div class="stat-num">{{ stats.commentStats?.total || 0 }}</div>
                <div class="stat-label">评论总数</div>
                <div class="stat-sub">正常 {{ stats.commentStats?.active || 0 }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon" style="background:#e6a23c">📂</div>
              <div class="stat-info">
                <div class="stat-num">{{ stats.sectionStats?.total || 0 }}</div>
                <div class="stat-label">版块总数</div>
                <div class="stat-sub">启用 {{ stats.sectionStats?.active || 0 }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon" style="background:#f56c6c">⏳</div>
              <div class="stat-info">
                <div class="stat-num">{{ stats.postStats?.pending || 0 }}</div>
                <div class="stat-label">待审核帖子</div>
                <div class="stat-sub">置顶 {{ stats.postStats?.topped || 0 }} · 精华 {{ stats.postStats?.essenced || 0 }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never" style="margin-top:20px">
          <template #header><span>📋 待审核帖子</span></template>
          <el-table :data="pendingPosts" size="small" v-loading="loading">
            <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="nickname" label="发帖人" width="100" />
            <el-table-column prop="sectionName" label="版块" width="100" />
            <el-table-column prop="createdAt" label="发布时间" width="160" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="success" link size="small" @click="handleAudit(row, 1)">通过</el-button>
                <el-button type="danger" link size="small" @click="handleAudit(row, 2)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!pendingPosts.length && !loading" description="暂无待审核帖子" />
        </el-card>
      </el-tab-pane>

      <!-- ========== Tab 2: 版块管理 ========== -->
      <el-tab-pane label="📂 版块管理" name="sections">
        <div style="margin-bottom:12px">
          <el-button type="primary" @click="openSectionDialog()"><el-icon><Plus /></el-icon> 新增版块</el-button>
        </div>
        <el-table :data="sections" v-loading="loading" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="sectionName" label="版块名称" min-width="120" />
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">{{ sectionTypeText(row.sectionType) }}</template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="70" align="center" />
          <el-table-column prop="postCount" label="帖子数" width="80" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status===1?'success':'danger'" size="small">{{ row.status===1?'正常':'禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openSectionDialog(row)">编辑</el-button>
              <el-button :type="row.status===1?'warning':'success'" link size="small" @click="toggleSectionStatus(row)">
                {{ row.status===1?'禁用':'启用' }}
              </el-button>
              <el-button type="danger" link size="small" @click="deleteSection(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 版块编辑弹窗 -->
        <el-dialog v-model="sectionDialogVisible" :title="editingSection.id?'编辑版块':'新增版块'" width="480px">
          <el-form :model="editingSection" label-width="80px">
            <el-form-item label="版块名称" required>
              <el-input v-model="editingSection.sectionName" placeholder="请输入版块名称" />
            </el-form-item>
            <el-form-item label="版块类型">
              <el-select v-model="editingSection.sectionType" style="width:100%">
                <el-option v-for="t in sectionTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="editingSection.description" type="textarea" :rows="2" placeholder="版块描述" />
            </el-form-item>
            <el-form-item label="图标">
              <el-input v-model="editingSection.icon" placeholder="图标（emoji或URL）" style="width:200px" />
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="editingSection.sort" :min="0" :max="999" />
            </el-form-item>
            <el-form-item label="状态">
              <el-radio-group v-model="editingSection.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="sectionDialogVisible=false">取消</el-button>
            <el-button type="primary" @click="saveSection">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- ========== Tab 3: 帖子管理 ========== -->
      <el-tab-pane label="📝 帖子管理" name="posts">
        <div class="filter-bar">
          <el-input v-model="postFilter.keyword" placeholder="搜索标题/内容" clearable style="width:200px" @keyup.enter="loadPosts" />
          <el-select v-model="postFilter.sectionId" placeholder="版块" clearable style="width:130px" @change="loadPosts">
            <el-option v-for="s in sections" :key="s.id" :label="s.sectionName" :value="s.id" />
          </el-select>
          <el-select v-model="postFilter.status" placeholder="状态" clearable style="width:110px" @change="loadPosts">
            <el-option label="正常" :value="1" />
            <el-option label="已删除" :value="0" />
          </el-select>
          <el-select v-model="postFilter.isTop" placeholder="置顶" clearable style="width:100px" @change="loadPosts">
            <el-option label="已置顶" :value="1" />
            <el-option label="未置顶" :value="0" />
          </el-select>
          <el-select v-model="postFilter.isEssence" placeholder="精华" clearable style="width:100px" @change="loadPosts">
            <el-option label="已加精" :value="1" />
            <el-option label="未加精" :value="0" />
          </el-select>
          <el-select v-model="postFilter.auditStatus" placeholder="审核" clearable style="width:110px" @change="loadPosts">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="未通过" :value="2" />
          </el-select>
          <el-button type="primary" @click="loadPosts">搜索</el-button>
          <el-button type="danger" :disabled="!selectedPostIds.length" @click="batchDelete">
            批量删除 ({{ selectedPostIds.length }})
          </el-button>
        </div>

        <el-table :data="posts" v-loading="loading" stripe @selection-change="onSelectionChange">
          <el-table-column type="selection" width="40" />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag v-if="row.isTop" type="danger" size="small" style="margin-right:4px">置顶</el-tag>
              <el-tag v-if="row.isEssence" type="warning" size="small" style="margin-right:4px">精华</el-tag>
              {{ row.title }}
            </template>
          </el-table-column>
          <el-table-column prop="nickname" label="发帖人" width="90" show-overflow-tooltip />
          <el-table-column prop="sectionName" label="版块" width="90" show-overflow-tooltip />
          <el-table-column label="浏览" width="60" align="center">
            <template #default="{ row }">{{ row.viewCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="赞" width="50" align="center">
            <template #default="{ row }">{{ row.likeCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="评" width="50" align="center">
            <template #default="{ row }">{{ row.commentCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status===1?'success':'info'" size="small">{{ row.status===1?'正常':'已删' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isAuditPassed===1?'success':row.isAuditPassed===2?'danger':'warning'" size="small">
                {{ row.isAuditPassed===1?'通过':row.isAuditPassed===2?'拒绝':'待审' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="发布时间" width="150" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="router.push(`/discuss/post/${row.id}`)">查看</el-button>
              <el-button v-if="row.status===1" :type="row.isTop?'warning':''" link size="small" @click="toggleTop(row)">
                {{ row.isTop?'取消置顶':'置顶' }}
              </el-button>
              <el-button v-if="row.status===1" :type="row.isEssence?'warning':''" link size="small" @click="toggleEssence(row)">
                {{ row.isEssence?'取消精华':'加精' }}
              </el-button>
              <el-button v-if="row.status===0" type="success" link size="small" @click="restorePost(row)">恢复</el-button>
              <el-button v-if="row.status===1" type="danger" link size="small" @click="deletePost(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination v-if="postTotal>0" v-model:current-page="postFilter.page" v-model:page-size="postFilter.size"
          :total="postTotal" layout="total, prev, pager, next" @current-change="loadPosts"
          style="margin-top:16px;justify-content:flex-end" />
      </el-tab-pane>

      <!-- ========== Tab 4: 评论管理 ========== -->
      <el-tab-pane label="💬 评论管理" name="comments">
        <div class="filter-bar">
          <el-input v-model="commentFilter.keyword" placeholder="搜索评论内容" clearable style="width:220px" @keyup.enter="loadComments" />
          <el-select v-model="commentFilter.status" placeholder="状态" clearable style="width:120px" @change="loadComments">
            <el-option label="正常" :value="1" />
            <el-option label="已删除" :value="0" />
          </el-select>
          <el-button type="primary" @click="loadComments">搜索</el-button>
        </div>

        <el-table :data="comments" v-loading="loading" stripe>
          <el-table-column prop="content" label="评论内容" min-width="250" show-overflow-tooltip />
          <el-table-column prop="username" label="评论人" width="100" />
          <el-table-column prop="post_title" label="所属帖子" min-width="180" show-overflow-tooltip />
          <el-table-column label="赞" width="50" align="center">
            <template #default="{ row }">{{ row.like_count || 0 }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status===1?'success':'info'" size="small">{{ row.status===1?'正常':'已删' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="created_at" label="评论时间" width="150" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status===1" type="danger" link size="small" @click="deleteComment(row)">删除</el-button>
              <span v-else style="color:#909399;font-size:12px">—</span>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination v-if="commentTotal>0" v-model:current-page="commentFilter.page" v-model:page-size="commentFilter.size"
          :total="commentTotal" layout="total, prev, pager, next" @current-change="loadComments"
          style="margin-top:16px;justify-content:flex-end" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  getDiscussStats, adminGetPosts, adminGetComments,
  createSection, updateSection, deleteSection as delSection,
  auditPost, restorePost as restorePostApi,
  toggleEssence as toggleEssenceApi,
  deletePost as delPostApi, batchDeletePosts,
} from '@/api/discuss'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('stats')

// 统计
const stats = ref<any>({})
const pendingPosts = ref<any[]>([])

// 版块
const sections = ref<any[]>([])
const sectionDialogVisible = ref(false)
const editingSection = reactive<any>({})

// 帖子
const posts = ref<any[]>([])
const postTotal = ref(0)
const postFilter = reactive({ keyword:'', sectionId:undefined as number|undefined, status:undefined as number|undefined, isTop:undefined as number|undefined, isEssence:undefined as number|undefined, auditStatus:undefined as number|undefined, page:1, size:10 })
const selectedPostIds = ref<number[]>([])

// 评论
const comments = ref<any[]>([])
const commentTotal = ref(0)
const commentFilter = reactive({ keyword:'', status:undefined as number|undefined, page:1, size:10 })

const sectionTypes = [
  { value:1, label:'学科讨论' },
  { value:2, label:'考试交流' },
  { value:3, label:'题目求助' },
  { value:4, label:'学习打卡' },
  { value:5, label:'官方公告' },
]
function sectionTypeText(t: number) {
  return sectionTypes.find(s => s.value === t)?.label || '其他'
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadStats(), loadSections()])
    if (activeTab.value === 'posts') await loadPosts()
    if (activeTab.value === 'comments') await loadComments()
  } finally { loading.value = false }
}

async function loadStats() {
  try {
    const res = await getDiscussStats()
    stats.value = res.data || {}
    // 加载待审核帖子
    const pendingRes = await adminGetPosts({ auditStatus: 0, page:1, size:5 })
    pendingPosts.value = pendingRes.data?.records || []
  } catch { /* ignore */ }
}

async function loadSections() {
  try {
    // 管理端获取所有版块（含禁用）
    const res = await request.get('/discuss/sections', { params: { all: true } })
    sections.value = res.data || []
  } catch { sections.value = [] }
}

async function loadPosts() {
  loading.value = true
  try {
    const res = await adminGetPosts(postFilter)
    posts.value = res.data?.records || []
    postTotal.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function loadComments() {
  loading.value = true
  try {
    const res = await adminGetComments(commentFilter)
    comments.value = res.data?.records || []
    commentTotal.value = res.data?.total || 0
  } finally { loading.value = false }
}

function onTabChange(tab: string) {
  if (tab === 'stats') loadStats()
  else if (tab === 'sections') loadSections()
  else if (tab === 'posts') loadPosts()
  else if (tab === 'comments') loadComments()
}

// ============ 版块管理 ============
function openSectionDialog(row?: any) {
  if (row) {
    Object.assign(editingSection, row)
  } else {
    Object.assign(editingSection, { id:null, sectionName:'', description:'', sectionType:1, icon:'', sort:0, status:1 })
  }
  sectionDialogVisible.value = true
}

async function saveSection() {
  if (!editingSection.sectionName?.trim()) { ElMessage.warning('请输入版块名称'); return }
  try {
    if (editingSection.id) {
      await updateSection(editingSection.id, editingSection)
    } else {
      await createSection(editingSection)
    }
    ElMessage.success('保存成功')
    sectionDialogVisible.value = false
    loadSections()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  }
}

async function toggleSectionStatus(row: any) {
  try {
    await request.put(`/discuss/admin/section/${row.id}/status`, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    loadSections()
  } catch { ElMessage.error('操作失败') }
}

async function deleteSection(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除版块「${row.sectionName}」？该版块下的帖子不受影响但将失去版块归属。`, '确认', { type:'warning' })
    await delSection(row.id)
    ElMessage.success('已删除')
    loadSections()
  } catch {}
}

// ============ 帖子管理 ============
function onSelectionChange(rows: any[]) {
  selectedPostIds.value = rows.map(r => r.id)
}

async function toggleTop(row: any) {
  try {
    await request.put(`/discuss/post/${row.id}/top`, { isTop: !row.isTop })
    ElMessage.success(row.isTop ? '已取消置顶' : '已置顶')
    loadPosts()
  } catch { ElMessage.error('操作失败') }
}

async function toggleEssence(row: any) {
  try {
    await toggleEssenceApi(row.id, !row.isEssence)
    ElMessage.success(row.isEssence ? '已取消精华' : '已加精')
    loadPosts()
  } catch { ElMessage.error('操作失败') }
}

async function deletePost(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除帖子「${row.title}」？`, '确认', { type:'warning' })
    await delPostApi(row.id)
    ElMessage.success('已删除')
    loadPosts()
  } catch {}
}

async function restorePost(row: any) {
  try {
    await restorePostApi(row.id)
    ElMessage.success('已恢复')
    loadPosts()
  } catch { ElMessage.error('操作失败') }
}

async function batchDelete() {
  try {
    await ElMessageBox.confirm(`确定批量删除 ${selectedPostIds.value.length} 条帖子？`, '确认', { type:'warning' })
    await batchDeletePosts(selectedPostIds.value)
    ElMessage.success('批量删除成功')
    selectedPostIds.value = []
    loadPosts()
  } catch {}
}

async function handleAudit(row: any, result: number) {
  try {
    let msg = ''
    if (result === 2) {
      const { value } = await ElMessageBox.prompt('请输入拒绝原因', '审核拒绝', { inputPlaceholder:'拒绝原因（可选）' })
      msg = value || ''
    }
    await auditPost(row.id, { isAuditPassed: result, auditMsg: msg })
    ElMessage.success(result === 1 ? '已通过审核' : '已拒绝')
    loadStats()
  } catch {}
}

// ============ 评论管理 ============
async function deleteComment(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该评论？', '确认', { type:'warning' })
    await request.delete(`/discuss/comment/${row.id}`)
    ElMessage.success('已删除')
    loadComments()
  } catch {}
}

onMounted(loadAll)
</script>

<style scoped>
.admin-page { padding: 24px; max-width: 1400px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }

.stat-card { display: flex; align-items: center; padding: 20px; }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 26px; margin-right: 16px; flex-shrink: 0; }
.stat-info { flex: 1; }
.stat-num { font-size: 28px; font-weight: 700; color: #303133; line-height: 1.2; }
.stat-label { font-size: 14px; color: #606266; margin-top: 4px; }
.stat-sub { font-size: 12px; color: #909399; }
</style>
