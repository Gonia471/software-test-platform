<template>
  <div class="register-page">
    <div class="auth-shell">
      <section class="auth-intro">
        <span class="auth-badge">Create Workspace</span>
        <h1 class="auth-intro__title">开通企业空间</h1>
        <p class="auth-intro__desc">
          一次创建即可拥有组织、项目、API 测试、UI 自动化与测试报告等完整能力，适合团队协作与系统演示。
        </p>
        <div class="auth-feature-list">
          <div class="auth-feature">
            <strong>企业空间即刻启用</strong>
            <span>注册后自动完成初始组织创建，直接进入统一测试平台。</span>
          </div>
          <div class="auth-feature">
            <strong>模块统一风格</strong>
            <span>进入系统后，各模块保持统一视觉与交互风格，便于团队长期使用。</span>
          </div>
          <div class="auth-feature">
            <strong>后续可继续邀请成员</strong>
            <span>创建后可在组织管理中生成邀请码并扩展成员协作。</span>
          </div>
        </div>
      </section>

      <section class="register-card">
        <div class="register-header">
          <div class="register-logo">
            <span class="register-logo-icon">🏢</span>
          </div>
          <h1 class="register-title">开通企业空间</h1>
          <p class="register-subtitle">创建您的测试团队，开始协作</p>
        </div>
        <div class="form">
          <label class="field-label">手机号</label>
          <input
            v-model="phone"
            type="tel"
            placeholder="请输入手机号"
            maxlength="11"
          />
          <label class="field-label">企业空间名称</label>
          <input
            v-model="orgName"
            type="text"
            placeholder="企业空间名称"
          />
          <label class="field-label">空间描述</label>
          <input
            v-model="description"
            type="text"
            placeholder="企业空间描述（选填）"
          />
          <p v-if="error" class="error">{{ error }}</p>
          <button type="button" class="submit-btn" :disabled="loading" @click="handleRegister">
            {{ loading ? '开通中...' : '立即开通' }}
          </button>
          <p class="tip">
            已有账号？<router-link to="/login">返回登录</router-link>
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
import { registerWithOrg } from '../api/auth'

const router = useRouter()
const userStore = useUserStore()

const phone = ref('')
const orgName = ref('')
const description = ref('')
const error = ref('')
const loading = ref(false)

async function handleRegister() {
  if (!phone.value || !orgName.value) {
    error.value = '请填写完整信息'
    return
  }
  error.value = ''
  loading.value = true
  try {
    const { data } = await registerWithOrg(phone.value, orgName.value, description.value)
    userStore.setAuth(data.token, data.username, data.phone, data.userId)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '开通失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
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

.register-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.36) 0%, rgba(255, 255, 255, 0.12) 100%);
  pointer-events: none;
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

.register-card {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-logo {
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

.register-logo-icon {
  font-size: 32px;
}

.register-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.register-subtitle {
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
  transition: var(--transition);
  background: #f8fbff;
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
  transform: none;
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

@media (max-width: 920px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-intro {
    display: none;
  }

  .register-card {
    padding: 40px 28px;
  }
}
</style>
