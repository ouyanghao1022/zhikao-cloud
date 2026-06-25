<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>📖 题库管理</h2>
      <div style="display:flex;gap:8px">
        <el-button @click="categoryDialogVisible=true">
          <el-icon><FolderAdd /></el-icon> 管理目录
        </el-button>
        <el-button @click="downloadQuestionTemplate"><el-icon><Download /></el-icon> 模板</el-button>
        <el-button type="success" @click="showImport=true"><el-icon><Upload /></el-icon> 导入题库</el-button>
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon> 添加题目
        </el-button>
      </div>
    </div>

    <el-card>
      <div class="filter-bar">
        <el-tree-select
          v-model="categoryFilter"
          :data="categoryTree"
          :props="{ label: 'categoryName', value: 'id', children: 'children' }"
          placeholder="题库目录"
          clearable
          check-strictly
          filterable
          style="width:180px"
          @change="loadData"
        />
        <el-select v-model="typeFilter" placeholder="题型" clearable style="width:120px" @change="loadData">
          <el-option label="单选题" :value="1" />
          <el-option label="多选题" :value="2" />
          <el-option label="判断题" :value="5" />
          <el-option label="填空题" :value="3" />
          <el-option label="简答题" :value="4" />
        </el-select>
        <el-select v-model="difficultyFilter" placeholder="难度" clearable style="width:120px" @change="loadData">
          <el-option label="简单" :value="1" />
          <el-option label="中等" :value="2" />
          <el-option label="困难" :value="3" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索题目..." clearable style="width:200px" @keyup.enter="loadData" @clear="loadData" />
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>

      <el-table :data="list" v-loading="loading" style="width:100%">
        <el-table-column type="index" label="序号" width="55" />
        <el-table-column prop="title" label="题目内容" show-overflow-tooltip min-width="280" />
        <el-table-column label="所属目录" width="140">
          <template #default="{ row }">
            <el-tag type="warning" size="small" v-if="row.categoryName">{{ row.categoryName }}</el-tag>
            <el-tag type="info" size="small" v-else>未分类</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="题型" width="70">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.questionType)" size="small">{{ typeLabel(row.questionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="70">
          <template #default="{ row }">
            <el-tag :type="difficultyTag(row.difficulty)" size="small">{{ difficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可练习" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.allowPractice ? 'success' : 'info'" size="small">
              {{ row.allowPractice ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total>0" v-model:current-page="current" v-model:page-size="size" :total="total"
        layout="total, prev, pager, next" @current-change="loadData" style="margin-top:16px;justify-content:flex-end" />
    </el-card>

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑题目':'添加题目'" width="580px" @closed="resetForm">
      <el-form :model="form" label-width="80px">
        <el-form-item label="所属目录" required>
          <el-tree-select
            v-model="form.categoryId"
            :data="categoryTree"
            :props="{ label: 'categoryName', value: 'id', children: 'children' }"
            placeholder="选择目录"
            check-strictly
            filterable
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="题型" required>
          <el-select v-model="form.questionType" style="width:100%" :disabled="isEdit">
            <el-option label="单选题" :value="1" /><el-option label="多选题" :value="2" />
            <el-option label="填空题" :value="3" /><el-option label="简答题" :value="4" />
            <el-option label="判断题" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty" style="width:100%">
            <el-option label="简单" :value="1" /><el-option label="中等" :value="2" /><el-option label="困难" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="允许练习">
          <el-switch v-model="form.allowPractice" active-text="可练习" inactive-text="不可练习" />
        </el-form-item>
        <el-form-item label="题目内容" required>
          <el-input v-model="form.title" type="textarea" :rows="3" placeholder="请输入题目内容" />
        </el-form-item>
        <template v-if="form.questionType===1||form.questionType===2">
          <el-form-item v-for="(_,i) in options" :key="i" :label="'选项 '+optionLabels[i]">
            <el-input v-model="options[i]" :placeholder="'选项 '+optionLabels[i]" />
          </el-form-item>
        </template>
        <el-form-item label="正确答案" required>
          <el-input v-if="form.questionType===3" v-model="form.answer" placeholder="正确答案" />
          <el-input v-else-if="form.questionType===4" v-model="form.answer" type="textarea" :rows="2" placeholder="参考答案" />
          <el-select v-else-if="form.questionType===5" v-model="form.answer" style="width:100%">
            <el-option label="正确" value="正确" /><el-option label="错误" value="错误" />
          </el-select>
          <div v-else>
            <el-input v-model="form.answer" placeholder="A/B/C/D，多选题逗号分隔" />
            <div style="font-size:12px;color:#909399;margin-top:4px">选择题填 A/B/C/D，多选题用逗号分隔</div>
          </div>
        </el-form-item>
        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" :rows="2" placeholder="选填" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 目录管理弹窗 -->
    <el-dialog v-model="categoryDialogVisible" title="题库目录管理" width="500px">
      <div style="margin-bottom:12px">
        <el-input v-model="newCategoryName" placeholder="输入新目录名称" style="width:200px" @keyup.enter="addCategory" />
        <el-button type="primary" style="margin-left:8px" @click="addCategory" :disabled="!newCategoryName.trim()">添加目录</el-button>
      </div>
      <el-table :data="flatCategories" max-height="300">
        <el-table-column prop="categoryName" label="目录名称" />
        <el-table-column label="练习" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.allowPractice"
              :active-value="1" :inactive-value="0"
              size="small"
              @change="togglePractice(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="danger" link @click="deleteCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 导库弹窗 -->
    <el-dialog v-model="showImport" title="导入题库" width="500px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px">
        请先下载模板按格式填写。列顺序：所属目录、题型、难度、允许练习、题目内容、选项A、选项B、选项C、选项D、正确答案、解析。必填：题目内容、正确答案。
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
          <p>共 {{ importResult.total }} 条，成功 {{ importResult.success }} 条{{ importResult.skip>0?`，跳过 ${importResult.skip} 条` : '' }}</p>
          <ul v-if="importResult.errors?.length" style="margin:4px 0 0;font-size:12px;max-height:80px;overflow-y:auto">
            <li v-for="(e,i) in importResult.errors" :key="i">{{ e }}</li>
          </ul>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="showImport=false;importResult=null">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="handleImportQuestions">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { getQuestionList, createQuestion, updateQuestion, deleteQuestion as delQuestion, getCategoryTree, getQuestionDetail } from '@/api/question'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, FolderAdd, Upload, UploadFilled, Download } from '@element-plus/icons-vue'
import axios from 'axios'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false); const submitting = ref(false)
const list = ref<any[]>([]); const total = ref(0)
const current = ref(1); const size = ref(10)
const typeFilter = ref<number|undefined>(undefined)
const difficultyFilter = ref<number|undefined>(undefined)
const categoryFilter = ref<number|undefined>(undefined)
const keyword = ref('')
const categoryTree = ref<any[]>([])

const dialogVisible = ref(false); const isEdit = ref(false)
const editingId = ref<number|null>(null)
const optionLabels = ['A','B','C','D','E','F']
const options = ref<string[]>(['','','',''])

const form = reactive({
  questionType:1, difficulty:1, title:'', answer:'', analysis:'',
  allowPractice: true, categoryId: undefined as number|undefined
})

// Category management
const categoryDialogVisible = ref(false)
const newCategoryName = ref('')

const flatCategories = computed(() => {
  const result: any[] = []
  function flatten(nodes: any[]) {
    for (const n of nodes) {
      result.push({ id: n.id, categoryName: n.categoryName, parentId: n.parentId, allowPractice: n.allowPractice })
      if (n.children) flatten(n.children)
    }
  }
  flatten(categoryTree.value)
  return result
})

const typeMap: Record<number,string> = {1:'单选',2:'多选',3:'填空',4:'简答',5:'判断'}
const diffMap: Record<number,string> = {1:'简单',2:'中等',3:'困难'}
function typeLabel(t:number){return typeMap[t]||'未知'}
function typeTag(t:number){return ['','','success','warning','danger','info'][t]||''}
function difficultyLabel(d:number){return diffMap[d]||'未知'}
function difficultyTag(d:number){return ['','success','warning','danger'][d]||''}

async function loadCategories() {
  try {
    const res = await getCategoryTree()
    if (res.data) categoryTree.value = res.data || []
  } catch(e) { /* ignore */ }
}

async function loadData(){
  loading.value = true
  try{
    const res = await getQuestionList({
      current:current.value, size:size.value,
      categoryId: categoryFilter.value || undefined,
      questionType:typeFilter.value, difficulty:difficultyFilter.value,
      keyword:keyword.value||undefined
    })
    if(res.data){list.value=res.data.records||[]; total.value=res.data.total||0}
  }finally{loading.value=false}
}

function openAdd(){
  isEdit.value=false; editingId.value=null
  Object.assign(form, {questionType:1, difficulty:1, title:'', answer:'', analysis:'', allowPractice:true, categoryId: undefined})
  options.value=['','','','']
  dialogVisible.value=true
}

async function openEdit(row:any){
  isEdit.value=true; editingId.value=row.id
  Object.assign(form, {
    questionType:row.questionType, difficulty:row.difficulty||1,
    title:row.title||'', answer:row.answer||'',
    analysis:row.analysis||row.answerAnalysis||'',
    allowPractice:row.allowPractice!==undefined?!!row.allowPractice:true,
    categoryId: row.categoryId || undefined
  })
  // 从详情接口加载选项（列表接口不含选项数据）
  dialogVisible.value=true
  try {
    const res = await getQuestionDetail(row.id)
    if (res.code===200 && res.data) {
      const detailQ = res.data.question || res.data
      const r = detailQ.options || detailQ.optionList || []
      options.value = r.length ? r : ['','','','']
      // 提取选项文本用于编辑
      if (r.length > 0) {
        const opts = r.map((o: any) => o.optionContent || o.content || '')
        while (opts.length < 4) opts.push('')
        options.value = opts
      }
    }
  } catch { /* ignore */ }
}

function resetForm(){}

async function handleSubmit(){
  if(!form.categoryId){ ElMessage.warning('请选择所属目录'); return }
  if(!form.title.trim()){ ElMessage.warning('请输入题目内容'); return }
  if(!form.answer.trim()){ ElMessage.warning('请输入正确答案'); return }
  submitting.value = true
  try{
    const data:any={
      id: editingId.value,
      categoryId: form.categoryId,
      questionType: form.questionType,
      difficulty: form.difficulty,
      title: form.title,
      answer: form.answer.trim(),
      answerAnalysis: form.analysis,
      allowPractice: form.allowPractice ? 1 : 0
    }
    if(form.questionType===1||form.questionType===2){
      const vo=options.value.filter(o=>o.trim())
      if(vo.length<2){ ElMessage.warning('至少需要2个选项'); submitting.value=false; return }
      vo.forEach((o,i)=>{ data['option'+optionLabels[i]]=o })
    }
    if(isEdit.value){ await updateQuestion(data); ElMessage.success('已更新') }
    else { await createQuestion(data); ElMessage.success('已添加') }
    dialogVisible.value=false; loadData()
  }finally{ submitting.value=false }
}

async function handleDelete(row:any){
  try{
    await ElMessageBox.confirm('确定删除？','确认',{type:'warning'})
    await delQuestion(row.id); ElMessage.success('已删除'); loadData()
  }catch{}
}

async function addCategory(){
  if(!newCategoryName.value.trim()) return
  try {
    await request.post('/question/category/create', { categoryName: newCategoryName.value.trim(), parentId: 0 })
    ElMessage.success('目录已添加')
    newCategoryName.value = ''
    loadCategories()
  } catch(e: any) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '添加失败')
  }
}

async function togglePractice(row: any) {
  try {
    await request.put(`/question/category/${row.id}/toggle-practice`)
  } catch {
    // 回滚开关
    row.allowPractice = row.allowPractice === 1 ? 0 : 1
    ElMessage.error('操作失败')
  }
}

async function deleteCategory(row: any){
  try {
    await ElMessageBox.confirm(`确定删除目录"${row.categoryName}"？若该目录下无题目才可删除。`, '确认', {type:'warning'})
    await request.delete(`/question/category/delete/${row.id}`)
    ElMessage.success('已删除')
    loadCategories(); loadData()
  }catch{}
}

// --- 导入 ---
const showImport = ref(false)
const importing = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<any>(null)

async function downloadQuestionTemplate() {
  try {
    const token = userStore.accessToken
    const res = await axios.get('/api/v1/admin/import/template/questions', {
      responseType: 'blob',
      headers: { Authorization: `Bearer ${token}` }
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a'); a.href = url; a.download = '题库导入模板.xlsx'; a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('下载失败') }
}

async function handleImportQuestions() {
  if (!importFile.value) return
  importing.value = true
  try {
    const fd = new FormData(); fd.append('file', importFile.value)
    const r = await request.post('/admin/import/questions', fd, { headers: { 'Content-Type': undefined } })
    importResult.value = r.data
    if (r.data.success > 0) { ElMessage.success(`成功导入 ${r.data.success} 题`); loadData(); loadCategories() }
  } catch { ElMessage.error('导入失败') }
  finally { importing.value = false }
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<style scoped>
.admin-page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
</style>
