<template>
  <div class="register-page">
    <h1>注册</h1>
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
        autocomplete="new-password"
      />
      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" :disabled="loading">注册</button>
    </form>
    <p class="tip">
      已有账号？ <router-link to="/login">登录</router-link>
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { register } from '../api/auth'

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
    const { data } = await register(username.value, password.value)
    userStore.setAuth(data.token, data.username)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
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
