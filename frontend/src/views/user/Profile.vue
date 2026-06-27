<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>个人中心</h2>
    </div>

    <!-- 上排：用户卡片 + 统计与班级 -->
    <div class="top-row">
      <div class="user-card">
        <div class="avatar-wrap">
          <el-avatar :size="68" :src="profile.avatar || undefined" class="avatar" @click="showAvatarDialog = true">
            {{ profile.avatar ? '' : (profile.nickname?.[0] || 'U') }}
          </el-avatar>
          <div class="camera-dot" @click="showAvatarDialog = true"><el-icon><Camera /></el-icon></div>
        </div>
        <div class="user-body">
          <div class="user-name">{{ profile.nickname }}</div>
          <div class="user-sub">
            <el-tag :type="roleType" size="small" effect="light">{{ roleLabel }}</el-tag>
            <span class="user-id">@{{ profile.username }}</span>
          </div>
        </div>
        <div class="user-btns">
          <el-button size="small" type="primary" @click="startEdit">
            <el-icon><Edit /></el-icon>编辑资料
          </el-button>
          <el-button size="small" @click="openPasswordDialog">
            <el-icon><Lock /></el-icon>修改密码
          </el-button>
        </div>
      </div>

      <div class="stats-class-group">
        <!-- 统计 -->
        <div class="stats-block">
          <div class="block-label">
            <el-icon><DataLine /></el-icon>
            {{ userStore.isAdmin ? '系统概览' : userStore.isTeacher ? '教学统计' : '学习统计' }}
          </div>
          <div class="stats-row">
            <template v-if="userStore.isAdmin">
              <div class="stat"><b>{{ stats.userCount ?? 0 }}</b><span>用户总数</span></div>
              <div class="stat"><b>{{ stats.examCount ?? 0 }}</b><span>试卷总数</span></div>
              <div class="stat"><b>{{ stats.questionCount ?? 0 }}</b><span>题目总数</span></div>
              <div class="stat"><b>{{ stats.classCount ?? 0 }}</b><span>班级总数</span></div>
            </template>
            <template v-else-if="userStore.isTeacher">
              <div class="stat"><b>{{ stats.classCount ?? 0 }}</b><span>我的班级</span></div>
              <div class="stat"><b>{{ stats.studentCount ?? 0 }}</b><span>学生总数</span></div>
              <div class="stat"><b>{{ stats.examPaperCount ?? 0 }}</b><span>试卷数</span></div>
              <div class="stat"><b>{{ stats.examCount ?? 0 }}</b><span>考试记录</span></div>
            </template>
            <template v-else>
              <div class="stat"><b>{{ stats.examCount ?? 0 }}</b><span>考试次数</span></div>
              <div class="stat"><b>{{ stats.correctRate > 0 ? stats.correctRate + '%' : '0%' }}</b><span>正确率</span></div>
              <div class="stat"><b>{{ stats.wrongBookCount ?? 0 }}</b><span>错题本</span></div>
              <div class="stat"><b>{{ stats.favoriteCount ?? 0 }}</b><span>收藏数</span></div>
            </template>
          </div>
        </div>

        <!-- 我的班级 -->
        <div v-if="!userStore.isAdmin" class="class-block">
          <div class="block-label">
            <el-icon><School /></el-icon> 我的班级
            <div class="block-actions">
              <el-button v-if="!userStore.isTeacher && myClasses.length > 0" type="danger" plain size="small" @click="handleLeaveClass">退出</el-button>
              <el-button v-if="!userStore.isTeacher && myClasses.length === 0" type="primary" size="small" @click="openJoinClassDialog"><el-icon><Plus /></el-icon>加入</el-button>
              <el-button v-if="userStore.isTeacher" type="success" size="small" @click="openCreateClassDialog"><el-icon><Plus /></el-icon>创建</el-button>
            </div>
          </div>
          <div v-if="myClasses.length === 0" class="class-empty">
            <span>暂未加入任何班级</span>
          </div>
          <div v-else class="class-items">
            <div v-for="c in myClasses" :key="c.id" class="class-item"
                 @click="!userStore.isTeacher && showClassMembers(c)">
              <span class="cname">{{ c.className }}</span>
              <span class="cgrade">{{ c.grade }}</span>
              <code class="ccode">{{ c.classCode }}</code>
              <span class="ccount">{{ c.studentCount || 0 }}人</span>
              <el-button v-if="userStore.isTeacher" type="primary" size="small" class="class-manage-btn"
                         @click.stop="openClassManage(c)">管理</el-button>
            </div>
          </div>
        </div>

        <div v-if="userStore.isAdmin" class="class-block admin-block">
          <el-icon :size="24" color="#409EFF"><Monitor /></el-icon>
          <span>超级管理员 · 系统全权限</span>
        </div>
      </div>
    </div>

    <!-- 下排：基本信息 -->
    <div class="bottom-row">
      <div class="info-card">
        <div class="block-label">
          <el-icon><User /></el-icon> 基本信息
        </div>

        <div v-if="!editMode" class="info-grid">
          <div class="info-item"><label>用户名</label><span>{{ profile.username }}</span></div>
          <div class="info-item"><label>昵称</label><span>{{ profile.nickname }}</span></div>
          <div class="info-item"><label>角色</label><span><el-tag size="small" :type="roleType">{{ roleLabel }}</el-tag></span></div>
          <div class="info-item"><label>性别</label><span>{{ genderLabel }}</span></div>
          <div class="info-item"><label>邮箱</label><span>{{ profile.email || '—' }}</span></div>
          <div class="info-item"><label>手机号</label><span>{{ profile.phone || '—' }}</span></div>
          <div class="info-item"><label>学校</label><span>{{ profile.school || '—' }}</span></div>
          <div class="info-item"><label>年级</label><span>{{ displayGrade }}</span></div>
          <div class="info-item"><label>注册时间</label><span>{{ formatTime(profile.createdAt) }}</span></div>
          <div class="info-item"><label>最近登录</label><span>{{ formatTime(profile.lastLoginTime) }}</span></div>
          <div class="info-item"><label>登录次数</label><span>{{ profile.loginCount || 0 }} 次</span></div>
          <div class="info-item"><label>个性签名</label><span class="sig">{{ profile.signature || '—' }}</span></div>
        </div>

        <div v-else class="edit-form">
          <el-form :model="editForm" label-width="80px" size="default">
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="昵称"><el-input v-model="editForm.nickname" /></el-form-item>
                <el-form-item label="性别">
                  <el-select v-model="editForm.gender" style="width:100%">
                    <el-option label="未知" :value="0" /><el-option label="男" :value="1" /><el-option label="女" :value="2" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
                <el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="学校"><el-input v-model="editForm.school" /></el-form-item>
                <el-form-item label="年级">
                  <template v-if="!userStore.isTeacher && !userStore.isAdmin && myClasses.length > 0">
                    <el-input :model-value="displayGrade" disabled placeholder="由班级决定" />
                    <span style="font-size:11px;color:#909399;line-height:1.6">学生年级由所在班级决定</span>
                  </template>
                  <el-select v-else v-model="editForm.grade" style="width:100%">
                    <el-option v-for="g in grades" :key="g" :label="g" :value="g" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="签名"><el-input v-model="editForm.signature" type="textarea" :rows="2" /></el-form-item>
            <el-form-item><el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button><el-button @click="editMode = false">取消</el-button></el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 学习成就：等级/证书/积分明细 -->
    <div class="achievement-row" v-if="!userStore.isAdmin">
      <div class="ach-card">
        <div class="block-label"><el-icon><Trophy /></el-icon> 等级与积分</div>
        <div class="level-row">
          <el-tag size="large" type="warning" effect="dark">{{ pointsProfile.levelName || '青铜' }}</el-tag>
          <div class="exp-info">
            <span>经验值 <b>{{ pointsProfile.experience ?? 0 }}</b></span>
            <span>积分 <b>{{ pointsProfile.integration ?? 0 }}</b></span>
            <span v-if="pointsProfile.continuousSignDays">连续签到 <b>{{ pointsProfile.continuousSignDays }}</b> 天</span>
          </div>
        </div>
        <el-progress :percentage="levelProgress" :stroke-width="10" color="#e6a23c" />
      </div>

      <div class="ach-card">
        <div class="block-label"><el-icon><Medal /></el-icon> 我的证书</div>
        <el-empty v-if="!certs.length" description="暂无证书，通过考试即可获得" :image-size="50" />
        <div v-else class="cert-list">
          <div v-for="c in certs" :key="c.id" class="cert-item">
            <div class="cert-info">
              <span class="cert-no">证书编号 {{ c.certNo }}</span>
              <span class="cert-time">{{ formatTime(c.issueTime) }}</span>
            </div>
            <el-button size="small" type="primary" link @click="viewCert(c.sessionId)">查看证书</el-button>
          </div>
        </div>
      </div>

      <div class="ach-card">
        <div class="block-label"><el-icon><List /></el-icon> 积分变动明细</div>
        <el-empty v-if="!integralLogs.length" description="暂无积分记录" :image-size="50" />
        <el-table v-else :data="integralLogs" size="small" max-height="240" stripe>
          <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
          <el-table-column label="变动" width="80" align="center">
            <template #default="{ row }">
              <span :class="row.changeValue >= 0 ? 'pos' : 'neg'">
                {{ row.changeValue >= 0 ? '+' : '' }}{{ row.changeValue }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="currentValue" label="变动后" width="80" align="center" />
          <el-table-column label="时间" width="150">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 对话框 -->
    <el-dialog v-model="showAvatarDialog" title="修改头像" width="420px" align-center>
      <AvatarCropper v-if="showAvatarDialog" @cropped="onAvatarCropped" />
      <template #footer>
        <el-button @click="showAvatarDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingAvatar" :disabled="!croppedAvatarBlob" @click="saveAvatar">
          上传头像
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPwdDialog" title="修改密码" width="380px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="pwdForm.confirmPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showPwdDialog = false">取消</el-button><el-button type="primary" :loading="savingPwd" @click="savePassword">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="showJoinClassDialog" title="加入班级" width="360px">
      <el-form label-width="80px"><el-form-item label="口令"><el-input v-model="joinClassCode" placeholder="6位班级口令" maxlength="6" /></el-form-item></el-form>
      <template #footer><el-button @click="showJoinClassDialog = false">取消</el-button><el-button type="primary" :loading="joiningClass" @click="handleJoinClass">加入</el-button></template>
    </el-dialog>

    <el-dialog v-model="showCreateClassDialog" title="创建班级" width="450px">
      <el-form :model="createClassForm" label-width="80px">
        <el-form-item label="名称" required><el-input v-model="createClassForm.className" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="学校"><el-input v-model="createClassForm.school" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="年级"><el-select v-model="createClassForm.grade" style="width:100%"><el-option v-for="g in grades" :key="g" :label="g" :value="g" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="上限"><el-input-number v-model="createClassForm.maxStudents" :min="10" :max="500" style="width:100%" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="createClassForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showCreateClassDialog = false">取消</el-button><el-button type="primary" :loading="creatingClass" @click="handleCreateClass">创建</el-button></template>
    </el-dialog>

    <el-dialog v-model="showMembersDialog" :title="'班级成员 - ' + currentClassName" width="560px">
      <el-table :data="classMembers" stripe size="small" max-height="350">
        <el-table-column type="index" label="#" width="45" align="center" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column label="角色" width="80" align="center">
          <template #default="{ row }"><el-tag :type="row.role===1?'warning':'success'" size="small">{{ row.role===1?'教师':'学生' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="加入时间" width="150">
          <template #default="{ row }">{{ row.joinTime || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="userStore.isTeacher" label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-popconfirm
              v-if="row.role !== 1"
              title="确定将此学生移出班级？"
              @confirm="handleRemoveStudent(row)"
            >
              <template #reference>
                <el-button type="danger" link size="small">移除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Plus, Edit, Lock, Camera, DataLine, School, User, Monitor, Trophy, Medal, List } from '@element-plus/icons-vue'
import { updateProfile, changePassword, getMyDashboard, getPointsProfile, getLevelConfig, getIntegralLogs } from '@/api/profile'
import { getMyClasses, joinClass, createClass, getClassMembers, leaveClass, removeStudent } from '@/api/class'
import { getMyCertificates } from '@/api/cert'
import axios from 'axios'
import dayjs from 'dayjs'
import AvatarCropper from '@/components/AvatarCropper.vue'

const router = useRouter()
const userStore = useUserStore()

const editMode = ref(false); const saving = ref(false)
const showAvatarDialog = ref(false); const savingAvatar = ref(false)
const croppedAvatarBlob = ref<Blob | null>(null)
const croppedAvatarPreview = ref('')
const showPwdDialog = ref(false); const savingPwd = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const profile = reactive<any>({ username: '', nickname: '', email: '', phone: '', gender: 0, avatar: '', school: '', grade: '', signature: '', role: '', createdAt: '', lastLoginTime: '' })
const editForm = reactive<any>({ nickname: '', email: '', phone: '', gender: 0, school: '', grade: '', signature: '' })
const stats = reactive<Record<string, number>>({})
const grades = ['初一','初二','初三','高一','高二','高三','大一','大二','大三','大四']

const roleType = computed(() => userStore.isAdmin ? 'danger' : userStore.isTeacher ? 'warning' : 'success')
const roleLabel = computed(() => userStore.isAdmin ? '超级管理员' : userStore.isTeacher ? '教师' : '学生')
const genderLabel = computed(() => ({ 0:'未知',1:'男',2:'女' } as Record<number,string>)[profile.gender] || '未知')
// 防御性显示：若用户年级为空，自动用班级年级填充（解决历史数据不一致问题）
const displayGrade = computed(() => {
  if (profile.grade) return profile.grade
  const cls = myClasses.value.find((c: any) => c.grade)
  return cls ? cls.grade : '—'
})
function formatTime(t:string) { return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '--' }

function startEdit() {
  // 防御性同步：确保编辑表单的年级与显示年级一致
  if (!editForm.grade && displayGrade.value !== '—') editForm.grade = displayGrade.value
  editMode.value = true
}

async function fetchStats() { try { const r = await getMyDashboard(); if (r.code===200&&r.data) Object.assign(stats,r.data) } catch {} }
async function saveProfile() { saving.value=true; try {
  const data = { ...editForm }
  // 学生：未加入班级时允许自由修改年级；已加入班级时年级锁定，剔除 grade
  if (!userStore.isTeacher && !userStore.isAdmin) {
    if (myClasses.value.length > 0) {
      delete data.grade  // 已加入班级，年级由班级决定
    }
    // myClasses.length === 0：未加入班级，保留 grade 让后端正常写入
  }
  await updateProfile(data); ElMessage.success('保存成功'); Object.assign(profile,editForm); editMode.value=false
} finally { saving.value=false } }

/** AvatarCropper 裁剪完成回调 */
function onAvatarCropped(blob: Blob, previewUrl: string) {
  croppedAvatarBlob.value = blob
  croppedAvatarPreview.value = previewUrl
}

async function saveAvatar() {
  if (!croppedAvatarBlob.value) return
  savingAvatar.value = true
  try {
    const fd = new FormData()
    const file = new File([croppedAvatarBlob.value], 'avatar.jpg', { type: 'image/jpeg' })
    fd.append('file', file)
    const r = await axios.post('/api/v1/upload/avatar', fd, {
      headers: { Authorization: `Bearer ${userStore.accessToken}` }
    })
    if (r.data?.code === 200 && r.data.data?.url) {
      await updateProfile({ avatar: r.data.data.url })
      profile.avatar = r.data.data.url
      userStore.avatar = r.data.data.url
      ElMessage.success('头像修改成功')
      showAvatarDialog.value = false
      croppedAvatarBlob.value = null
      croppedAvatarPreview.value = ''
    } else {
      ElMessage.error(r.data?.message || '上传失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '上传失败')
  } finally {
    savingAvatar.value = false
  }
}
function openPasswordDialog() { pwdForm.oldPassword='';pwdForm.newPassword='';pwdForm.confirmPassword='';showPwdDialog.value=true }
async function savePassword() {
  if (!pwdForm.oldPassword||!pwdForm.newPassword||!pwdForm.confirmPassword) return ElMessage.warning('请填写完整')
  if (pwdForm.newPassword!==pwdForm.confirmPassword) return ElMessage.warning('两次密码不一致')
  savingPwd.value=true
  try { await changePassword(pwdForm); ElMessage.success('密码修改成功'); showPwdDialog.value=false; setTimeout(()=>{userStore.logout();router.push('/auth/login')},1000) } finally { savingPwd.value=false }
}

const myClasses = ref<any[]>([])
const showJoinClassDialog=ref(false); const showCreateClassDialog=ref(false); const showMembersDialog=ref(false)
const currentClassName=ref(''); const joinClassCode=ref(''); const joiningClass=ref(false); const creatingClass=ref(false)
const classMembers=ref<any[]>([])
const managingClassId=ref<number|null>(null)
const createClassForm=reactive({className:'',school:'',grade:'',maxStudents:100,description:''})

function openJoinClassDialog(){joinClassCode.value='';showJoinClassDialog.value=true}
function openCreateClassDialog(){createClassForm.className='';createClassForm.school=profile.school||'';createClassForm.grade=profile.grade||'';createClassForm.maxStudents=100;createClassForm.description='';showCreateClassDialog.value=true}
async function handleJoinClass(){if(!joinClassCode.value.trim())return ElMessage.warning('请输入口令');joiningClass.value=true;try{await joinClass(joinClassCode.value.trim().toUpperCase());ElMessage.success('加入成功');showJoinClassDialog.value=false;await Promise.all([fetchMyClasses(),fetchStats(),refreshProfile()])}finally{joiningClass.value=false}}
async function handleLeaveClass(){try{await leaveClass();ElMessage.success('已退出');await Promise.all([fetchMyClasses(),fetchStats()])}catch{ElMessage.error('退出失败')}}
async function handleCreateClass(){if(!createClassForm.className.trim())return ElMessage.warning('请输入名称');creatingClass.value=true;try{await createClass(createClassForm);ElMessage.success('创建成功');showCreateClassDialog.value=false;await fetchMyClasses()}finally{creatingClass.value=false}}
async function fetchMyClasses(){try{const r=await getMyClasses();myClasses.value=r.data||[]}catch{myClasses.value=[]}}
async function showClassMembers(c:any){currentClassName.value=c.className;try{const r=await getClassMembers(c.id);classMembers.value=r.data||[]}catch{classMembers.value=[]};showMembersDialog.value=true}

// 教师管理班级
async function openClassManage(c: any) {
  managingClassId.value = c.id
  currentClassName.value = c.className
  try { const r = await getClassMembers(c.id); classMembers.value = r.data || [] } catch { classMembers.value = [] }
  showMembersDialog.value = true
}

async function handleRemoveStudent(row: any) {
  if (!managingClassId.value) return
  try {
    await removeStudent(managingClassId.value, row.userId)
    ElMessage.success(`已将 ${row.nickname || row.username} 移出班级`)
    // 刷新成员列表
    const r = await getClassMembers(managingClassId.value)
    classMembers.value = r.data || []
    // 刷新班级列表（更新人数）
    await fetchMyClasses()
  } catch { ElMessage.error('移除失败') }
}

async function refreshProfile(){
  try{const r=await axios.get('/api/v1/auth/me',{headers:{Authorization:`Bearer ${userStore.accessToken}`}});if(r.data.code===200){Object.assign(profile,r.data.data);Object.assign(editForm,r.data.data)}}catch{}
}

// ===== 学习成就：等级/证书/积分 =====
const pointsProfile = reactive<any>({ level: 1, experience: 0, integration: 0, levelName: '', continuousSignDays: 0 })
const levelConfig = ref<any[]>([])
const certs = ref<any[]>([])
const integralLogs = ref<any[]>([])
const levelProgress = computed(() => {
  const lv = pointsProfile.level || 1
  const cfg = levelConfig.value.find((c: any) => c.levelCode === lv)
  if (!cfg) return 0
  const min = cfg.minExperience || 0
  const max = cfg.maxExperience || (min + 1000)
  const exp = pointsProfile.experience || 0
  return Math.max(0, Math.min(100, Math.round((exp - min) / (max - min) * 100)))
})
async function loadAchievement() {
  try {
    const [pp, lc, certsRes, logs] = await Promise.all([
      getPointsProfile(), getLevelConfig(), getMyCertificates(), getIntegralLogs({ page: 1, size: 20 })
    ])
    if (pp.data) {
      Object.assign(pointsProfile, pp.data)
      const cfg = (lc.data || []).find((c: any) => c.levelCode === (pp.data.level || 1))
      pointsProfile.levelName = cfg?.levelName || ''
    }
    levelConfig.value = lc.data || []
    certs.value = certsRes.data || []
    const ld: any = logs.data
    integralLogs.value = (ld && ld.records) ? ld.records : (Array.isArray(ld) ? ld : [])
  } catch {}
}
function viewCert(sessionId: number) {
  window.open(`/api/v1/cert/by-session/${sessionId}`, '_blank')
}

onMounted(async()=>{
  await refreshProfile()
  await Promise.all([fetchStats(),fetchMyClasses(),loadAchievement()])
})
</script>

<style scoped>
.profile-page { padding: 20px 24px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; color: #303133; }

/* ======== 上排 ======== */
.top-row { display: flex; gap: 16px; margin-bottom: 16px; }
.user-card { width: 260px; flex-shrink: 0; background: #fff; border-radius: 12px; padding: 24px 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); display: flex; flex-direction: column; align-items: center; gap: 14px; }
.avatar-wrap { position: relative; }
.avatar { background: linear-gradient(135deg,#409EFF,#337ECC); font-size: 24px; font-weight: 700; cursor: pointer; }
.camera-dot { position: absolute; bottom: 0; right: 0; width: 20px; height: 20px; background: #409EFF; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 10px; border: 2px solid #fff; cursor: pointer; }
.user-name { font-size: 17px; font-weight: 600; color: #303133; }
.user-body { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.user-sub { display: flex; align-items: center; gap: 8px; }
.user-id { font-size: 12px; color: #909399; }
.user-btns { display: flex; flex-direction: column; gap: 8px; }
.user-btns :deep(.el-button) {
  width: 100%; height: 34px; padding: 0 14px; margin: 0;
  display: inline-flex; align-items: center; justify-content: center;
  gap: 4px; border-radius: 8px; font-size: 13px; line-height: 1;
  box-sizing: border-box; font-weight: 500;
}
.user-btns :deep(.el-button) .el-icon { margin-right: 0; }
.user-btns :deep(.el-button--primary) { background: #409EFF; border-color: #409EFF; color: #fff; }
.user-btns :deep(.el-button--primary:hover) { background: #66b1ff; border-color: #66b1ff; }
.user-btns :deep(.el-button--default) { background: #fff; border-color: #dcdfe6; color: #606266; }
.user-btns :deep(.el-button--default:hover) { color: #409EFF; border-color: #c6e2ff; background: #ecf5ff; }

.stats-class-group { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.stats-block, .class-block { background: #fff; border-radius: 12px; padding: 18px 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.block-label { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 14px; }
.block-label .el-icon { font-size: 17px; color: #409EFF; }
.block-actions { margin-left: auto; }
.stats-row { display: flex; gap: 0; }
.stat { flex: 1; text-align: center; padding: 6px 4px; }
.stat + .stat { border-left: 1px solid #f0f0f0; }
.stat b { display: block; font-size: 22px; font-weight: 700; color: #409EFF; line-height: 1.2; }
.stat span { display: block; font-size: 11px; color: #909399; margin-top: 4px; }

.class-items { display: flex; flex-wrap: wrap; gap: 10px; }
.class-item { display: inline-flex; align-items: center; gap: 10px; padding: 8px 14px; background: #f8f9fa; border-radius: 8px; cursor: pointer; transition: all .2s; border: 1px solid transparent; }
.class-item:hover { background: #ecf5ff; border-color: #d9ecff; }
.class-manage-btn { margin-left: auto; flex-shrink: 0; }
.cname { font-size: 14px; font-weight: 600; color: #303133; }
.cgrade { font-size: 11px; color: #409EFF; background: #ecf5ff; padding: 1px 7px; border-radius: 3px; }
.ccode { font-family: monospace; font-size: 11px; color: #409EFF; background: #fff; padding: 2px 6px; border-radius: 4px; }
.ccount { font-size: 11px; color: #909399; }
.class-empty { text-align: center; padding: 20px 0; color: #c0c4cc; font-size: 13px; }
.admin-block { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 24px 20px; color: #409EFF; font-weight: 500; font-size: 14px; }

/* ======== 下排 ======== */
.bottom-row { }
.info-card { background: #fff; border-radius: 12px; padding: 20px 24px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.info-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.info-item { background: #f8f9fa; border-radius: 8px; padding: 10px 14px; display: flex; flex-direction: column; gap: 3px; }
.info-item label { font-size: 11px; color: #909399; }
.info-item span { font-size: 13px; color: #303133; font-weight: 500; }
.info-item span.sig { color: #606266; font-style: italic; }

.edit-form { padding-top: 4px; }

.avatar-upload-box { width:100px;height:100px;border:2px dashed #dcdfe6;border-radius:10px;margin:0 auto;display:flex;align-items:center;justify-content:center;overflow:hidden;cursor:pointer;transition:all .3s; }
.avatar-upload-box:hover{border-color:#409EFF;background:#f5f7fa;}
.avatar-preview-img{width:100%;height:100%;object-fit:cover;}
.upload-placeholder{text-align:center;color:#c0c4cc;}
.upload-placeholder span{display:block;font-size:12px;margin-top:4px;}

/* ===== 学习成就 ===== */
.achievement-row { display: grid; grid-template-columns: 1fr 1fr 1.2fr; gap: 16px; margin-top: 16px; }
.ach-card { background: #fff; border-radius: 12px; padding: 18px 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.ach-card .block-label { margin-bottom: 14px; }
.level-row { display: flex; align-items: center; gap: 14px; margin-bottom: 14px; }
.exp-info { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: #606266; }
.exp-info b { color: #e6a23c; font-size: 15px; margin-left: 4px; }
.cert-list { display: flex; flex-direction: column; gap: 10px; }
.cert-item { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; background: #f8f9fa; border-radius: 8px; }
.cert-info { display: flex; flex-direction: column; gap: 2px; }
.cert-no { font-size: 13px; color: #303133; font-weight: 500; }
.cert-time { font-size: 11px; color: #909399; }
.pos { color: #67c23a; font-weight: 600; }
.neg { color: #f56c6c; font-weight: 600; }
</style>
