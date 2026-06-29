<template>
  <div class="login-page">
    <!-- 左侧装饰区 -->
    <div class="deco-panel">
      <div class="deco-content">
        <div class="deco-seal">智考</div>
        <h1 class="deco-title">智考云</h1>
        <p class="deco-sub">在线考试与学习平台</p>
        <div class="deco-lines">
          <span></span><span></span><span></span>
        </div>
        <p class="deco-quote">学而不思则罔，思而不学则殆</p>
      </div>
      <!-- 装饰角纹 -->
      <div class="corner-pattern top-left"></div>
      <div class="corner-pattern bottom-right"></div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-panel">
      <div class="form-box">
        <div class="card-brand">
          <div class="brand-seal-sm">智</div>
          <h1>智考云</h1>
          <p>{{ isRegister ? '创建新账号' : '欢迎登录' }}</p>
        </div>

        <!-- 登录表单 -->
        <template v-if="!isRegister">
          <div class="card-form">
            <div class="input-group">
              <el-icon class="input-prefix"><User /></el-icon>
              <input v-model="loginForm.account" placeholder="用户名 / 邮箱 / 手机号" />
            </div>
            <div class="input-group">
              <el-icon class="input-prefix"><Lock /></el-icon>
              <input v-model="loginForm.password" :type="showPwd ? 'text' : 'password'" placeholder="密码" @keyup.enter="handleLogin" />
              <el-icon class="input-suffix" @click="showPwd = !showPwd"><View v-if="showPwd" /><Hide v-else /></el-icon>
            </div>
            <div class="captcha-row">
              <div class="input-group" style="flex:1">
                <el-icon class="input-prefix"><Key /></el-icon>
                <input v-model="loginForm.captcha" placeholder="验证码" maxlength="4" @keyup.enter="handleLogin" />
              </div>
              <div class="captcha-img" @click="refreshCaptcha">{{ captchaCode }}</div>
            </div>
            <label class="remember-row">
              <input type="checkbox" v-model="rememberMe" />
              <span>记住密码</span>
            </label>
            <button class="btn-primary" :disabled="loading" @click="handleLogin">
              {{ loading ? '登录中...' : '登 录' }}
            </button>

            <div class="other-login">
              <div class="divider"><span>其他方式</span></div>
              <div class="other-icons">
                <span title="手机登录" @click="showSmsLogin = true"><el-icon :size="20"><Iphone /></el-icon></span>
                <span title="微信登录" @click="handleOAuth('wechat')"><el-icon :size="20"><ChatDotRound /></el-icon></span>
              </div>
            </div>
          </div>
        </template>

        <!-- 注册表单 -->
        <template v-else>
          <div class="card-form">
            <div class="input-group">
              <el-icon class="input-prefix"><User /></el-icon>
              <input v-model="registerForm.username" placeholder="用户名（3-50位）" />
            </div>
            <div class="input-group">
              <el-icon class="input-prefix"><UserFilled /></el-icon>
              <input v-model="registerForm.nickname" placeholder="昵称" />
            </div>
            <div class="input-group">
              <el-icon class="input-prefix"><Message /></el-icon>
              <input v-model="registerForm.email" placeholder="邮箱（选填）" />
            </div>
            <div class="input-group">
              <el-icon class="input-prefix"><Iphone /></el-icon>
              <input v-model="registerForm.phone" placeholder="手机号（选填）" />
            </div>
            <div class="input-group">
              <el-icon class="input-prefix"><Lock /></el-icon>
              <input v-model="registerForm.password" :type="showRegPwd ? 'text' : 'password'" placeholder="密码（6-20位）" />
              <el-icon class="input-suffix" @click="showRegPwd = !showRegPwd"><View v-if="showRegPwd" /><Hide v-else /></el-icon>
            </div>
            <div class="input-group">
              <el-icon class="input-prefix"><Lock /></el-icon>
              <input v-model="registerForm.confirmPassword" :type="showRegPwd ? 'text' : 'password'" placeholder="确认密码" />
            </div>
            <div class="captcha-row">
              <div class="input-group" style="flex:1">
                <el-icon class="input-prefix"><Key /></el-icon>
                <input v-model="registerForm.captcha" placeholder="验证码" maxlength="4" />
              </div>
              <div class="captcha-img" @click="refreshCaptcha">{{ captchaCode }}</div>
            </div>
            <button class="btn-primary" :disabled="loading" @click="handleRegister">
              {{ loading ? '注册中...' : '注 册' }}
            </button>
          </div>
        </template>

        <!-- 底部切换 -->
        <div class="card-footer">
          <template v-if="!isRegister">
            还没有账号？<a @click="isRegister = true">立即注册</a>
          </template>
          <template v-else>
            已有账号？<a @click="isRegister = false">返回登录</a>
          </template>
        </div>
      </div>
    </div>

    <!-- SMS弹窗 -->
    <el-dialog v-model="showSmsLogin" title="手机验证码登录" width="360px" :close-on-click-modal="false" center>
      <div style="padding:4px 0">
        <div class="input-group">
          <el-icon class="input-prefix"><Iphone /></el-icon>
          <input v-model="smsForm.phone" placeholder="手机号" maxlength="11" />
        </div>
        <div class="captcha-row" style="margin-top:14px">
          <div class="input-group" style="flex:1">
            <el-icon class="input-prefix"><Key /></el-icon>
            <input v-model="smsForm.code" placeholder="验证码" maxlength="6" />
          </div>
          <button class="sms-btn" :disabled="smsCountdown > 0" @click="sendSmsCode">
            {{ smsCountdown > 0 ? smsCountdown + 's' : '获取验证码' }}
          </button>
        </div>
      </div>
      <template #footer>
        <el-button @click="showSmsLogin = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSmsLogin">登录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock, View, Hide, Key, Iphone, ChatDotRound, UserFilled, Message } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const isRegister = ref(false)
const loading = ref(false)
const showPwd = ref(false)
const showRegPwd = ref(false)
const rememberMe = ref(false)

const loginForm = ref({ account: '', password: '', captcha: '' })
const registerForm = ref({ username: '', password: '', confirmPassword: '', email: '', phone: '', nickname: '', captcha: '' })

const showSmsLogin = ref(false)
const smsForm = ref({ phone: '', code: '' })
const smsCountdown = ref(0)
let smsTimer: any = null

function handleOAuth(p: string) { ElMessage.info(p + ' 登录开发中') }

async function sendSmsCode() {
  if (!/^1[3-9]\d{9}$/.test(smsForm.value.phone)) { ElMessage.warning('手机号不正确'); return }
  try {
    const r = await fetch('/api/v1/sms/send', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ phone: smsForm.value.phone }) }).then(r => r.json())
    if (r.code === 200) { ElMessage.success('验证码已发送'); smsCountdown.value = 60; smsTimer = setInterval(() => { smsCountdown.value--; if (smsCountdown.value <= 0) clearInterval(smsTimer) }, 1000) }
    else ElMessage.error(r.message)
  } catch { ElMessage.error('发送失败') }
}

async function handleSmsLogin() {
  if (!/^1[3-9]\d{9}$/.test(smsForm.value.phone)) { ElMessage.warning('手机号不正确'); return }
  if (smsForm.value.code.length !== 6) { ElMessage.warning('验证码6位'); return }
  loading.value = true
  try {
    const v = await fetch('/api/v1/sms/verify', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ phone: smsForm.value.phone, code: smsForm.value.code }) }).then(r => r.json())
    if (v.code !== 200) { ElMessage.error(v.message); return }
    const ok = await userStore.handleLogin(smsForm.value.phone, smsForm.value.phone + '@123')
    if (ok) router.push('/dashboard')
  } catch { ElMessage.error('登录失败') } finally { loading.value = false }
}

const captchaCode = ref(genCaptcha())
function genCaptcha() { const c = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; let s = ''; for (let i = 0; i < 4; i++) s += c[Math.floor(Math.random() * c.length)]; return s }
function refreshCaptcha() { captchaCode.value = genCaptcha() }

function validateLogin() {
  if (!loginForm.value.account.trim()) { ElMessage.warning('请输入账号'); return false }
  if (!loginForm.value.password) { ElMessage.warning('请输入密码'); return false }
  if (!loginForm.value.captcha || loginForm.value.captcha.toUpperCase() !== captchaCode.value.toUpperCase()) { ElMessage.warning('验证码错误'); refreshCaptcha(); loginForm.value.captcha = ''; return false }
  return true
}

function validateRegister() {
  const f = registerForm.value
  if (!f.username.trim() || f.username.trim().length < 3) { ElMessage.warning('用户名至少3位'); return false }
  if (!f.nickname.trim()) { ElMessage.warning('请输入昵称'); return false }
  if (!f.password || f.password.length < 6) { ElMessage.warning('密码至少6位'); return false }
  if (f.password !== f.confirmPassword) { ElMessage.warning('两次密码不一致'); return false }
  if (!f.captcha || f.captcha.toUpperCase() !== captchaCode.value.toUpperCase()) { ElMessage.warning('验证码错误'); refreshCaptcha(); f.captcha = ''; return false }
  return true
}

async function handleLogin() {
  if (!validateLogin()) return
  loading.value = true
  try { const ok = await userStore.handleLogin(loginForm.value.account, loginForm.value.password); if (ok) router.push('/dashboard') } finally { loading.value = false }
}

async function handleRegister() {
  if (!validateRegister()) return
  loading.value = true
  try {
    const ok = await userStore.handleRegister({ ...registerForm.value })
    if (ok) { ElMessage.success('注册成功，请登录'); isRegister.value = false; registerForm.value = { username: '', password: '', confirmPassword: '', email: '', phone: '', nickname: '', captcha: '' }; refreshCaptcha() }
  } finally { loading.value = false }
}

onUnmounted(() => { if (smsTimer) clearInterval(smsTimer) })
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex;
  font-family: 'Noto Serif SC', 'Source Han Serif CN', 'SimSun', 'Songti SC',
    'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ===== 左侧装饰区 ===== */
.deco-panel {
  flex: 1; background: var(--color-xuan-blue);
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.deco-content { text-align: center; position: relative; z-index: 2; }
.deco-seal {
  width: 72px; height: 72px; border-radius: var(--radius-sm); margin: 0 auto 24px;
  border: 2.5px solid rgba(255,255,255,0.25);
  display: flex; align-items: center; justify-content: center;
  font-size: 32px; font-weight: 900; color: rgba(255,255,255,0.8);
  letter-spacing: 4px;
}
.deco-title {
  font-size: 36px; font-weight: 800; color: #f1f5f9; margin: 0 0 6px;
  letter-spacing: 8px;
}
.deco-sub { font-size: 14px; color: rgba(255,255,255,0.5); margin: 0 0 32px; letter-spacing: 3px; }
.deco-lines { display: flex; gap: 6px; justify-content: center; margin-bottom: 28px; }
.deco-lines span { width: 24px; height: 2px; background: var(--color-primary); border-radius: 1px; }
.deco-quote {
  font-size: 14px; color: rgba(255,255,255,0.35); letter-spacing: 3px; font-style: italic;
}

/* 角纹装饰 */
.corner-pattern {
  position: absolute; width: 120px; height: 120px;
  border: 1.5px solid rgba(255,255,255,0.08);
}
.corner-pattern.top-left { top: 32px; left: 32px; border-right: none; border-bottom: none; }
.corner-pattern.bottom-right { bottom: 32px; right: 32px; border-left: none; border-top: none; }

/* ===== 右侧表单区 ===== */
.form-panel {
  flex: 1; display: flex; align-items: center; justify-content: center;
  background: var(--color-rice); padding: 40px;
}
.form-box { width: 100%; max-width: 380px; }

.card-brand { text-align: center; margin-bottom: 36px; }
.brand-seal-sm {
  width: 42px; height: 42px; border-radius: var(--radius-sm); margin: 0 auto 12px;
  background: var(--color-primary); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 800;
  box-shadow: 0 3px 12px rgba(5,150,105,0.25);
}
.card-brand h1 { font-size: 24px; font-weight: 700; color: var(--color-ink); margin: 0 0 4px; letter-spacing: 4px; }
.card-brand p { font-size: 13px; color: var(--color-ink-muted); margin: 0; }

/* 表单 */
.card-form { display: flex; flex-direction: column; gap: 16px; }

.input-group {
  display: flex; align-items: center; border: 1.5px solid var(--color-rice-border); border-radius: var(--radius-md);
  overflow: hidden; transition: border-color .25s; background: var(--color-rice-card);
}
.input-group:focus-within { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(5,150,105,0.06); }
.input-prefix { padding: 0 12px; font-size: 16px; flex-shrink: 0; color: var(--color-ink-muted); }
.input-group input {
  flex: 1; border: none; outline: none; padding: 12px 8px 12px 0; font-size: 14px;
  color: var(--color-ink); background: transparent; min-width: 0;
}
.input-group input::placeholder { color: #c0b8a8; }
.input-suffix { padding: 0 14px; cursor: pointer; font-size: 15px; flex-shrink: 0; color: var(--color-ink-muted); }

/* 验证码 */
.captcha-row { display: flex; gap: 12px; }
.captcha-img {
  width: 100px; height: 46px; border-radius: var(--radius-md); flex-shrink: 0; cursor: pointer;
  background: var(--color-xuan-blue);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; color: var(--color-primary-light); letter-spacing: 4px;
  font-family: 'Courier New', monospace; user-select: none;
}

/* 记住密码 */
.remember-row { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--color-ink-muted); cursor: pointer; }
.remember-row input[type="checkbox"] { accent-color: var(--color-primary); }

/* 按钮 */
.btn-primary {
  width: 100%; height: 46px; border: none; border-radius: var(--radius-md); cursor: pointer;
  background: var(--color-primary); color: #fff; font-size: 15px; font-weight: 600;
  letter-spacing: 6px; transition: all .25s;
}
.btn-primary:hover:not(:disabled) { background: var(--color-primary-dark); box-shadow: 0 6px 20px rgba(5,150,105,0.25); }
.btn-primary:disabled { opacity: .55; cursor: not-allowed; }

/* 其他登录 */
.other-login { margin-top: 4px; }
.divider { text-align: center; border-top: 1px solid var(--color-rice-border); margin: 8px 0 14px; }
.divider span { background: var(--color-rice); padding: 0 12px; color: var(--color-ink-muted); font-size: 12px; position: relative; top: -9px; }
.other-icons { display: flex; justify-content: center; gap: 20px; }
.other-icons span {
  width: 40px; height: 40px; border-radius: 50%; border: 1.5px solid var(--color-rice-border);
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  color: var(--color-ink-muted); transition: all .2s;
}
.other-icons span:hover { border-color: var(--color-primary); color: var(--color-primary); }

/* 短信按钮 */
.sms-btn {
  width: 110px; height: 46px; flex-shrink: 0; border: none; border-radius: var(--radius-md);
  background: var(--color-primary); color: #fff; font-size: 13px; cursor: pointer; white-space: nowrap;
}
.sms-btn:disabled { opacity: .5; cursor: not-allowed; }

/* 底部 */
.card-footer { text-align: center; margin-top: 28px; font-size: 13px; color: var(--color-ink-muted); }
.card-footer a { color: var(--color-primary); cursor: pointer; font-weight: 500; }
.card-footer a:hover { text-decoration: underline; }

@media (max-width: 768px) {
  .login-page { flex-direction: column; }
  .deco-panel { min-height: 200px; padding: 40px 20px; }
  .form-panel { padding: 32px 20px; }
  .deco-title { font-size: 28px; }
  .corner-pattern { display: none; }
}
</style>
