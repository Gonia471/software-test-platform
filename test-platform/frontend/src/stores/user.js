import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const phone = ref(localStorage.getItem('phone') || '')
  const userId = ref(localStorage.getItem('userId') || null)
  const isDevMode = ref(localStorage.getItem('isDevMode') === 'true')

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(t, name, p, id, devMode = false) {
    token.value = t
    username.value = name || ''
    phone.value = p || ''
    userId.value = id || null
    isDevMode.value = devMode

    if (t) {
      localStorage.setItem('token', t)
      localStorage.setItem('username', name || '')
      localStorage.setItem('phone', p || '')
      localStorage.setItem('userId', id || '')
      localStorage.setItem('isDevMode', devMode ? 'true' : 'false')
    } else {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('phone')
      localStorage.removeItem('userId')
      localStorage.removeItem('isDevMode')
    }
  }

  function logout() {
    setAuth('', '', '', null, false)
  }

  return { token, username, phone, userId, isDevMode, isLoggedIn, setAuth, logout }
})