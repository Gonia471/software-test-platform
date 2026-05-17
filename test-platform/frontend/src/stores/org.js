import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserOrganizations, createOrganization } from '../api/organization'

export const useOrgStore = defineStore('org', () => {
  const organizations = ref([])
  const currentOrganizationId = ref(localStorage.getItem('currentOrganizationId') || null)
  const lastFetchError = ref('')
  const currentOrganization = computed(() =>
    organizations.value.find(o => o.id === Number(currentOrganizationId.value))
  )

  async function fetchOrganizations() {
    try {
      const res = await getUserOrganizations()
      organizations.value = res.data || []
      lastFetchError.value = ''

      const hasCurrentOrganization = organizations.value.some(
        o => Number(o.id) === Number(currentOrganizationId.value)
      )

      if (organizations.value.length > 0) {
        if (!currentOrganizationId.value || !hasCurrentOrganization) {
          setCurrentOrganization(organizations.value[0].id)
        }
      } else {
        currentOrganizationId.value = null
        localStorage.removeItem('currentOrganizationId')
      }
    } catch (err) {
      console.error('获取组织列表失败:', err)
      lastFetchError.value = err.response?.data?.message || err.message || '获取组织列表失败'
      organizations.value = []
      currentOrganizationId.value = null
      localStorage.removeItem('currentOrganizationId')
      return []
    }
    return organizations.value
  }

  function setCurrentOrganization(id) {
    currentOrganizationId.value = id
    localStorage.setItem('currentOrganizationId', id)
  }

  async function createOrg(data) {
    const res = await createOrganization(data)
    await fetchOrganizations()
    return res
  }

  return {
    organizations,
    currentOrganizationId,
    currentOrganization,
    lastFetchError,
    fetchOrganizations,
    setCurrentOrganization,
    createOrg
  }
})
