<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>🏫 班级管理</h2>
      <el-button type="primary" @click="showCreate=true">
        <el-icon><Plus /></el-icon> 创建班级
      </el-button>
    </div>

    <el-card>
      <div class="filter-bar">
        <el-select v-if="userStore.isAdmin" v-model="selectedTeacher" placeholder="按教师筛选" clearable style="width:220px" @change="onTeacherChange">
          <el-option v-for="t in teacherList" :key="t.id" :label="`${t.name}（${t.classCount}个班级）`" :value="t.id" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索班级名称..." clearable style="width:220px" @keyup.enter="loadData" @clear="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe style="width:100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="班级名称" min-width="140">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewMembers(row)">{{ row.className }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="口令" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small" effect="plain" style="font-family:monospace">{{ row.classCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="创建教师" min-width="100" />
        <el-table-column prop="school" label="学校" min-width="100" />
        <el-table-column prop="grade" label="年级" width="80" align="center" />
        <el-table-column label="人数" width="90" align="center">
          <template #default="{ row }">
            <span :style="{ color: (row.studentCount||0) >= (row.maxStudents||50) ? 'var(--color-danger)' : 'var(--color-success)' }">
              {{ row.studentCount || 0 }}/{{ row.maxStudents || 50 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':'info'" size="small" effect="plain">
              {{ row.status===1?'正常':'已解散' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="155">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewMembers(row)">
              <el-icon><User /></el-icon> 成员
            </el-button>
            <el-button v-if="row.status===1" type="warning" link size="small" @click="openEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button v-if="row.status===1" type="success" link size="small" @click="handleRegenCode(row)">
              <el-icon><RefreshRight /></el-icon> 口令
            </el-button>
            <el-popconfirm
              v-if="row.status===1"
              title="确定解散此班级？"
              @confirm="handleDismiss(row)"
            >
              <template #reference>
                <el-button type="danger" link size="small">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total>0" v-model:current-page="current" v-model:page-size="size" :total="total"
        layout="total, prev, pager, next" @current-change="loadData" style="margin-top:16px;justify-content:flex-end" />
    </el-card>

    <!-- 成员列表弹窗 -->
    <el-dialog v-model="showMembers" :title="'班级成员 - ' + currentClass?.className" width="550px">
      <el-table :data="memberList" v-loading="memberLoading" stripe size="small" max-height="360">
        <el-table-column type="index" label="#" width="45" align="center" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column label="角色" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role===1?'warning':'success'" size="small" effect="plain">
              {{ row.role===1?'教师':'学生' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="加入时间" width="155">
          <template #default="{ row }">{{ formatTime(row.joinTime || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ row: member }">
            <el-popconfirm
              v-if="member.role !== 1"
              title="确定移除此学生？"
              @confirm="handleRemoveStudent(member)"
            >
              <template #reference>
                <el-button type="danger" link size="small">移除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 创建班级弹窗 -->
    <el-dialog v-model="showCreate" title="创建班级" width="480px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="班级名称" required>
          <el-input v-model="createForm.className" placeholder="请输入班级名称" maxlength="20" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学校">
              <el-input v-model="createForm.school" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级">
              <el-select v-model="createForm.grade" placeholder="选填" style="width:100%" clearable>
                <el-option v-for="g in grades" :key="g" :label="g" :value="g" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="最大人数">
          <el-input-number v-model="createForm.maxStudents" :min="10" :max="500" style="width:100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate=false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑班级弹窗 -->
    <el-dialog v-model="showEdit" title="编辑班级" width="480px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="班级名称" required>
          <el-input v-model="editForm.className" placeholder="请输入班级名称" maxlength="20" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学校">
              <el-input v-model="editForm.school" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级">
              <el-select v-model="editForm.grade" placeholder="选填" style="width:100%" clearable>
                <el-option v-for="g in grades" :key="g" :label="g" :value="g" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="最大人数">
          <el-input-number v-model="editForm.maxStudents" :min="5" :max="1000" style="width:100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, User, Delete, Edit, RefreshRight } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const userStore = useUserStore()

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const keyword = ref('')
const selectedTeacher = ref<number | undefined>(undefined)
const teacherList = ref<any[]>([])

const showMembers = ref(false)
const memberList = ref<any[]>([])
const memberLoading = ref(false)
const currentClass = ref<any>(null)

const showCreate = ref(false)
const creating = ref(false)
const createForm = reactive({ className: '', school: '', grade: '', maxStudents: 100, description: '' })

const showEdit = ref(false)
const saving = ref(false)
const editForm = reactive({ id: 0, className: '', school: '', grade: '', maxStudents: 100, description: '' })
const grades = ['初一', '初二', '初三', '高一', '高二', '高三', '大一', '大二', '大三', '大四']

function formatTime(t: string) { return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '--' }

async function loadData() {
  loading.value = true
  try {
    const params: any = { current: current.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (selectedTeacher.value) params.teacherId = selectedTeacher.value
    const res = await request.get('/class/list', { params })
    if (res.data) { list.value = res.data.records || []; total.value = res.data.total || 0 }
  } finally { loading.value = false }
}

async function loadTeachers() {
  try { const res = await request.get('/class/teachers'); teacherList.value = res.data || [] } catch {}
}

function onTeacherChange() { current.value = 1; loadData() }

async function viewMembers(row: any) {
  currentClass.value = row
  showMembers.value = true
  memberLoading.value = true
  try {
    const res = await request.get(`/class/members/${row.id}`)
    memberList.value = res.data || []
  } finally { memberLoading.value = false }
}

async function handleDismiss(row: any) {
  try {
    await ElMessageBox.confirm(`确定解散班级「${row.className}」？`, '确认解散', { type: 'warning' })
  } catch { return }
  try {
    await request.delete(`/class/${row.id}`)
    ElMessage.success('已解散')
    list.value = list.value.filter((item: any) => item.id !== row.id)
    total.value = Math.max(0, total.value - 1)
  } catch {}
}

async function handleCreate() {
  if (!createForm.className.trim()) { ElMessage.warning('请输入班级名称'); return }
  creating.value = true
  try {
    await request.post('/class/create', { ...createForm })
    ElMessage.success('创建成功')
    showCreate.value = false
    Object.assign(createForm, { className: '', school: '', grade: '', maxStudents: 100, description: '' })
    loadData()
  } finally { creating.value = false }
}

function openEdit(row: any) {
  editForm.id = row.id
  editForm.className = row.className || ''
  editForm.school = row.school || ''
  editForm.grade = row.grade || ''
  editForm.maxStudents = row.maxStudents || 100
  editForm.description = row.description || ''
  showEdit.value = true
}

async function handleSave() {
  if (!editForm.className.trim()) { ElMessage.warning('请输入班级名称'); return }
  saving.value = true
  try {
    await request.put(`/class/${editForm.id}`, { ...editForm })
    ElMessage.success('保存成功')
    showEdit.value = false
    loadData()
  } finally { saving.value = false }
}

async function handleRegenCode(row: any) {
  try {
    await ElMessageBox.confirm(`确定重新生成「${row.className}」的口令？原口令将失效。`, '确认', { type: 'warning' })
  } catch { return }
  try {
    const res = await request.put(`/class/${row.id}/regenerate-code`)
    const newCode = res.data || ''
    ElMessage.success(`新口令：${newCode}`)
    row.classCode = newCode
  } catch {}
}

async function handleRemoveStudent(member: any) {
  try {
    await request.delete(`/class/members/${currentClass.value.id}/user/${member.userId}`)
    ElMessage.success(`已移除 ${member.nickname || member.username}`)
    memberList.value = memberList.value.filter((m: any) => m.userId !== member.userId)
    // 更新列表中的班级人数
    if (currentClass.value) currentClass.value.studentCount = Math.max(0, (currentClass.value.studentCount || 1) - 1)
    const idx = list.value.findIndex((item: any) => item.id === currentClass.value.id)
    if (idx >= 0) list.value[idx].studentCount = currentClass.value.studentCount
  } catch {}
}

onMounted(() => { loadTeachers(); loadData() })
</script>

<style scoped>
/* page-specific overrides */
</style>
