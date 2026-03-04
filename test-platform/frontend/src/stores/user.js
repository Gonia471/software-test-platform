import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')

  function setAuth(t, name) {
    token.value = t
    username.value = name || ''
    if (t) {
      localStorage.setItem('token', t)
      localStorage.setItem('username', name || '')
    } else {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
    }
  }

  function logout() {
    setAuth('', '')
  }

  return { token, username, setAuth, logout }
})
