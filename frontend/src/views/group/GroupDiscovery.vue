<template>
  <div class="group-discovery-page">
    <div class="page-header">
      <h2>学习小组</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon> 创建小组
        </el-button>
      </div>
    </div>

    <!-- 搜索与筛选 -->
    <el-card class="search-card" shadow="never">
      <div class="search-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索小组名称..."
          clearable
          style="width: 320px"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="filterJoinType" placeholder="加入方式" clearable style="width: 160px" @change="handleSearch">
          <el-option label="公开加入" :value="1" />
          <el-option label="审核加入" :value="2" />
          <el-option label="邀请加入" :value="3" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
    </el-card>

    <!-- 小组卡片列表 -->
    <div class="group-grid" v-loading="loading">
      <div v-for="group in groups" :key="group.id" class="group-card">
        <div class="group-card-header">
          <div class="group-cover" v-if="group.coverUrl">
            <img :src="group.coverUrl" alt="cover" />
          </div>
          <div class="group-cover-placeholder" v-else>
            <el-icon :size="40"><UserFilled /></el-icon>
          </div>
        </div>
        <div class="group-card-body">
          <div class="group-title-row">
            <h4 class="group-name">{{ group.groupName }}</h4>
            <el-tag
              :type="group.joinType === 1 ? 'success' : group.joinType === 2 ? 'warning' : 'info'"
              size="small"
            >
              {{ joinTypeMap[group.joinType] || '未知' }}
            </el-tag>
          </div>
          <p class="group-desc" v-if="group.description">{{ group.description }}</p>
          <div class="group-tags" v-if="group.tags">
            <el-tag
              v-for="(tag, idx) in group.tags.split(',')"
              :key="idx"
              size="small"
              class="tag-item"
            >
              {{ tag.trim() }}
            </el-tag>
          </div>
          <div class="group-meta">
            <span class="meta-item">
              <el-icon><User /></el-icon>
              {{ group.currentMembers || 0 }}/{{ group.maxMembers || 0 }}人
            </span>
            <span class="meta-item">
              <el-icon><Star /></el-icon>
              Lv.{{ group.level || 1 }}
            </span>
            <span class="meta-item">
              <el-icon><TrendCharts /></el-icon>
              活跃度 {{ group.activeScore || 0 }}
            </span>
          </div>
          <div class="group-actions">
            <el-button
              type="primary"
              size="small"
              @click="handleJoinGroup(group)"
              :disabled="group.currentMembers >= group.maxMembers"
            >
              {{ group.currentMembers >= group.maxMembers ? '已满员' : '加入小组' }}
            </el-button>
            <el-button size="small" @click="goToDetail(group.id)">查看详情</el-button>
          </div>
        </div>
      </div>
      <div v-if="groups.length === 0 && !loading" class="empty-tip">
        暂无小组，快来创建第一个学习小组吧
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 36]"
        layout="total, sizes, prev, pager, next"
        @change="loadGroups"
      />
    </div>

    <!-- 创建小组对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建学习小组" width="520px" :close-on-click-modal="false">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="小组名称" required>
          <el-input v-model="createForm.groupName" placeholder="请输入小组名称" maxlength="20" />
        </el-form-item>
        <el-form-item label="小组简介">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="介绍一下小组的学习目标..."
            maxlength="200"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="createForm.tags" placeholder="例：考研,英语,高数（逗号分隔）" maxlength="100" />
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="createForm.maxMembers" :min="10" :max="500" :step="10" />
        </el-form-item>
        <el-form-item label="加入方式">
          <el-radio-group v-model="createForm.joinType">
            <el-radio :value="1">公开加入</el-radio>
            <el-radio :value="2">审核加入</el-radio>
            <el-radio :value="3">邀请加入</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateGroup">创建小组</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getGroupList, createGroup, joinGroup } from '@/api/group'
import { Plus, Search, UserFilled, User, Star, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const creating = ref(false)
const groups = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const searchKeyword = ref('')
const filterJoinType = ref<number | undefined>(undefined)
const showCreateDialog = ref(false)

const joinTypeMap: Record<number, string> = { 1: '公开', 2: '审核', 3: '邀请' }

const createForm = ref({
  groupName: '',
  description: '',
  tags: '',
  maxMembers: 200,
  joinType: 1
})

async function loadGroups() {
  loading.value = true
  try {
    const res = await getGroupList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      joinType: filterJoinType.value
    })
    if (res.data) {
      groups.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadGroups()
}

async function handleCreateGroup() {
  if (!createForm.value.groupName.trim()) {
    ElMessage.warning('请输入小组名称')
    return
  }
  creating.value = true
  try {
    await createGroup(createForm.value)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.value = { groupName: '', description: '', tags: '', maxMembers: 200, joinType: 1 }
    await loadGroups()
  } catch { /* error handled by interceptor */ } finally {
    creating.value = false
  }
}

async function handleJoinGroup(group: any) {
  try {
    await joinGroup(group.id)
    ElMessage.success('加入成功')
    await loadGroups()
  } catch { /* error handled by interceptor */ }
}

function goToDetail(id: number) {
  router.push(`/group/${id}`)
}

onMounted(loadGroups)
</script>

<style scoped>
.group-discovery-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
.header-actions { display: flex; gap: 12px; }

.search-card { margin-bottom: 20px; }
.search-card :deep(.el-card__body) { padding: 16px 20px; }
.search-row { display: flex; gap: 12px; align-items: center; }

.group-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 20px; min-height: 200px; }

.group-card {
  background: var(--color-rice-card); border-radius: var(--radius-md); overflow: hidden;
  border: 1px solid var(--color-rice-border); border-left: 4px solid #7c3aed; transition: all 0.3s;
}
.group-card:hover { box-shadow: var(--shadow-md); border-color: var(--color-primary); transform: translateY(-2px); }

.group-card-header {
  height: 120px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.group-cover img { width: 100%; height: 120px; object-fit: cover; }
.group-cover-placeholder { color: rgba(255,255,255,0.6); }

.group-card-body {
  padding: 16px;
}
.group-title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.group-name { font-size: 16px; color: var(--color-ink); margin: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.group-desc {
  font-size: 13px; color: var(--color-ink-muted); margin: 0 0 12px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; line-height: 1.5;
}
.group-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.tag-item { max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.group-meta { display: flex; gap: 16px; font-size: 13px; color: var(--color-ink-muted); margin-bottom: 14px; }
.meta-item { display: flex; align-items: center; gap: 4px; }
.group-actions { display: flex; gap: 8px; }

.empty-tip { text-align: center; color: var(--color-ink-muted); padding: 60px 0; grid-column: 1/-1; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 24px; }
</style>
