<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>考试管理</h2>
      <div style="display:flex;gap:8px">
        <el-button @click="downloadExamTemplate"><el-icon><Download /></el-icon> 模板</el-button>
        <el-button type="success" @click="showImport=true"><el-icon><Upload /></el-icon> 导入试卷</el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> 创建考试
        </el-button>
      </div>
    </div>

    <!-- 试卷列表 -->
    <el-card shadow="never">
      <div class="filter-bar">
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="loadData" style="width:120px">
          <el-option label="全部" :value="undefined" />
          <el-option label="已发布" :value="1" />
          <el-option label="已结束" :value="2" />
          <el-option label="草稿" :value="0" />
        </el-select>
        <el-button @click="loadData">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe style="width:100%">
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="title" label="考试名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="组卷模式" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.paperType===2?'warning':''" size="small">{{ row.paperType===2?'随机组卷':'固定组卷' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="70" align="center" />
        <el-table-column prop="duration" label="时长(min)" width="85" align="center" />
        <el-table-column prop="passScore" label="合格分" width="70" align="center" />
        <el-table-column label="状态" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':row.status===2?'warning':'info'" size="small">
              {{ ['草稿','已发布','已结束'][row.status||0] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">查看</el-button>
            <el-button v-if="row.status!==2" type="warning" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status===0" type="success" link size="small" @click="publishExam(row)">发布</el-button>
            <el-button type="danger" link size="small" @click="deleteExam(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total>0" v-model:current-page="current" v-model:page-size="size" :total="total"
        layout="total, prev, pager, next" @current-change="loadData" style="margin-top:16px;justify-content:flex-end" />
    </el-card>

    <!-- ========== 试卷详情弹窗 ========== -->
    <el-dialog v-model="detailVisible" :title="'试卷详情 - ' + (detailExam?.title||'')" width="750px">
      <div v-if="detailExam" v-loading="detailLoading">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="总分">{{ detailExam.totalScore }} 分</el-descriptions-item>
          <el-descriptions-item label="时长">{{ detailExam.duration }} 分钟</el-descriptions-item>
          <el-descriptions-item label="合格分">{{ detailExam.passScore || '--' }} 分</el-descriptions-item>
          <el-descriptions-item label="组卷模式">
            <el-tag :type="detailExam.paperType===2?'warning':''" size="small">{{ detailExam.paperType===2?'随机组卷':'固定组卷' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailExam.status===1?'success':detailExam.status===2?'warning':'info'" size="small">
              {{ ['草稿','已发布','已结束'][detailExam.status||0] }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="参加人数">{{ detailExam.enrolledCount||0 }} 人</el-descriptions-item>
          <el-descriptions-item label="创建人" :span="3">{{ detailExam.creatorName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="出题来源" :span="3">{{ detailTeacherNames.length ? detailTeacherNames.join('、') : '—' }}</el-descriptions-item>
          <el-descriptions-item label="题型分布" :span="3">{{ questionTypeSummary }}</el-descriptions-item>
          <el-descriptions-item label="说明" :span="3">{{ detailExam.description || '无' }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin:16px 0 10px">题目列表 ({{ detailQuestions.length }} 题)</h4>
        <el-table :data="detailQuestions" size="small" max-height="360">
          <el-table-column label="序号" type="index" width="55" />
          <el-table-column label="题型" width="75">
            <template #default="{row}"><el-tag size="small">{{ typeLabel(row.questionType||row.type) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="title" label="题目内容" show-overflow-tooltip />
          <el-table-column label="分值" width="60" align="center">
            <template #default="{row}">{{ row.score||0 }}分</template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button v-if="detailExam?.paperType===2 && detailExam?.status===0" type="warning" :loading="reassembling" @click="reassemblePaper">重新抽题</el-button>
        <el-button @click="detailVisible=false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ========== 编辑考试弹窗 ========== -->
    <el-dialog v-model="showEdit" title="编辑考试" width="600px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="90px" label-position="right">
        <el-form-item label="考试名称" required>
          <el-input v-model="editForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="时长(min)">
              <el-input-number v-model="editForm.duration" :min="1" :max="300" :disabled="editHasSessions" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总分">
              <el-input-number v-model="editForm.totalScore" disabled style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合格分">
              <el-input-number v-model="editForm.passScore" :min="0" :max="999" :disabled="editHasSessions" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" required>
              <el-date-picker v-model="editForm.startTime" type="datetime" placeholder="选择开始时间"
                value-format="YYYY-MM-DD HH:mm:ss" :disabled="editHasSessions" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" required>
              <el-date-picker v-model="editForm.endTime" type="datetime" placeholder="选择结束时间"
                value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="切屏次数">
          <el-input-number v-model="editForm.maxScreenSwitch" :min="0" :max="10" :disabled="editHasSessions" style="width:120px" />
          <span v-if="editHasSessions" style="font-size:12px;color:#e6a23c;margin-left:8px">已有作答记录，大部分字段不可修改</span>
        </el-form-item>
        <el-form-item label="考试说明">
          <el-input v-model="editForm.description" type="textarea" :rows="2" placeholder="考试说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit=false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- ========== 创建考试弹窗（两步） ========== -->
    <el-dialog v-model="showCreate" title="创建考试" width="860px" :close-on-click-modal="false">
      <el-steps :active="createStep" align-center simple style="margin-bottom:24px">
        <el-step v-for="(s,i) in createSteps" :key="i" :title="s" />
      </el-steps>

      <!-- 步骤0：选择出题老师（仅管理员） -->
      <div v-show="userStore.isAdmin && createStep===0">
        <el-form label-width="100px">
          <el-form-item label="出题老师" required>
            <el-select v-model="selectedTeacherIds" multiple filterable placeholder="选择一位或多位老师（决定题库范围）" style="width:100%" @change="onTeacherChange">
              <el-option v-for="t in teacherOptions" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
            <div style="font-size:12px;color:#909399;margin-top:6px">仅所选老师名下的题目对本次组卷可见</div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤1：基本信息 -->
      <div v-show="createStep===stepBasic">
        <el-form :model="createForm" label-width="90px" label-position="right">
          <el-form-item label="考试名称" required>
            <el-input v-model="createForm.title" placeholder="请输入考试名称" maxlength="100" show-word-limit />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="时长(min)">
                <el-input-number v-model="createForm.duration" :min="1" :max="300" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="总分">
                <el-input-number v-model="createForm.totalScore" :min="1" :max="999" style="width:100%" @change="onTotalScoreChange" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="合格分">
                <el-input-number v-model="createForm.passScore" :min="0" :max="999" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="组卷模式" required>
            <el-radio-group v-model="createForm.mode">
              <el-radio-button :value="1">固定组卷（手动选题）</el-radio-button>
              <el-radio-button :value="2">随机组卷（自动抽题）</el-radio-button>
            </el-radio-group>
            <div style="font-size:12px;color:#909399;margin-top:6px">
              {{ createForm.mode===1?'手动从题库中选择题目组成试卷':'按题型/数量/难度自动从题库随机抽取' }}
            </div>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="开始时间" required>
                <el-date-picker v-model="createForm.startTime" type="datetime" placeholder="必填" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束时间" required>
                <el-date-picker v-model="createForm.endTime" type="datetime" placeholder="必填" value-format="YYYY-MM-DD HH:mm:ss" :default-time="defaultEndTime" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="考试说明">
            <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="考试说明（可选）" />
          </el-form-item>
          <el-form-item label="允许切屏">
            <el-input-number v-model="createForm.maxScreenSwitch" :min="0" :max="10" style="width:120px" />
            <span style="font-size:12px;color:#909399;margin-left:8px">超过次数自动交卷，0 为不允许切屏</span>
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤2A：固定组卷 - 选择题目 -->
      <div v-show="createStep===stepPick && createForm.mode===1">
        <el-row :gutter="16">
          <!-- 左侧：题库 -->
          <el-col :span="14">
            <div class="panel-title">题库</div>
            <div class="filter-bar" style="margin-bottom:12px">
              <el-select v-model="qCategoryFilter" placeholder="分类" clearable size="small" style="width:110px" @change="searchQuestions">
                <el-option label="默认题库" :value="1" />
                <el-option label="历年真题" :value="2" />
              </el-select>
              <el-select v-model="qTypeFilter" placeholder="题型" clearable size="small" style="width:100px" @change="searchQuestions">
                <el-option label="单选" :value="1" /><el-option label="多选" :value="2" />
                <el-option label="判断" :value="5" /><el-option label="填空" :value="3" />
                <el-option label="简答" :value="4" />
              </el-select>
              <el-select v-model="qDiffFilter" placeholder="难度" clearable size="small" style="width:90px" @change="searchQuestions">
                <el-option label="简单" :value="1" /><el-option label="中等" :value="2" /><el-option label="困难" :value="3" />
              </el-select>
              <el-input v-model="qKeyword" placeholder="搜索..." clearable size="small" style="width:140px" @keyup.enter="searchQuestions" />
              <el-button size="small" @click="searchQuestions">搜索</el-button>
            </div>
            <el-table ref="qTableRef" :data="questionBank" v-loading="qLoading" size="small" max-height="380"
              @selection-change="onBankSelectionChange" row-key="id">
              <el-table-column type="selection" width="40" :selectable="(row: any) => !selectedMap.has(row.id)" />
              <el-table-column label="题型" width="55">
                <template #default="{row}"><span style="font-size:12px">{{ typeShort(row.questionType) }}</span></template>
              </el-table-column>
              <el-table-column prop="title" label="题目" show-overflow-tooltip />
              <el-table-column prop="score" label="分" width="45" align="center">
                <template #default="{row}">{{ row.score||0 }}</template>
              </el-table-column>
            </el-table>
            <el-pagination v-if="qTotal>10" v-model:current-page="qPage" :page-size="10" :total="qTotal"
              layout="prev, pager, next" @current-change="searchQuestions" size="small" style="margin-top:8px;justify-content:center" />
          </el-col>
          <!-- 右侧：已选题目 -->
          <el-col :span="10">
            <div class="panel-title">已选题目 ({{ selectedQuestions.length }})</div>
            <div v-if="selectedQuestions.length===0" class="empty-hint">点击左侧题目加入试卷</div>
            <el-scrollbar max-height="420px">
              <div v-for="(q,i) in selectedQuestions" :key="q.id" class="selected-item">
                <div class="sel-header">
                  <el-tag size="small">{{ typeShort(q.questionType) }}</el-tag>
                  <span class="sel-title">{{ q.title }}</span>
                  <el-button type="danger" link size="small" @click="removeQuestion(i)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
                <div class="sel-score">
                  <span style="font-size:12px;color:#909399">分值：</span>
                  <el-input-number v-model="selectedScores[i]" :min="0" :max="100" size="small" style="width:90px" />
                </div>
              </div>
            </el-scrollbar>
          </el-col>
        </el-row>
      </div>

      <!-- 步骤2B：随机组卷 - 抽题配置 -->
      <div v-show="createStep===stepPick && createForm.mode===2">
        <el-form label-width="100px">
          <el-form-item label="题型与数量">
            <div v-for="t in randomTypes" :key="t.value" class="random-type-row">
              <el-checkbox v-model="t.enabled" style="width:72px">{{ t.label }}</el-checkbox>
              <span style="margin:0 4px;color:#909399">数量</span>
              <el-input-number v-model="t.count" :min="0" :max="50" size="small" :disabled="!t.enabled" style="width:80px" />
              <span style="margin:0 4px;color:#909399">难度</span>
              <el-select v-model="t.difficulty" size="small" :disabled="!t.enabled" style="width:90px">
                <el-option label="不限" :value="0" /><el-option label="简单" :value="1" />
                <el-option label="中等" :value="2" /><el-option label="困难" :value="3" />
              </el-select>
            </div>
            <div style="font-size:12px;color:#909399;margin-top:4px">
              预计总题数：{{ randomTypes.filter(t=>t.enabled).reduce((s,t)=>s+t.count,0) }} 题
            </div>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleCancelCreate">取消</el-button>
          <el-button v-if="createStep>0" @click="handlePrevStep">上一步</el-button>
          <el-button v-if="createStep<maxStep" type="primary" @click="goNextStep">下一步</el-button>
          <el-button v-if="createStep===maxStep" type="primary" :loading="creating" @click="handleCreate">创建考试</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 发布考试-选择班级 -->
    <el-dialog v-model="publishVisible" title="发布考试 - 选择班级" width="500px">
      <p style="margin-bottom:16px">将 <strong>{{ publishTarget?.title }}</strong> 发布到以下班级：</p>
      <!-- 管理员：先选教师 -->
      <el-form-item v-if="userStore.isAdmin" label="选择教师" style="margin-bottom:12px">
        <el-select v-model="publishTeacherId" placeholder="选择教师" clearable @change="onPublishTeacherChange" style="width:100%">
          <el-option v-for="t in teacherList" :key="t.id" :label="t.name + ' (' + t.classCount + '个班级)'" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-checkbox-group v-model="publishClassIds" style="display:flex;flex-direction:column;gap:6px">
        <el-checkbox v-for="c in publishClasses" :key="c.id" :value="c.id" :label="c.className || c.name">
          {{ c.className || c.name }}
          <span style="color:#909399;font-size:12px">（{{ c.studentCount || 0 }}人）</span>
        </el-checkbox>
      </el-checkbox-group>
      <div v-if="publishClasses.length===0" style="text-align:center;padding:24px;color:#909399">
        {{ userStore.isAdmin ? '请先选择教师' : '您还没有创建班级' }}
      </div>
      <template #footer>
        <el-button @click="publishVisible=false">取消</el-button>
        <el-button type="primary" :disabled="publishClassIds.length===0" :loading="publishing" @click="doPublish">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- ========== 导入试卷弹窗 ========== -->
    <el-dialog v-model="showImport" title="导入试卷" width="540px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px">
        <div style="line-height:1.6">
          请先下载模板按格式填写。Excel 包含两个 Sheet：<br>
          <b>①试卷信息</b>：试卷名称*、考试时长、总分、及格分、开始时间*、结束时间*、组卷模式、考试说明<br>
          <b>②题目列表</b>：题型、难度、题目内容*、选项A-D、正确答案*、解析、分值*
        </div>
      </el-alert>
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="(f:any)=>{importFile=f.raw}"
        :on-remove="()=>{importFile=null}"
      >
        <el-icon :size="40"><UploadFilled /></el-icon>
        <div style="margin-top:8px">拖拽或点击上传 Excel 文件</div>
      </el-upload>
      <div v-if="importResult" style="margin-top:12px">
        <el-alert :type="importResult.errors?.length?'warning':'success'" :closable="false">
          <p>试卷「{{ importResult.paperTitle }}」导入完成：共 {{ importResult.total }} 题，成功 {{ importResult.success }} 题{{ importResult.skip>0?`，跳过 ${importResult.skip} 题` : '' }}，总分 {{ importResult.actualTotalScore }} 分</p>
          <ul v-if="importResult.errors?.length" style="margin:4px 0 0;font-size:12px;max-height:80px;overflow-y:auto">
            <li v-for="(e,i) in importResult.errors" :key="i">{{ e }}</li>
          </ul>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="showImport=false;importResult=null">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="handleImportExam">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getExamList, getExamDetail } from '@/api/exam'
import { getQuestionList } from '@/api/question'
import { getMyClasses } from '@/api/class'
import request from '@/utils/request'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Download, Upload, UploadFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const statusFilter = ref<number|undefined>(undefined)

// 详情
const detailVisible = ref(false)
const detailExam = ref<any>(null)
const detailQuestions = ref<any[]>([])
const detailLoading = ref(false)
const detailTeacherNames = ref<string[]>([])
const reassembling = ref(false)

// 创建考试
const showCreate = ref(false)
const creating = ref(false)
const createStep = ref(0)
const createForm = reactive({
  title: '', duration: 60, totalScore: 100, passScore: 60, description: '', mode: 1, maxScreenSwitch: 3,
  startTime: null as string | null, endTime: null as string | null,
})
// 结束时间默认 23:59:59 —— Element Plus 的 default-time 必须是 Date 对象，
// 传 { hours, minutes, seconds } 普通对象会导致 dayjs(obj) 返回 Invalid，日历渲染全 NaN
const defaultEndTime = new Date(2000, 0, 1, 23, 59, 59)

// 题库选择（固定组卷）
const questionBank = ref<any[]>([])
const qLoading = ref(false)
const qTotal = ref(0)
const qPage = ref(1)
const qTypeFilter = ref<number|undefined>(undefined)
const qDiffFilter = ref<number|undefined>(undefined)
const qCategoryFilter = ref<number|undefined>(undefined)
const qKeyword = ref('')
const selectedQuestions = ref<any[]>([])
const selectedScores = ref<number[]>([])
const selectedMap = ref(new Map<number,boolean>()) // 已选题目ID映射

// 随机组卷配置
const randomTypes = reactive([
  { value:1, label:'单选', enabled:true, count:10, difficulty:0 },
  { value:2, label:'多选', enabled:true, count:5, difficulty:0 },
  { value:5, label:'判断', enabled:true, count:10, difficulty:0 },
  { value:3, label:'填空', enabled:false, count:0, difficulty:0 },
  { value:4, label:'简答', enabled:false, count:0, difficulty:0 },
])

const typeMap: Record<number,string> = {1:'单选题',2:'多选题',3:'填空题',4:'简答题',5:'判断题'}
const typeShortMap: Record<number,string> = {1:'单选',2:'多选',3:'填空',4:'简答',5:'判断'}
function typeLabel(t:number){return typeMap[t]||'未知'}
function typeShort(t:number){return typeShortMap[t]||'其他'}

const questionTypeSummary = computed(() => {
  if (!detailQuestions.value.length) return '--'
  const m: Record<number,number> = {}
  detailQuestions.value.forEach((q:any) => {
    const t = q.questionType || q.type || 1
    m[t] = (m[t]||0) + 1
  })
  return Object.entries(m).map(([k,v]) => typeLabel(Number(k)) + '×' + v).join(' / ')
})

async function loadData() {
  loading.value = true
  try {
    const res = await getExamList({ current: current.value, size: size.value, status: statusFilter.value })
    if (res.data) { list.value = res.data.records||[]; total.value = res.data.total||0 }
  } finally { loading.value = false }
}

async function viewDetail(row: any) {
  detailExam.value = row
  detailQuestions.value = []
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getExamDetail(row.id)
    if (res.data) {
      const d = res.data
      detailExam.value = d.paper || d
      detailQuestions.value = d.questions || d.questionList || []
      detailTeacherNames.value = d.teacherNames || []
    }
  } finally { detailLoading.value = false }
}

async function reassemblePaper() {
  if (!detailExam.value) return
  reassembling.value = true
  try {
    const res = await request.post(`/exam/${detailExam.value.id}/reassemble`)
    if (res.code === 200) {
      ElMessage.success(`重新抽题完成：${res.data?.questionCount} 题，总分 ${res.data?.totalScore}`)
      await viewDetail(detailExam.value)
      loadData()
    } else {
      ElMessage.error(res.message || '重新抽题失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '重新抽题失败')
  } finally { reassembling.value = false }
}

// ============ 编辑考试 ============
const showEdit = ref(false)
const editSaving = ref(false)
const editHasSessions = ref(false)
const editForm = reactive({
  id: 0, title: '', duration: 60, totalScore: 100, passScore: 60,
  startTime: '', endTime: '', maxScreenSwitch: 3, description: ''
})

function openEdit(row: any) {
  editForm.id = row.id
  editForm.title = row.title || ''
  editForm.duration = row.duration || 60
  editForm.totalScore = row.totalScore ?? 100
  editForm.passScore = row.passScore ?? 60
  editForm.startTime = row.startTime ? dayjs(row.startTime).format('YYYY-MM-DD HH:mm:ss') : ''
  editForm.endTime = row.endTime ? dayjs(row.endTime).format('YYYY-MM-DD HH:mm:ss') : ''
  editForm.maxScreenSwitch = row.maxScreenSwitch ?? 3
  editForm.description = row.description || ''
  editHasSessions.value = (row.enrolledCount || 0) > 0
  showEdit.value = true
}

async function saveEdit() {
  if (!editForm.title.trim()) { ElMessage.warning('请输入考试名称'); return }
  if (!editForm.startTime || !editForm.endTime) { ElMessage.warning('请填写开始时间和结束时间'); return }
  editSaving.value = true
  try {
    const payload: any = {
      title: editForm.title,
      description: editForm.description || null,
      duration: editForm.duration,
      passScore: editForm.passScore,
      startTime: editForm.startTime,
      endTime: editForm.endTime,
      maxScreenSwitch: editForm.maxScreenSwitch,
    }
    const res = await request.put(`/exam/${editForm.id}`, payload)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      showEdit.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally { editSaving.value = false }
}

const userStore = useUserStore()

// 出题老师（管理员创建考试时框定题库范围）
const selectedTeacherIds = ref<number[]>([])
const teacherOptions = ref<any[]>([])
const stepBasic = computed(() => userStore.isAdmin ? 1 : 0)
const stepPick = computed(() => userStore.isAdmin ? 2 : 1)
const maxStep = computed(() => userStore.isAdmin ? 2 : 1)
const createSteps = computed(() => userStore.isAdmin ? ['选择老师','基本信息','选题/配置'] : ['基本信息','选题/配置'])

// 发布班级选择
const publishVisible = ref(false)
const publishTarget = ref<any>(null)
const publishing = ref(false)
const publishClassIds = ref<number[]>([])
const publishClasses = ref<any[]>([])
const publishTeacherId = ref<number|undefined>(undefined)
const teacherList = ref<any[]>([])

async function publishExam(row: any) {
  publishTarget.value = row
  publishClassIds.value = []
  publishTeacherId.value = undefined
  publishVisible.value = true

  if (userStore.isAdmin) {
    // 管理员：加载教师列表
    try {
      const res = await request.get('/class/teachers')
      teacherList.value = res.data || []
    } catch { teacherList.value = [] }
    publishClasses.value = []
  } else {
    // 教师：加载自己的班级
    await loadMyClasses()
  }
}

async function onPublishTeacherChange(teacherId: number | undefined) {
  publishClassIds.value = []
  if (!teacherId) { publishClasses.value = []; return }
  try {
    const res = await request.get('/class/list', { params: { current:1, size:100, teacherId } })
    publishClasses.value = res.data?.records || []
  } catch { publishClasses.value = [] }
}

async function loadMyClasses() {
  try {
    const res = await getMyClasses()
    publishClasses.value = res.data || []
  } catch { publishClasses.value = [] }
}

async function doPublish() {
  if (!publishTarget.value || publishClassIds.value.length === 0) return
  publishing.value = true
  try {
    const res = await request.put(`/exam/publish/${publishTarget.value.id}`, {
      classIds: publishClassIds.value
    })
    if (res.code === 200) {
      ElMessage.success('发布成功')
      publishVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch {
    ElMessage.error('发布失败')
  } finally {
    publishing.value = false
  }
}

// 旧版 publishExam 已替换为上面的实现

async function deleteExam(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」？`,'确认删除',{type:'warning'})
    await request.delete(`/exam/${row.id}`)
    ElMessage.success('已删除')
    loadData()
  } catch {}
}

// ============ 创建考试流程 ============

async function openCreate() {
  resetCreate()
  showCreate.value = true
  if (userStore.isAdmin) {
    try {
      const res = await request.get('/class/teachers')
      teacherOptions.value = res.data || []
    } catch { teacherOptions.value = [] }
  }
}

function resetCreate() {
  createStep.value = 0
  selectedTeacherIds.value = []
  Object.assign(createForm, { title:'', duration:60, totalScore:100, passScore:60, description:'', mode:1, maxScreenSwitch:3, startTime:null, endTime:null })
  selectedQuestions.value = []
  selectedScores.value = []
  selectedMap.value = new Map()
  questionBank.value = []
  qTotal.value = 0
  qPage.value = 1
  qKeyword.value = ''
  qTypeFilter.value = undefined
  qDiffFilter.value = undefined
  qCategoryFilter.value = undefined
  randomTypes.forEach(t => { t.enabled = [1,2,5].includes(t.value); t.count = t.value===4||t.value===3?0:10 })
}

function handleCancelCreate() {
  showCreate.value = false
  setTimeout(() => resetCreate(), 200)
}

// 总分变化时自动调整及格分（60%）
function onTotalScoreChange(val: number | undefined) {
  if (val) createForm.passScore = Math.round(val * 0.6)
}

function handlePrevStep() {
  if (createStep.value > 0) createStep.value--
}

async function goNextStep() {
  // 步骤0（管理员选老师）→ 步骤1
  if (userStore.isAdmin && createStep.value === 0) {
    if (selectedTeacherIds.value.length === 0) { ElMessage.warning('请至少选择一位出题老师'); return }
    for (const tid of selectedTeacherIds.value) {
      try {
        const res = await getQuestionList({ current:1, size:1, creatorIds: String(tid) })
        if (!res.data?.total) {
          const t = teacherOptions.value.find(o => o.id === tid)
          ElMessage.warning(`老师${t?.name || tid}名下暂无可用试题`)
          return
        }
      } catch { /* ignore */ }
    }
    createStep.value = stepBasic.value
    return
  }
  // 步骤1（基本信息）→ 步骤2（选题/配置）
  if (createStep.value === stepBasic.value) {
    if (!createForm.title.trim()) { ElMessage.warning('请输入考试名称'); return }
    if (!createForm.startTime) { ElMessage.warning('请选择考试开始时间'); return }
    if (!createForm.endTime) { ElMessage.warning('请选择考试结束时间'); return }
    if (createForm.mode === 1) {
      searchQuestions()
    }
    createStep.value = stepPick.value
  }
}

async function onTeacherChange() {
  // 已进入选题步骤时切换老师 → 提示重新选题
  if (createStep.value === stepPick.value && (selectedQuestions.value.length || randomTypes.some(t => t.count > 0))) {
    try {
      await ElMessageBox.confirm('题库范围已变更，是否清空已选题目并重新选题？', '范围变更', { type: 'warning' })
      selectedQuestions.value = []
      selectedScores.value = []
      selectedMap.value = new Map()
      randomTypes.forEach(t => { t.count = [1,2,5].includes(t.value) ? 10 : 0 })
      if (createForm.mode === 1) searchQuestions()
    } catch { /* 取消 */ }
  }
}

// 搜索题库
async function searchQuestions() {
  qLoading.value = true
  try {
    const res = await getQuestionList({
      current: qPage.value, size: 10,
      questionType: qTypeFilter.value,
      difficulty: qDiffFilter.value,
      categoryId: qCategoryFilter.value,
      keyword: qKeyword.value || undefined,
      creatorIds: selectedTeacherIds.value.length ? selectedTeacherIds.value.join(',') : undefined
    })
    if (res.data) { questionBank.value = res.data.records||[]; qTotal.value = res.data.total||0 }
  } finally { qLoading.value = false }
}

function onBankSelectionChange(rows: any[]) {
  // 只处理新增的（不在已选中的里面）
  const newOnes = rows.filter(r => !selectedMap.value.has(r.id))
  newOnes.forEach(r => {
    selectedMap.value.set(r.id, true)
    selectedQuestions.value.push(r)
    selectedScores.value.push(r.score||1)
  })
}

function removeQuestion(index: number) {
  const q = selectedQuestions.value[index]
  selectedMap.value.delete(q.id)
  selectedQuestions.value.splice(index, 1)
  selectedScores.value.splice(index, 1)
}

async function handleCreate() {
  // 校验
  if (createForm.mode === 1) {
    if (selectedQuestions.value.length === 0) { ElMessage.warning('请选择至少一道题目'); return }
  } else {
    const total = randomTypes.filter(t=>t.enabled).reduce((s,t)=>s+t.count,0)
    if (total === 0) { ElMessage.warning('请配置至少一种题型'); return }
  }

  creating.value = true
  try {
    const payload: any = {
      title: createForm.title,
      duration: createForm.duration,
      totalScore: createForm.totalScore,
      passScore: createForm.passScore,
      description: createForm.description,
      paperType: createForm.mode,
      maxScreenSwitch: createForm.maxScreenSwitch,
      teacherIds: selectedTeacherIds.value,
    }
    if (createForm.startTime) payload.startTime = createForm.startTime
    if (createForm.endTime) payload.endTime = createForm.endTime

    if (createForm.mode === 1) {
      // 固定组卷
      payload.questionIds = selectedQuestions.value.map(q => q.id)
      payload.questionScores = selectedScores.value.map(String)
    } else {
      // 随机组卷
      const enabled = randomTypes.filter(t => t.enabled && t.count > 0)
      const typeCountMap: Record<number,number> = {}
      enabled.forEach(t => { typeCountMap[t.value] = t.count })
      payload.randomConfig = {
        totalCount: enabled.reduce((s,t) => s + t.count, 0),
        typeCountMap,
      }
    }

    await request.post('/exam/create', payload)
    ElMessage.success('创建成功')
    showCreate.value = false
    loadData()
  } catch {} finally { creating.value = false }
}

// ============ 导入试卷 ============
const showImport = ref(false)
const importing = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<any>(null)

async function downloadExamTemplate() {
  try {
    const token = userStore.accessToken
    const res = await axios.get('/api/v1/admin/import/template/exam', {
      responseType: 'blob',
      headers: { Authorization: `Bearer ${token}` }
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a'); a.href = url; a.download = '试卷导入模板.xlsx'; a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('下载失败') }
}

async function handleImportExam() {
  if (!importFile.value) return
  importing.value = true
  try {
    const fd = new FormData(); fd.append('file', importFile.value)
    const r = await request.post('/admin/import/exam', fd, { headers: { 'Content-Type': undefined } })
    importResult.value = r.data
    if (r.data.success > 0) { ElMessage.success(`成功导入试卷「${r.data.paperTitle}」`); loadData() }
  } catch(e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '导入失败')
  }
  finally { importing.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.panel-title { font-size: 14px; font-weight: 600; color: var(--color-ink); margin-bottom: 12px; padding-bottom: 8px; border-bottom: 2px solid var(--color-primary); }
.empty-hint { color: var(--color-ink-muted); text-align: center; padding: 32px 0; font-size: 13px; }

.selected-item { border: 1px solid #e8e8e8; border-radius: 6px; padding: 10px 12px; margin-bottom: 8px; background: #fafafa; }
.selected-item:hover { border-color: var(--color-primary); }
.sel-header { display: flex; align-items: center; gap: 8px; }
.sel-title { flex: 1; font-size: 13px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sel-score { margin-top: 6px; display: flex; align-items: center; gap: 4px; }

.random-type-row { display: flex; align-items: center; gap: 6px; padding: 6px 0; }
.random-type-row + .random-type-row { border-top: 1px dashed #eee; }

</style>
