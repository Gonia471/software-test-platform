import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const phone = ref(localStorage.getItem('phone') || '')
  const userId = ref(localStorage.getItem('userId') || null)
  const isDevMode = ref(localStorage.getItem('isDevMode') === 'true')
  const hasEnterpriseSpace = ref(localStorage.getItem('hasEnterpriseSpace') === 'true')
  const enterpriseSpaceId = ref(localStorage.getItem('enterpriseSpaceId') || null)
  const enterpriseSpaceName = ref(localStorage.getItem('enterpriseSpaceName') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(t, name, p, id, devMode = false, enterpriseMeta = {}) {
    token.value = t
    username.value = name || ''
    phone.value = p || ''
    userId.value = id || null
    isDevMode.value = devMode
    hasEnterpriseSpace.value = !!enterpriseMeta.hasEnterpriseSpace
    enterpriseSpaceId.value = enterpriseMeta.enterpriseSpaceId || null
    enterpriseSpaceName.value = enterpriseMeta.enterpriseSpaceName || ''

    if (t) {
      localStorage.setItem('token', t)
      localStorage.setItem('username', name || '')
      localStorage.setItem('phone', p || '')
      localStorage.setItem('userId', id || '')
      localStorage.setItem('isDevMode', devMode ? 'true' : 'false')
      localStorage.setItem('hasEnterpriseSpace', enterpriseMeta.hasEnterpriseSpace ? 'true' : 'false')
      localStorage.setItem('enterpriseSpaceId', enterpriseMeta.enterpriseSpaceId || '')
      localStorage.setItem('enterpriseSpaceName', enterpriseMeta.enterpriseSpaceName || '')
    } else {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('phone')
      localStorage.removeItem('userId')
      localStorage.removeItem('isDevMode')
      localStorage.removeItem('hasEnterpriseSpace')
      localStorage.removeItem('enterpriseSpaceId')
      localStorage.removeItem('enterpriseSpaceName')
    }
  }

  function logout() {
    setAuth('', '', '', null, false)
  }

  return {
    token,
    username,
    phone,
    userId,
    isDevMode,
    hasEnterpriseSpace,
    enterpriseSpaceId,
    enterpriseSpaceName,
    isLoggedIn,
    setAuth,
    logout,
  }
})
