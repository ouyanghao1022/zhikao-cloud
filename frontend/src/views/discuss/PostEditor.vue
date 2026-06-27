<template>
  <div class="post-editor-page">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/discuss' }">讨论区</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/discuss/posts' }">帖子列表</el-breadcrumb-item>
      <el-breadcrumb-item>{{ isEdit ? '编辑帖子' : '发布新帖' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="editor-card">
      <h2 class="editor-title">{{ isEdit ? '编辑帖子' : '发布新帖' }}</h2>

      <el-form :model="form" label-position="top" :rules="rules" ref="formRef">
        <el-form-item label="选择版块" prop="sectionId" v-if="!isEdit">
          <el-select v-model="form.sectionId" placeholder="请选择版块" style="width: 100%">
            <el-option
              v-for="s in sections"
              :key="s.id"
              :label="s.sectionName"
              :value="s.id"
            >
              <span>{{ getSectionIcon(s.sectionType) }} {{ s.sectionName }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入帖子标题（1-100字）" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="标签" prop="tags">
          <el-input v-model="form.tags" placeholder="输入标签，用逗号分隔" maxlength="50" />
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="12"
            placeholder="请输入帖子内容..."
            maxlength="20000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">
            {{ isEdit ? '保存修改' : '发布帖子' }}
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createPost, updatePost, getPostDetail, getSections } from '@/api/discuss'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => route.path.includes('/edit/'))
const formRef = ref()
const submitting = ref(false)
const sections = ref<any[]>([])

const form = reactive({
  sectionId: undefined as number | undefined,
  title: '',
  tags: '',
  content: ''
})

const sectionIcons: Record<number, string> = {
  1: '论', 2: '文', 3: '问', 4: '活', 5: '告'
}

function getSectionIcon(type: number) {
  return sectionIcons[type] || '帖'
}

const rules = {
  sectionId: [{ required: true, message: '请选择版块', trigger: 'change' }],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 1, max: 100, message: '标题长度为1-100字', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

async function loadSections() {
  try {
    const res = await getSections()
    sections.value = res.data || []
  } catch { /* ignore */ }
}

async function loadPost() {
  if (!isEdit.value) return
  try {
    const id = Number(route.params.postId)
    const res = await getPostDetail(id)
    const post = res.data
    if (post) {
      form.title = post.title
      form.content = post.content
      form.tags = post.tags || ''
    }
  } catch {
    ElMessage.error('加载帖子失败')
    router.back()
  }
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updatePost(Number(route.params.postId), {
        title: form.title,
        content: form.content,
        tags: form.tags || undefined
      })
      ElMessage.success('修改成功')
      router.push(`/discuss/post/${route.params.postId}`)
    } else {
      const res = await createPost({
        sectionId: form.sectionId!,
        title: form.title,
        content: form.content,
        tags: form.tags || undefined
      })
      ElMessage.success('发布成功')
      router.push(`/discuss/post/${res.data?.id}`)
    }
  } catch { /* error handled by interceptor */ } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadSections()
  loadPost()
})
</script>

<style scoped>
.post-editor-page { padding: 24px; max-width: 800px; margin: 0 auto; }
.breadcrumb { margin-bottom: 20px; }
.editor-card { background: var(--color-rice-card); border-radius: var(--radius-md); padding: 32px; box-shadow: var(--shadow-md); }
.editor-title { font-size: 22px; color: var(--color-ink); margin-bottom: 28px; }
</style>
