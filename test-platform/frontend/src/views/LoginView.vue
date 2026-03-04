<template>
  <div class="login-page">
    <button type="button" class="dev-enter" @click="quickEnter">
      直接进入主界面（开发用）
    </button>
    <h1>软件测试系统</h1>
    <form class="form" @submit.prevent="onSubmit">
      <input
        v-model="username"
        type="text"
        placeholder="用户名"
        required
        autocomplete="username"
      />
      <input
        v-model="password"
        type="password"
        placeholder="密码"
        required
        autocomplete="current-password"
      />
      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" :disabled="loading">登录</button>
    </form>
    <p class="tip">
      没有账号？ <router-link to="/register">注册</router-link>
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { login } from '../api/auth'

const router = useRouter()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function onSubmit() {
  error.value = ''
  loading.value = true
  try {
    const { data } = await login(username.value, password.value)
    userStore.setAuth(data.token, data.username)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '登录失败'
  } finally {
    loading.value = false
  }
}

function quickEnter() {
  userStore.setAuth('dev-token', '开发模式用户')
  router.push('/')
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}

.dev-enter {
  position: absolute;
  top: 16px;
  right: 16px;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid #d1d5db;
  background-color: #f9fafb;
  color: #4b5563;
  cursor: pointer;
}

.dev-enter:hover {
  background-color: #e5e7eb;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  width: 240px;
}
.form input {
  padding: 0.5rem 0.75rem;
  border: 1px solid #ccc;
  border-radius: 6px;
}
.form button {
  padding: 0.5rem;
  background: #1e293b;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.form button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.error { color: #dc2626; font-size: 14px; }
.tip { font-size: 14px; color: #64748b; }
.tip a { color: #42b983; }
</style>
