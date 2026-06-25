<template>
  <div class="exam-grade">
    <el-card>
      <template #header><span>主观题批阅</span></template>
      <el-form inline>
        <el-form-item label="试卷ID">
          <el-input-number v-model="paperId" :min="1" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadPending">加载待批阅</el-button>
        </el-form-item>
      </el-form>
      <div class="tip">加载该试卷下状态为"待阅"的考试记录，逐题复核 AI 预评分并提交。</div>
    </el-card>

    <el-card v-if="pendingList.length" style="margin-top: 16px">
      <el-collapse v-model="activeNames">
        <el-collapse-item
          v-for="item in pendingList"
          :key="item.sessionId"
          :name="item.sessionId"
        >
          <template #title>
            <span class="stu">考生 #{{ item.userId }}</span>
            <span class="total">当前总分 {{ item.totalScore }}</span>
          </template>
          <div v-for="a in item.answers" :key="a.answerId" class="answer-item">
            <div class="q-title">{{ a.title }}</div>
            <div class="ref">参考答案：{{ a.referenceAnswer || '（无）' }}</div>
            <div class="usr">学生作答：{{ a.userAnswer || '（未作答）' }}</div>
            <div class="ai">AI 预评分：{{ a.currentScore }}</div>
            <div class="grade-row">
              <span>改分：</span>
              <el-input-number v-model="scoreMap[a.answerId]" :min="0" :precision="1" :step="0.5" />
              <el-button type="primary" size="small" @click="doGrade(a.answerId)">提交批阅</el-button>
              <el-tag v-if="a.graded" type="success" size="small" style="margin-left: 8px">已批阅</el-tag>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
    <el-empty v-else-if="loaded" description="无待批阅的主观题" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingGrade, gradeAnswer } from '@/api/exam'

const paperId = ref(1)
const pendingList = ref<any[]>([])
const activeNames = ref<number[]>([])
const loaded = ref(false)
const scoreMap = reactive<Record<number, number>>({})

async function loadPending() {
  if (!paperId.value) {
    ElMessage.warning('请输入试卷ID')
    return
  }
  const res: any = await getPendingGrade(paperId.value)
  pendingList.value = res.data || []
  activeNames.value = pendingList.value.map((i: any) => i.sessionId)
  pendingList.value.forEach((item: any) => {
    item.answers.forEach((a: any) => {
      scoreMap[a.answerId] = a.currentScore ?? 0
    })
  })
  loaded.value = true
}

async function doGrade(answerId: number) {
  await gradeAnswer({ answerId, score: scoreMap[answerId] })
  ElMessage.success('批阅成功')
  // 局部刷新
  await loadPending()
}
</script>

<style scoped>
.tip { color: #909399; font-size: 13px; margin-top: 8px }
.stu { font-weight: 600; margin-right: 16px }
.total { color: #e6a23c }
.answer-item { padding: 12px 0; border-bottom: 1px dashed #eee }
.q-title { font-weight: 600; margin-bottom: 6px }
.ref { color: #67c23a; font-size: 13px; margin-bottom: 4px }
.usr { color: #303133; margin-bottom: 4px }
.ai { color: #e6a23c; font-size: 13px; margin-bottom: 6px }
.grade-row { display: flex; align-items: center; gap: 8px; margin-top: 4px }
</style>
