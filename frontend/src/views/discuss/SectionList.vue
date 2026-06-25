<template>
  <div class="section-list-page">
    <div class="page-header">
      <h2>讨论区</h2>
      <el-button type="primary" @click="$router.push('/discuss/create')">
        <el-icon><Edit /></el-icon> 发布新帖
      </el-button>
    </div>

    <!-- 版块卡片网格 -->
    <div class="section-grid" v-loading="loading">
      <div
        v-for="section in sections"
        :key="section.id"
        class="section-card"
        @click="$router.push(`/discuss/posts?sectionId=${section.id}`)"
      >
        <div class="section-icon">{{ getSectionIcon(section.sectionType) }}</div>
        <div class="section-info">
          <h3 class="section-name">{{ section.sectionName }}</h3>
          <p class="section-desc">{{ section.description || '暂无描述' }}</p>
          <div class="section-meta">
            <span>帖子 {{ section.postCount || 0 }}</span>
            <el-tag size="small" :type="getSectionTagType(section.sectionType)">
              {{ getSectionTypeName(section.sectionType) }}
            </el-tag>
          </div>
        </div>
      </div>
      <div v-if="sections.length === 0 && !loading" class="empty-tip">
        暂无版块
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSections } from '@/api/discuss'
import { Edit } from '@element-plus/icons-vue'

const loading = ref(false)
const sections = ref<any[]>([])

const sectionIcons: Record<number, string> = {
  1: '📚',
  2: '📝',
  3: '❓',
  4: '📅',
  5: '📢'
}

const sectionTypeNames: Record<number, string> = {
  1: '学科讨论',
  2: '考试交流',
  3: '题目求助',
  4: '学习打卡',
  5: '官方公告'
}

function getSectionIcon(type: number) {
  return sectionIcons[type] || '💬'
}

function getSectionTypeName(type: number) {
  return sectionTypeNames[type] || '其他'
}

function getSectionTagType(type: number) {
  const types: Record<number, string> = { 1: '', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }
  return types[type] || ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await getSections()
    sections.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.section-list-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h2 { font-size: 22px; color: #303133; }
.section-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 16px; }
.section-card {
  background: #fff; border-radius: 12px; padding: 24px; cursor: pointer;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06); display: flex; gap: 16px;
  transition: all 0.3s; align-items: flex-start;
}
.section-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.12); }
.section-icon { font-size: 36px; flex-shrink: 0; width: 56px; height: 56px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 12px; }
.section-info { flex: 1; min-width: 0; }
.section-name { font-size: 17px; color: #303133; margin-bottom: 6px; }
.section-desc { font-size: 13px; color: #909399; margin-bottom: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.section-meta { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #909399; }
.empty-tip { text-align: center; color: #909399; padding: 60px 0; grid-column: 1/-1; }
</style>
