import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCasesByOrganization, createCase as createCaseApi, updateCase as updateCaseApi, getCase as getCaseApi, deleteCase as deleteCaseApi } from '../api/uiTest'
import { useOrgStore } from './org'

const defaultModules = [
  { key: 'test', name: 'test' },
]

export const useUiTestStore = defineStore('uiTest', () => {
  const orgStore = useOrgStore()
  const modules = ref(defaultModules)
  const cases = ref([])
  const loading = ref(false)
  const error = ref(null)

  const selectedModuleKey = ref('all')

  // 从后端API获取当前组织的用例列表
  async function fetchCases(organizationId = orgStore.currentOrganizationId) {
    loading.value = true
    error.value = null
    try {
      if (!organizationId) {
        cases.value = []
        return
      }
      const response = await getCasesByOrganization(organizationId)
      cases.value = response.data || []
    } catch (err) {
      error.value = err.message || '获取用例列表失败'
      console.error('获取UI测试用例失败:', err)
      cases.value = []
    } finally {
      loading.value = false
    }
  }

  const filteredCases = computed(() => {
    return cases.value.filter((c) => {
      if (selectedModuleKey.value && selectedModuleKey.value !== 'all') {
        return c.moduleKey === selectedModuleKey.value
      }
      return true
    })
  })

  function setModule(moduleKey) {
    selectedModuleKey.value = moduleKey
  }

  const nextSeq = computed(() => {
    if (!cases.value.length) {
      return 1
    }
    return Math.max(...cases.value.map((item) => Number(item.id) || 0)) + 1
  })

  function findCaseById(id) {
    return cases.value.find((item) => Number(item.id) === Number(id)) || null
  }

  // 创建用例
  async function createCase(data) {
    loading.value = true
    error.value = null
    try {
      const response = await createCaseApi(data)
      // 创建成功后重新获取列表
      await fetchCases()
      return response.data
    } catch (err) {
      error.value = err.message || '创建用例失败'
      console.error('创建UI测试用例失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  // 更新用例
  async function updateCaseMeta(id, data) {
    loading.value = true
    error.value = null
    try {
      const response = await updateCaseApi(id, data)
      // 更新成功后重新获取列表
      await fetchCases()
      return response.data
    } catch (err) {
      error.value = err.message || '更新用例失败'
      console.error('更新UI测试用例失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  // 删除用例
  async function removeCase(id) {
    loading.value = true
    error.value = null
    try {
      await deleteCaseApi(id)
      // 删除成功后重新获取列表
      await fetchCases()
    } catch (err) {
      error.value = err.message || '删除用例失败'
      console.error('删除UI测试用例失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  // 获取单个用例详情
  async function getCaseById(id) {
    loading.value = true
    error.value = null
    try {
      const response = await getCaseApi(id)
      return response.data
    } catch (err) {
      error.value = err.message || '获取用例详情失败'
      console.error('获取UI测试用例详情失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    modules,
    cases,
    nextSeq,
    loading,
    error,
    selectedModuleKey,
    filteredCases,
    setModule,
    findCaseById,
    createCase,
    updateCaseMeta,
    removeCase,
    getCaseById,
    fetchCases,
  }
})
