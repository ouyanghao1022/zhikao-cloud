<template>
  <div class="post-list-page">
    <div class="page-header">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/discuss' }">讨论区</el-breadcrumb-item>
        <el-breadcrumb-item v-if="sectionName">{{ sectionName }}</el-breadcrumb-item>
        <el-breadcrumb-item>帖子列表</el-breadcrumb-item>
      </el-breadcrumb>
      <el-button type="primary" @click="$router.push('/discuss/create')">
        <el-icon><Edit /></el-icon> 发布新帖
      </el-button>
    </div>

    <!-- 搜索筛选 -->
    <div class="filter-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索帖子标题或内容..."
        clearable
        style="width: 320px"
        @keyup.enter="loadData"
        :prefix-icon="Search"
      />
      <el-select v-model="currentSectionId" placeholder="选择版块" clearable style="width: 180px" @change="loadData">
        <el-option
          v-for="s in sections"
          :key="s.id"
          :label="s.sectionName"
          :value="s.id"
        />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
    </div>

    <!-- 帖子列表 -->
    <div class="post-list" v-loading="loading">
      <div
        v-for="post in posts"
        :key="post.id"
        class="post-item"
        @click="$router.push(`/discuss/post/${post.id}`)"
      >
        <div class="post-left">
          <div class="post-stats">
            <div class="stat-item">
              <span class="stat-num">{{ post.viewCount || 0 }}</span>
              <span class="stat-label">浏览</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ post.likeCount || 0 }}</span>
              <span class="stat-label">点赞</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ post.commentCount || 0 }}</span>
              <span class="stat-label">评论</span>
            </div>
          </div>
        </div>
        <div class="post-main">
          <div class="post-title-row">
            <el-tag v-if="post.isTop" type="danger" size="small" effect="dark">置顶</el-tag>
            <el-tag v-if="post.isEssence" type="warning" size="small" effect="dark">精华</el-tag>
            <h3 class="post-title">{{ post.title }}</h3>
            <el-tag v-if="post.tags" size="small" type="info" class="post-tag">
              {{ post.tags }}
            </el-tag>
          </div>
          <p class="post-preview">{{ truncateContent(post.content, 120) }}</p>
          <div class="post-meta">
            <span class="meta-user">{{ post.nickname || post.username || '匿名用户' }}</span>
            <span class="meta-section" v-if="post.sectionName">{{ post.sectionName }}</span>
            <span class="meta-time">{{ formatTime(post.createdAt) }}</span>
          </div>
        </div>
      </div>
      <div v-if="posts.length === 0 && !loading" class="empty-tip">
        暂无帖子，快来发布第一个帖子吧
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPosts, getSections } from '@/api/discuss'
import { Search, Edit } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const route = useRoute()
const keyword = ref('')
const currentSectionId = ref<number | undefined>(undefined)
const sectionName = ref('')
const loading = ref(false)
const posts = ref<any[]>([])
const sections = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

function formatTime(time: string) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '--'
}

function truncateContent(content: string, len: number) {
  if (!content) return ''
  const text = content.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ')
  return text.length > len ? text.substring(0, len) + '...' : text
}

async function loadSections() {
  try {
    const res = await getSections()
    sections.value = res.data || []
  } catch { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    const sectionId = currentSectionId.value || (route.params.sectionId ? Number(route.params.sectionId) : undefined) || (route.query.sectionId ? Number(route.query.sectionId) : undefined)
    const res = await getPosts({
      sectionId,
      keyword: keyword.value || undefined,
      page: currentPage.value,
      size: pageSize.value
    })
    posts.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.params.sectionId) {
    currentSectionId.value = Number(route.params.sectionId)
  } else if (route.query.sectionId) {
    currentSectionId.value = Number(route.query.sectionId)
  }
  loadSections()
  loadData()
})
</script>

<style scoped>
.post-list-page { padding: 24px; max-width: 1000px; margin: 0 auto; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.post-list { background: var(--color-rice-card); border-radius: var(--radius-md); overflow: hidden; box-shadow: var(--shadow-md); }
.post-item {
  display: flex; padding: 16px 24px; border-bottom: 1px solid var(--color-rice-border); cursor: pointer;
  transition: background 0.2s;
}
.post-item:last-child { border-bottom: none; }
.post-item:hover { background: var(--color-rice-card); }
.post-left { margin-right: 20px; flex-shrink: 0; }
.post-stats { display: flex; gap: 16px; }
.stat-item { text-align: center; min-width: 48px; }
.stat-num { display: block; font-size: 16px; font-weight: 600; color: var(--color-ink); }
.stat-label { display: block; font-size: 11px; color: var(--color-ink-muted); margin-top: 2px; }
.post-main { flex: 1; min-width: 0; }
.post-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.post-title { font-size: 16px; color: var(--color-ink); margin: 0; }
.post-tag { flex-shrink: 0; }
.post-preview { font-size: 13px; color: var(--color-ink-muted); margin-bottom: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.post-meta { display: flex; gap: 16px; font-size: 12px; color: var(--color-ink-muted); }
.empty-tip { text-align: center; color: var(--color-ink-muted); padding: 60px 0; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 24px; }
</style>
