<template>
  <div class="post-detail-page">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/discuss' }">讨论区</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/discuss/posts' }">帖子列表</el-breadcrumb-item>
      <el-breadcrumb-item>帖子详情</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-loading="loading" class="post-container">
      <!-- 帖子内容 -->
      <div v-if="post" class="post-main-card">
        <div class="post-header">
          <div class="post-tags">
            <el-tag v-if="post.isTop" type="danger" size="small" effect="dark">置顶</el-tag>
            <el-tag v-if="post.isEssence" type="warning" size="small" effect="dark">精华</el-tag>
            <el-tag v-if="post.tags" size="small" type="info">{{ post.tags }}</el-tag>
          </div>
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="post-author">
            <span class="author-name">{{ post.nickname || post.username || '匿名用户' }}</span>
            <el-button
              v-if="post.userId && post.userId !== userStore.userId"
              :type="followed ? 'success' : 'primary'"
              :plain="followed"
              size="small"
              :loading="followLoading"
              @click="handleFollow"
            >
              {{ followed ? '已关注' : '关注作者' }}
            </el-button>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
            <span class="post-section" v-if="post.sectionName">{{ post.sectionName }}</span>
            <span class="post-views"> {{ post.viewCount || 0 }} 次浏览</span>
          </div>
        </div>

        <div class="post-content" v-html="post.content"></div>

        <div class="post-actions">
          <el-button
            :type="post.isLiked ? 'primary' : 'default'"
            size="default"
            @click="handleLike"
            :loading="liking"
            :icon="post.isLiked ? StarFilled : Star"
          >
            <span v-if="post.isLiked">已点赞</span>
            <span v-else>点赞</span>
            <span class="like-count">{{ post.likeCount || 0 }}</span>
          </el-button>

          <el-dropdown trigger="click" v-if="canManage">
            <el-button size="default">
              管理 <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleEdit">编辑帖子</el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin || userStore.isTeacher" @click="handleToggleTop">
                  {{ post.isTop ? '取消置顶' : '置顶' }}
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin || userStore.isTeacher" @click="handleToggleEssence">
                  {{ post.isEssence ? '取消加精' : '加精' }}
                </el-dropdown-item>
                <el-dropdown-item divided type="danger" @click="handleDelete">删除帖子</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="comment-section">
        <h3 class="comment-title">评论 ({{ comments.length }})</h3>

        <!-- 发表评论 -->
        <div class="comment-editor">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
            maxlength="2000"
            show-word-limit
          />
          <div class="comment-editor-actions">
            <span v-if="replyTarget" class="reply-hint">
              回复 @{{ replyTarget }} <el-button link type="danger" @click="cancelReply">取消</el-button>
            </span>
            <el-button type="primary" :disabled="!commentContent.trim()" @click="submitComment" :loading="submitting">
              发表评论
            </el-button>
          </div>
        </div>

        <!-- 评论列表 -->
        <div class="comment-list" v-if="comments.length > 0">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-main">
              <div class="comment-user">{{ comment.username || '匿名用户' }}</div>
              <div class="comment-text">{{ comment.content }}</div>
              <div class="comment-footer">
                <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                <el-button link size="small" @click="startReply(comment)">
                  回复
                </el-button>
                <el-button link size="small" @click="likeComment(comment)">
                  {{ comment.likeCount || 0 }} 赞
                </el-button>
              </div>
            </div>

            <!-- 子评论 -->
            <div v-if="comment.children && comment.children.length > 0" class="sub-comments">
              <div v-for="child in comment.children" :key="child.id" class="sub-comment-item">
                <span class="sub-user">{{ child.username || '匿名用户' }}</span>
                <span v-if="child.replyToUsername" class="reply-to"> 回复 @{{ child.replyToUsername }}</span>
                <span class="sub-text">: {{ child.content }}</span>
                <div class="sub-footer">
                  <span class="sub-time">{{ formatTime(child.createdAt) }}</span>
                  <el-button link size="small" @click="startReply(comment)">回复</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else-if="!loading" class="empty-tip">暂无评论，快来抢沙发吧</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getPostDetail, getComments, addComment, toggleLike, deletePost, toggleTop, toggleEssence, toggleFollow } from '@/api/discuss'
import request from '@/utils/request'
import { Star, StarFilled, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const liking = ref(false)
const post = ref<any>(null)
const comments = ref<any[]>([])
const commentContent = ref('')
const replyTarget = ref('')
const replyToUserId = ref<number | undefined>(undefined)
const replyParentId = ref<number | undefined>(undefined)

const followed = ref(false)
const followLoading = ref(false)

// localStorage: 缓存用户点赞的帖子ID
const LIKED_POSTS_KEY = 'zhikao_liked_posts'
function getLikedPostIds(): number[] {
  try {
    const raw = localStorage.getItem(LIKED_POSTS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch { return [] }
}
function setLikedPostIds(ids: number[]) {
  try {
    localStorage.setItem(LIKED_POSTS_KEY, JSON.stringify(ids))
  } catch { /* ignore */ }
}
function isPostLikedInCache(postId: number): boolean {
  return getLikedPostIds().includes(postId)
}

const canManage = computed(() => userStore.isAdmin || userStore.isTeacher || (post.value && post.value.userId === userStore.userId))

function formatTime(time: string) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '--'
}

async function loadPost() {
  loading.value = true
  try {
    const id = Number(route.params.postId)
    const res = await getPostDetail(id)
    post.value = res.data
    // 检查点赞状态：优先从后端返回，否则查 localStorage 缓存，或尝试 get 接口
    if (post.value && post.value.isLiked === undefined) {
      post.value.isLiked = false
      try {
        const likeRes = await request.get(`/discuss/like/check?targetType=1&targetId=${id}`)
        if (likeRes.data?.liked !== undefined) {
          post.value.isLiked = likeRes.data.liked
        }
      } catch {
        post.value.isLiked = isPostLikedInCache(id)
      }
    }
    if (post.value && post.value.likeCount === undefined) post.value.likeCount = 0
    if (post.value && post.value.followed !== undefined) {
      followed.value = !!post.value.followed
    }
    await loadComments()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载帖子失败')
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  try {
    const id = Number(route.params.postId)
    const res = await getComments(id)
    comments.value = res.data || []
  } catch { /* ignore */ }
}

async function handleLike() {
  if (!post.value || liking.value) return
  liking.value = true
  try {
    const res = await toggleLike({ targetType: 1, targetId: post.value.id })
    if (res.data) {
      post.value.isLiked = res.data.liked
      post.value.likeCount = res.data.likeCount
      // 同步到 localStorage
      const likedIds = getLikedPostIds()
      if (post.value.isLiked && !likedIds.includes(post.value.id)) {
        likedIds.push(post.value.id)
      } else if (!post.value.isLiked) {
        const idx = likedIds.indexOf(post.value.id)
        if (idx > -1) likedIds.splice(idx, 1)
      }
      setLikedPostIds(likedIds)
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    liking.value = false
  }
}

async function likeComment(comment: any) {
  try {
    const res = await toggleLike({ targetType: 2, targetId: comment.id })
    if (res.data) {
      comment.likeCount = res.data.likeCount
    }
  } catch { /* ignore */ }
}

function startReply(comment: any) {
  replyTarget.value = comment.username || '匿名用户'
  replyParentId.value = comment.id
  replyToUserId.value = comment.userId
  commentContent.value = ''
}

function cancelReply() {
  replyTarget.value = ''
  replyParentId.value = undefined
  replyToUserId.value = undefined
  commentContent.value = ''
}

async function submitComment() {
  if (!commentContent.value.trim()) return
  submitting.value = true
  try {
    await addComment({
      postId: Number(route.params.postId),
      content: commentContent.value,
      parentId: replyParentId.value,
      replyToUserId: replyToUserId.value
    })
    commentContent.value = ''
    cancelReply()
    ElMessage.success('评论成功')
    await loadComments()
  } catch { /* ignore */ } finally {
    submitting.value = false
  }
}

async function handleEdit() {
  if (post.value) {
    router.push(`/discuss/edit/${post.value.id}`)
  }
}

async function handleToggleTop() {
  if (!post.value) return
  try {
    await toggleTop(post.value.id, !post.value.isTop)
    post.value.isTop = post.value.isTop ? 0 : 1
    ElMessage.success(post.value.isTop ? '已置顶' : '已取消置顶')
  } catch { /* ignore */ }
}

async function handleToggleEssence() {
  if (!post.value) return
  try {
    await toggleEssence(post.value.id, !post.value.isEssence)
    post.value.isEssence = post.value.isEssence ? 0 : 1
    ElMessage.success(post.value.isEssence ? '已加精' : '已取消加精')
  } catch { /* ignore */ }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这个帖子吗？', '确认删除', { type: 'warning' })
    await deletePost(Number(route.params.postId))
    ElMessage.success('删除成功')
    router.push('/discuss')
  } catch { /* cancel */ }
}

async function handleFollow() {
  if (!post.value || !post.value.userId || followLoading.value) return
  followLoading.value = true
  try {
    const res: any = await toggleFollow(post.value.userId)
    if (res.data?.followed !== undefined) {
      followed.value = !!res.data.followed
    } else {
      followed.value = !followed.value
    }
    ElMessage.success(followed.value ? '已关注' : '已取消关注')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}

onMounted(loadPost)
</script>

<style scoped>
.post-detail-page { padding: 24px; max-width: 900px; margin: 0 auto; }
.breadcrumb { margin-bottom: 20px; }
.post-container { background: var(--color-rice-card); border-radius: var(--radius-md); padding: 32px; box-shadow: var(--shadow-md); }
.post-main-card { margin-bottom: 32px; padding-bottom: 24px; border-bottom: 1px solid var(--color-rice-border); }
.post-header { margin-bottom: 20px; }
.post-tags { display: flex; gap: 8px; margin-bottom: 12px; }
.post-title { font-size: 24px; color: var(--color-ink); margin-bottom: 12px; }
.post-author { display: flex; gap: 16px; font-size: 13px; color: var(--color-ink-muted); align-items: center; }
.author-name { font-weight: 600; color: var(--color-ink); }
.post-content { font-size: 15px; line-height: 1.8; color: var(--color-ink); margin-bottom: 24px; word-break: break-word; }
.post-content :deep(img) { max-width: 100%; }
.post-actions { display: flex; gap: 12px; align-items: center; }
.like-count { margin-left: 4px; }

.comment-section { margin-top: 8px; }
.comment-title { font-size: 18px; color: var(--color-ink); margin-bottom: 20px; }
.comment-editor { margin-bottom: 24px; }
.comment-editor-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }
.reply-hint { font-size: 13px; color: var(--color-ink-muted); }

.comment-item { padding: 16px 0; border-bottom: 1px solid #f5f5f5; }
.comment-item:last-child { border-bottom: none; }
.comment-main { }
.comment-user { font-size: 14px; font-weight: 600; color: var(--color-ink); margin-bottom: 6px; }
.comment-text { font-size: 14px; color: var(--color-ink); line-height: 1.6; margin-bottom: 8px; }
.comment-footer { display: flex; gap: 16px; font-size: 12px; color: var(--color-ink-muted); }
.comment-time { color: var(--color-ink-muted); }

.sub-comments { margin-top: 12px; margin-left: 48px; padding: 12px 16px; background: var(--color-rice-card); border-radius: var(--radius-sm); }
.sub-comment-item { padding: 8px 0; border-bottom: 1px solid var(--color-rice-border); font-size: 13px; }
.sub-comment-item:last-child { border-bottom: none; }
.sub-user { color: var(--color-primary); font-weight: 500; }
.reply-to { color: var(--color-ink-muted); }
.sub-text { color: var(--color-ink); }
.sub-footer { display: flex; gap: 12px; font-size: 11px; color: var(--color-ink-muted); margin-top: 4px; }
.sub-time { color: var(--color-ink-muted); }

.empty-tip { text-align: center; color: var(--color-ink-muted); padding: 40px 0; }
</style>
