import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserOrganizations, createOrganization } from '../api/organization'

export const useOrgStore = defineStore('org', () => {
  const organizations = ref([])
  const currentOrganizationId = ref(localStorage.getItem('currentOrganizationId') || null)
  const currentOrganization = computed(() =>
    organizations.value.find(o => o.id === Number(currentOrganizationId.value))
  )

  async function fetchOrganizations() {
    const res = await getUserOrganizations()
    organizations.value = res.data || []

    if (organizations.value.length > 0 && !currentOrganizationId.value) {
      setCurrentOrganization(organizations.value[0].id)
    }
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
    fetchOrganizations,
    setCurrentOrganization,
    createOrg
  }
})