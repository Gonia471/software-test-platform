<template>
  <div class="login-page">
    <button type="button" class="dev-enter" @click="quickEnter">直接进入主界面（开发用）</button>
    <div class="auth-shell">
      <section class="auth-intro">
        <span class="auth-badge">EasyTest</span>
        <h1 class="auth-intro__title">软件测试平台</h1>
        <p class="auth-intro__desc">
          面向企业研发与测试团队的统一测试工作台，支持组织协作、项目管理、API 测试、UI 自动化与报告查看。
        </p>
        <div class="auth-feature-list">
          <div class="auth-feature">
            <strong>统一入口</strong>
            <span>一个平台完成项目、接口、UI 与报告管理。</span>
          </div>
          <div class="auth-feature">
            <strong>简洁界面</strong>
            <span>全站采用清晰统一的蓝灰卡片风格，便于日常使用与团队协作。</span>
          </div>
          <div class="auth-feature">
            <strong>协作测试</strong>
            <span>支持组织与项目维度的成员协作和测试资源管理。</span>
          </div>
        </div>
      </section>

      <section class="login-card">
        <div class="login-header">
          <div class="login-logo">
            <span class="login-logo-icon">🧪</span>
          </div>
          <h1 class="login-title">欢迎回来</h1>
          <p class="login-subtitle">登录到 EasyTest 软件测试平台</p>
        </div>
        <div class="form">
          <label class="field-label">手机号</label>
          <input
            v-model="phone"
            type="tel"
            placeholder="请输入手机号"
            maxlength="11"
            inputmode="numeric"
            @input="phone = sanitizePhoneInput(phone)"
          />
          <p v-if="error" class="error">{{ error }}</p>
          <button type="button" class="submit-btn" :disabled="loading" @click="handleLogin">
            {{ loading ? '登录中...' : '登录' }}
          </button>
          <div v-if="pendingInvitations.length" class="invitation-panel">
            <strong>检测到企业邀请</strong>
            <div
              v-for="invitation in pendingInvitations"
              :key="invitation.invitationId"
              class="invitation-item"
            >
              <div>
                <div class="invitation-title">{{ invitation.enterpriseSpaceName }}</div>
                <div class="invitation-desc">
                  {{ invitation.organizationName ? `拟加入组织：${invitation.organizationName}` : '暂未指定组织' }}
                </div>
                <div class="invitation-desc">邀请人：{{ invitation.inviterName }}</div>
              </div>
              <button
                type="button"
                class="join-btn"
                :disabled="joiningInvitationId === invitation.invitationId"
                @click="handleAcceptInvitation(invitation)"
              >
                {{ joiningInvitationId === invitation.invitationId ? '加入中...' : '加入企业空间' }}
              </button>
            </div>
          </div>
          <p class="tip">
            没有收到企业邀请？<router-link to="/register">点击这里，开通企业空间</router-link>
          </p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { loginWithCode } from '../api/auth'
import { acceptInvitation } from '../api/invitation'
import { isValidPhone, sanitizePhoneInput } from '../utils/phone'

const router = useRouter()
const userStore = useUserStore()

const phone = ref('')
const error = ref('')
const loading = ref(false)
const pendingInvitations = ref([])
const joiningInvitationId = ref(null)

async function handleLogin() {
  phone.value = sanitizePhoneInput(phone.value)
  if (!phone.value) {
    error.value = '请输入手机号'
    return
  }
  if (!isValidPhone(phone.value)) {
    error.value = '请输入11位手机号'
    return
  }
  error.value = ''
  loading.value = true
  try {
    const { data } = await loginWithCode(phone.value)
    userStore.setAuth(
      data.token,
      data.username,
      data.phone,
      data.userId,
      data.isDevMode || false,
      {
        hasEnterpriseSpace: data.hasEnterpriseSpace,
        enterpriseSpaceId: data.enterpriseSpaceId,
        enterpriseSpaceName: data.enterpriseSpaceName,
      }
    )
    pendingInvitations.value = data.pendingInvitations || []

    if (pendingInvitations.value.length > 0) {
      error.value = ''
      return
    }

    if (!data.hasEnterpriseSpace) {
      error.value = '当前手机号尚未加入企业空间，请联系管理员邀请，或先开通企业空间'
      return
    }

    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleAcceptInvitation(invitation) {
  joiningInvitationId.value = invitation.invitationId
  error.value = ''
  try {
    await acceptInvitation(invitation.invitationId)
    userStore.setAuth(
      userStore.token,
      userStore.username,
      userStore.phone,
      userStore.userId,
      userStore.isDevMode,
      {
        hasEnterpriseSpace: true,
        enterpriseSpaceId: invitation.enterpriseSpaceId,
        enterpriseSpaceName: invitation.enterpriseSpaceName,
      }
    )
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '加入失败'
  } finally {
    joiningInvitationId.value = null
  }
}

function quickEnter() {
  userStore.setAuth('dev-token', '开发模式用户', '', 1, true, {
    hasEnterpriseSpace: false,
    enterpriseSpaceId: null,
    enterpriseSpaceName: '',
  })
  router.push('/')
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.18) 0, rgba(59, 130, 246, 0) 32%),
    radial-gradient(circle at bottom right, rgba(20, 184, 166, 0.16) 0, rgba(20, 184, 166, 0) 30%),
    linear-gradient(135deg, #eff6ff 0%, #f8fbff 48%, #eef8f7 100%);
  background-attachment: fixed;
}

.login-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.36) 0%, rgba(255, 255, 255, 0.12) 100%);
  pointer-events: none;
}

.dev-enter {
  position: absolute;
  top: 20px;
  right: 20px;
  font-size: 12px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: rgba(255, 255, 255, 0.82);
  color: var(--text-primary);
  cursor: pointer;
  backdrop-filter: blur(14px);
  z-index: 2;
}

.dev-enter:hover {
  background: #ffffff;
}

.auth-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(320px, 1.1fr) minmax(340px, 420px);
  width: min(1100px, 100%);
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.72);
}

.auth-intro {
  padding: 48px;
  background: linear-gradient(160deg, #0f172a 0%, #172554 42%, #0f766e 100%);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-badge {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 18px;
}

.auth-intro__title {
  font-size: 38px;
  line-height: 1.15;
  margin-bottom: 14px;
}

.auth-intro__desc {
  font-size: 15px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.82);
  margin-bottom: 28px;
}

.auth-feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.auth-feature {
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.auth-feature strong {
  display: block;
  margin-bottom: 6px;
  font-size: 15px;
}

.auth-feature span {
  color: rgba(255, 255, 255, 0.76);
  font-size: 13px;
  line-height: 1.7;
}

.login-card {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  width: 64px;
  height: 64px;
  background: var(--primary-gradient);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 14px 32px rgba(59, 130, 246, 0.28);
}

.login-logo-icon {
  font-size: 32px;
  color: #fff;
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.login-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 600;
}

.form input {
  padding: 14px 16px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 14px;
  font-size: 14px;
  background: #f8fbff;
  transition: var(--transition);
}

.form input:focus {
  outline: none;
  border-color: var(--primary-color);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.08);
}

.form input::placeholder {
  color: #9ca3af;
}

.submit-btn {
  padding: 14px;
  background: var(--primary-gradient);
  color: #fff;
  border: none;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  box-shadow: 0 14px 26px rgba(59, 130, 246, 0.24);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: #ef4444;
  font-size: 13px;
  text-align: center;
  padding: 8px;
  background: #fef2f2;
  border-radius: 8px;
}

.tip {
  font-size: 13px;
  color: var(--text-secondary);
  text-align: center;
  margin-top: 8px;
}

.tip a {
  color: var(--primary-color);
  font-weight: 600;
  text-decoration: none;
}

.invitation-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.invitation-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.invitation-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.invitation-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.join-btn {
  padding: 10px 14px;
  border: none;
  border-radius: 10px;
  background: var(--primary-gradient);
  color: #fff;
  cursor: pointer;
  white-space: nowrap;
}

@media (max-width: 920px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-intro {
    display: none;
  }

  .login-card {
    padding: 40px 28px;
  }
}
</style>
