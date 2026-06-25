<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>👥 用户管理</h2>
      <div style="display:flex;gap:8px">
        <el-button @click="downloadUserTemplate"><el-icon><Download /></el-icon> 模板</el-button>
        <el-button type="success" @click="showImport=true"><el-icon><Upload /></el-icon> 导入用户</el-button>
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增用户</el-button>
      </div>
    </div>

    <el-card>
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索用户名/昵称/邮箱..." clearable style="width:240px" @keyup.enter="loadData" @clear="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="roleFilter" placeholder="角色" clearable style="width:120px" @change="loadData">
          <el-option label="学生" value="STUDENT" />
          <el-option label="教师" value="TEACHER" />
          <el-option label="管理员" value="SUPER_ADMIN" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe style="width:100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="username" label="用户名" width="110" />
        <el-table-column prop="nickname" label="昵称" width="110" />
        <el-table-column label="角色" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTag(row.roles?.[0]?.roleCode || '')" size="small" effect="plain">
              {{ roleLabel(row.roles?.[0]?.roleCode || '学生') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
        <el-table-column label="登录次数" width="85" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.loginCount > 10 ? '#67c23a' : '#909399' }">{{ row.loginCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="155">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button type="warning" link size="small" @click="openResetPwd(row)">
              <el-icon><Lock /></el-icon> 密码
            </el-button>
            <el-popconfirm
              v-if="row.roles?.[0]?.roleCode!=='SUPER_ADMIN' && row.id!==userStore.userId"
              title="确定删除此用户？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button type="danger" link size="small">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-popconfirm>
            <el-button v-else type="info" link size="small" disabled>
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total>0" v-model:current-page="current" v-model:page-size="size" :total="total"
        layout="total, prev, pager, next" @current-change="loadData" style="margin-top:16px;justify-content:flex-end" />
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="480px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="editForm.username" disabled /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="editForm.nickname" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width:100%" :disabled="editForm.id === userStore.userId">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="SUPER_ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item>
        <el-form-item label="学校"><el-input v-model="editForm.school" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="editForm.grade" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="重置密码" width="400px">
      <el-form label-width="100px">
        <el-form-item label="用户">
          <el-tag>{{ pwdUser?.username }}</el-tag>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" placeholder="留空则重置为 123456" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible=false">取消</el-button>
        <el-button type="warning" :loading="pwdSaving" @click="doResetPwd">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- 新增用户弹窗 -->
    <el-dialog v-model="addVisible" title="新增用户" width="520px">
      <el-form :model="addForm" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" required>
              <el-input v-model="addForm.username" placeholder="登录用户名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" required>
              <el-input v-model="addForm.password" type="password" placeholder="默认密码" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="昵称" required>
              <el-input v-model="addForm.nickname" placeholder="显示昵称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="addForm.role" style="width:100%">
                <el-option label="学生" value="STUDENT" />
                <el-option label="教师" value="TEACHER" />
                <el-option label="管理员" value="SUPER_ADMIN" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="addForm.email" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="addForm.phone" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学校">
              <el-input v-model="addForm.school" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级">
              <el-input v-model="addForm.grade" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAdd">确认新增</el-button>
      </template>
    </el-dialog>

    <!-- 导入用户弹窗 -->
    <el-dialog v-model="showImport" title="导入用户" width="500px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px">
        请先下载模板，按格式填写后上传。必填字段：用户名、昵称、密码。
      </el-alert>
      <el-upload
        ref="userUploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="(f:any)=>{importFile=f.raw}"
        :on-remove="()=>{importFile=null}"
      >
        <el-icon :size="40"><UploadFilled /></el-icon>
        <div style="margin-top:8px">拖拽或点击上传 Excel 文件</div>
        <template #tip><div style="font-size:12px;color:#909399">仅支持 .xlsx / .xls</div></template>
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
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="handleImportUsers">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Plus, Search, Edit, Lock, Delete, Upload, UploadFilled, Download } from '@element-plus/icons-vue'
import axios from 'axios'
import dayjs from 'dayjs'

const userStore = useUserStore()
const loading = ref(false); const saving = ref(false)
const list = ref<any[]>([]); const total = ref(0)
const current = ref(1); const size = ref(10)
const keyword = ref('')
const roleFilter = ref<string|undefined>(undefined)

const editVisible = ref(false)
const editForm = reactive({ id:0, username:'', nickname:'', role:'', email:'', phone:'', school:'', grade:'' })

const pwdVisible = ref(false); const pwdSaving = ref(false)
const pwdUser = ref<any>(null); const newPassword = ref('')

const addVisible = ref(false); const adding = ref(false)
const addForm = reactive({ username:'', password:'123456', nickname:'', role:'STUDENT', email:'', phone:'', school:'', grade:'' })

const roleMap: Record<string,string> = { STUDENT:'学生', TEACHER:'教师', SUPER_ADMIN:'管理员' }
function roleLabel(r:string){ return roleMap[r]||r }
function roleTag(r:string){ return r==='SUPER_ADMIN'?'danger':r==='TEACHER'?'warning':'success' }
function formatTime(t:string){ return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '--' }

async function loadData() {
  loading.value = true
  try {
    const params: any = { current: current.value, size: size.value }
    if(keyword.value) params.keyword = keyword.value
    if(roleFilter.value) params.role = roleFilter.value
    const res = await request.get('/user/list',{params})
    if(res.data){ list.value = res.data.records||[]; total.value = res.data.total||0 }
  } finally { loading.value = false }
}

function openEdit(row: any) {
  Object.assign(editForm, {
    id:row.id, username:row.username, nickname:row.nickname||'',
    role: row.roles?.[0]?.roleCode || 'STUDENT',
    email:row.email||'', phone:row.phone||'', school:row.school||'',
    grade:row.grade||''
  })
  editVisible.value = true
}

async function saveEdit() {
  if (editForm.id === userStore.userId) {
    ElMessage.warning('不能修改自己的角色')
    return
  }
  saving.value = true
  try {
    await request.put(`/user/${editForm.id}/role`, { role: editForm.role })
    ElMessage.success('已保存')
    editVisible.value = false
    loadData()
  } finally { saving.value = false }
}

function openResetPwd(row: any) {
  pwdUser.value = row; newPassword.value = ''
  pwdVisible.value = true
}

async function doResetPwd() {
  pwdSaving.value = true
  try {
    const pw = newPassword.value || '123456'
    await request.put(`/user/${pwdUser.value.id}/reset-password`, { newPassword: pw })
    ElMessage.success('密码已重置')
    pwdVisible.value = false
  } finally { pwdSaving.value = false }
}

async function handleDelete(row: any) {
  if(row.id===userStore.userId){ ElMessage.warning('不能删除自己'); return }
  await request.delete(`/user/${row.id}`)
  ElMessage.success('已删除'); loadData()
}

function openAdd() {
  Object.assign(addForm, { username:'', password:'123456', nickname:'', role:'STUDENT', email:'', phone:'', school:'', grade:'' })
  addVisible.value = true
}

async function handleAdd() {
  if (!addForm.username.trim()) { ElMessage.warning('请输入用户名'); return }
  if (!addForm.nickname.trim()) { ElMessage.warning('请输入昵称'); return }
  adding.value = true
  try {
    await request.post('/auth/register', {
      username: addForm.username, password: addForm.password || '123456',
      confirmPassword: addForm.password || '123456',
      nickname: addForm.nickname,
      email: addForm.email || (addForm.username + '@example.com'),
      phone: addForm.phone || ''
    })
    if (addForm.role !== 'STUDENT') {
      setTimeout(async () => {
        const res = await request.get('/user/list', { params: { keyword: addForm.username, size: 1 } })
        if (res.data?.records?.length > 0) {
          await request.put(`/user/${res.data.records[0].id}/role`, { role: addForm.role }).catch(()=>{})
        }
      }, 500)
    }
    ElMessage.success('已新增')
    addVisible.value = false
    setTimeout(() => loadData(), 1000)
  } finally { adding.value = false }
}

// --- 导入 ---
const showImport = ref(false)
const importing = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<any>(null)

async function downloadUserTemplate() {
  try {
    const token = userStore.accessToken
    const res = await axios.get('/api/v1/admin/import/template/users', {
      responseType: 'blob',
      headers: { Authorization: `Bearer ${token}` }
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a'); a.href = url; a.download = '用户导入模板.xlsx'; a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('下载失败') }
}

async function handleImportUsers() {
  if (!importFile.value) return
  importing.value = true
  try {
    const fd = new FormData(); fd.append('file', importFile.value)
    const r = await request.post('/admin/import/users', fd, { headers: { 'Content-Type': undefined } })
    importResult.value = r.data
    if (r.data.success > 0) ElMessage.success(`成功导入 ${r.data.success} 人`); loadData()
  } catch { ElMessage.error('导入失败') }
  finally { importing.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.admin-page { padding: 24px; max-width: 1400px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
</style>
