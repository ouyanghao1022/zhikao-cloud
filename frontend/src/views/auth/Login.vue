<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <!-- 品牌 -->
        <div class="card-brand">
          <span class="brand-icon">📚</span>
          <h1>智考云</h1>
          <p>在线考试与学习平台</p>
        </div>

        <!-- 登录表单 -->
        <template v-if="!isRegister">
          <div class="card-form">
            <div class="input-group">
              <span class="input-prefix">👤</span>
              <input v-model="loginForm.account" placeholder="用户名 / 邮箱 / 手机号" />
            </div>
            <div class="input-group">
              <span class="input-prefix">🔒</span>
              <input v-model="loginForm.password" :type="showPwd ? 'text' : 'password'" placeholder="密码" @keyup.enter="handleLogin" />
              <span class="input-suffix" @click="showPwd = !showPwd">{{ showPwd ? '🙈' : '👁' }}</span>
            </div>
            <div class="captcha-row">
              <div class="input-group" style="flex:1">
                <span class="input-prefix">🔐</span>
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
                <span title="手机登录" @click="showSmsLogin = true">📱</span>
                <span title="微信登录" @click="handleOAuth('wechat')">💚</span>
                <span title="QQ登录" @click="handleOAuth('qq')">💙</span>
                <span title="钉钉登录" @click="handleOAuth('dingtalk')">💜</span>
              </div>
            </div>
          </div>
        </template>

        <!-- 注册表单 -->
        <template v-else>
          <div class="card-form">
            <div class="input-group">
              <span class="input-prefix">👤</span>
              <input v-model="registerForm.username" placeholder="用户名（3-50位）" />
            </div>
            <div class="input-group">
              <span class="input-prefix">😊</span>
              <input v-model="registerForm.nickname" placeholder="昵称" />
            </div>
            <div class="input-group">
              <span class="input-prefix">📧</span>
              <input v-model="registerForm.email" placeholder="邮箱（选填）" />
            </div>
            <div class="input-group">
              <span class="input-prefix">📱</span>
              <input v-model="registerForm.phone" placeholder="手机号（选填）" />
            </div>
            <div class="input-group">
              <span class="input-prefix">🔒</span>
              <input v-model="registerForm.password" :type="showRegPwd ? 'text' : 'password'" placeholder="密码（6-20位）" />
              <span class="input-suffix" @click="showRegPwd = !showRegPwd">{{ showRegPwd ? '🙈' : '👁' }}</span>
            </div>
            <div class="input-group">
              <span class="input-prefix">🔒</span>
              <input v-model="registerForm.confirmPassword" :type="showRegPwd ? 'text' : 'password'" placeholder="确认密码" />
            </div>
            <div class="captcha-row">
              <div class="input-group" style="flex:1">
                <span class="input-prefix">🔐</span>
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
          <span class="input-prefix">📱</span>
          <input v-model="smsForm.phone" placeholder="手机号" maxlength="11" />
        </div>
        <div class="captcha-row" style="margin-top:14px">
          <div class="input-group" style="flex:1">
            <span class="input-prefix">🔐</span>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

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
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 45%, #0f3460 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  padding: 24px;
}

.login-container { width: 100%; max-width: 400px; }

.login-card {
  background: #fff; border-radius: 16px; padding: 40px 36px 32px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.45);
}

/* 品牌 */
.card-brand { text-align: center; margin-bottom: 32px; }
.card-brand .brand-icon { font-size: 48px; display: block; margin-bottom: 8px; }
.card-brand h1 { font-size: 28px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; letter-spacing: 4px; }
.card-brand p { font-size: 13px; color: #909399; margin: 0; }

/* 表单 */
.card-form { display: flex; flex-direction: column; gap: 16px; }

.input-group {
  display: flex; align-items: center; border: 1.5px solid #e4e7ed; border-radius: 8px;
  overflow: hidden; transition: border-color .25s; background: #fafbfc;
}
.input-group:focus-within { border-color: #409EFF; background: #fff; box-shadow: 0 0 0 3px rgba(64,158,255,.08); }
.input-prefix { padding: 0 12px; font-size: 15px; flex-shrink: 0; opacity: .7; }
.input-group input {
  flex: 1; border: none; outline: none; padding: 11px 8px 11px 0; font-size: 14px;
  color: #303133; background: transparent; min-width: 0;
}
.input-group input::placeholder { color: #c0c4cc; }
.input-suffix { padding: 0 14px; cursor: pointer; font-size: 15px; flex-shrink: 0; user-select: none; }

/* 验证码 */
.captcha-row { display: flex; gap: 12px; }
.captcha-img {
  width: 100px; height: 44px; border-radius: 8px; flex-shrink: 0; cursor: pointer;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; color: #fff; letter-spacing: 4px;
  font-family: 'Courier New', monospace; user-select: none;
}

/* 记住密码 */
.remember-row { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #909399; cursor: pointer; }
.remember-row input[type="checkbox"] { accent-color: #409EFF; }

/* 按钮 */
.btn-primary {
  width: 100%; height: 44px; border: none; border-radius: 8px; cursor: pointer;
  background: linear-gradient(135deg, #409EFF, #337ecc);
  color: #fff; font-size: 15px; font-weight: 600; letter-spacing: 4px;
  transition: all .25s;
}
.btn-primary:hover:not(:disabled) { box-shadow: 0 6px 20px rgba(64,158,255,.4); transform: translateY(-1px); }
.btn-primary:disabled { opacity: .6; cursor: not-allowed; }

/* 其他登录 */
.other-login { margin-top: 4px; }
.divider { text-align: center; border-top: 1px solid #eee; margin: 8px 0 14px; }
.divider span { background: #fff; padding: 0 12px; color: #c0c4cc; font-size: 12px; position: relative; top: -9px; }
.other-icons { display: flex; justify-content: center; gap: 16px; }
.other-icons span { font-size: 22px; cursor: pointer; opacity: .6; transition: all .2s; }
.other-icons span:hover { opacity: 1; transform: scale(1.15); }

/* 短信按钮 */
.sms-btn {
  width: 110px; height: 44px; flex-shrink: 0; border: none; border-radius: 8px;
  background: #409EFF; color: #fff; font-size: 13px; cursor: pointer; white-space: nowrap;
}
.sms-btn:disabled { opacity: .5; cursor: not-allowed; }

/* 底部 */
.card-footer { text-align: center; margin-top: 24px; font-size: 13px; color: #909399; }
.card-footer a { color: #409EFF; cursor: pointer; font-weight: 500; }
.card-footer a:hover { text-decoration: underline; }

@media (max-width: 440px) {
  .login-page { padding: 16px; }
  .login-card { padding: 32px 24px; }
}
</style>
