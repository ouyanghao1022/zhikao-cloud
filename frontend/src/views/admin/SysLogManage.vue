<template>
  <div class="syslog-page">
    <div class="page-header"><h2>系统日志</h2></div>

    <el-tabs v-model="activeTab">
      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="oper">
        <el-form inline class="filter-form">
          <el-form-item label="用户名">
            <el-input v-model="operQuery.username" placeholder="模糊查询" clearable style="width:160px" @keyup.enter="loadOper(1)" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="operQuery.status" clearable placeholder="全部" style="width:120px">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadOper(1)">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="operLogs" v-loading="operLoading" stripe size="small" border>
          <el-table-column prop="username" label="用户" width="120" show-overflow-tooltip />
          <el-table-column prop="operation" label="操作" min-width="180" show-overflow-tooltip />
          <el-table-column prop="method" label="请求方法" min-width="200" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="duration" label="耗时(ms)" width="100" align="center" />
          <el-table-column prop="errorMsg" label="错误信息" min-width="160" show-overflow-tooltip />
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="operPage.current"
          :page-size="operPage.size"
          :total="operPage.total"
          layout="total, prev, pager, next, jumper"
          @current-change="loadOper"
          class="pager"
        />
      </el-tab-pane>

      <!-- 登录日志 -->
      <el-tab-pane label="登录日志" name="login">
        <el-form inline class="filter-form">
          <el-form-item label="用户名">
            <el-input v-model="loginQuery.username" placeholder="模糊查询" clearable style="width:160px" @keyup.enter="loadLogin(1)" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="loginQuery.status" clearable placeholder="全部" style="width:120px">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadLogin(1)">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="loginLogs" v-loading="loginLoading" stripe size="small" border>
          <el-table-column prop="username" label="用户名" width="130" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="location" label="地点" width="120" show-overflow-tooltip />
          <el-table-column prop="browser" label="浏览器" width="130" show-overflow-tooltip />
          <el-table-column prop="os" label="操作系统" width="130" show-overflow-tooltip />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="msg" label="信息" min-width="140" show-overflow-tooltip />
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="loginPage.current"
          :page-size="loginPage.size"
          :total="loginPage.total"
          layout="total, prev, pager, next, jumper"
          @current-change="loadLogin"
          class="pager"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { getOperLogs, getLoginLogs } from '@/api/log'

const activeTab = ref('oper')

function formatTime(t: string) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '--'
}

// 操作日志
const operQuery = reactive<{ username: string; status: number | undefined }>({ username: '', status: undefined })
const operPage = reactive({ current: 1, size: 15, total: 0 })
const operLogs = ref<any[]>([])
const operLoading = ref(false)

async function loadOper(page?: number) {
  if (page) operPage.current = page
  operLoading.value = true
  try {
    const res: any = await getOperLogs({
      page: operPage.current,
      size: operPage.size,
      username: operQuery.username || undefined,
      status: operQuery.status
    })
    const data = res.data || {}
    operLogs.value = data.records || []
    operPage.total = data.total || 0
  } catch { operLogs.value = [] } finally { operLoading.value = false }
}

// 登录日志
const loginQuery = reactive<{ username: string; status: number | undefined }>({ username: '', status: undefined })
const loginPage = reactive({ current: 1, size: 15, total: 0 })
const loginLogs = ref<any[]>([])
const loginLoading = ref(false)

async function loadLogin(page?: number) {
  if (page) loginPage.current = page
  loginLoading.value = true
  try {
    const res: any = await getLoginLogs({
      page: loginPage.current,
      size: loginPage.size,
      username: loginQuery.username || undefined,
      status: loginQuery.status
    })
    const data = res.data || {}
    loginLogs.value = data.records || []
    loginPage.total = data.total || 0
  } catch { loginLogs.value = [] } finally { loginLoading.value = false }
}

onMounted(() => {
  loadOper(1)
  loadLogin(1)
})
</script>

<style scoped>
.syslog-page { padding: 20px 24px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; color: #303133; }
.filter-form { margin-bottom: 12px; }
.pager { margin-top: 14px; display: flex; justify-content: flex-end; }
</style>
