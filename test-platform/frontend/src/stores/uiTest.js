import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getCasesByOrganization,
  getCategoriesByOrganization,
  createCategory as createCategoryApi,
  updateCategory as updateCategoryApi,
  deleteCategory as deleteCategoryApi,
  createCase as createCaseApi,
  updateCase as updateCaseApi,
  getCase as getCaseApi,
  deleteCase as deleteCaseApi,
} from '../api/uiTest'
import { useOrgStore } from './org'

export const useUiTestStore = defineStore('uiTest', () => {
  const orgStore = useOrgStore()
  const cases = ref([])
  const modules = ref([])
  const loading = ref(false)
  const error = ref(null)
  const selectedModuleKey = ref('all')

  function normalizeModuleKey(moduleKey) {
    return String(moduleKey || '').trim()
  }

  async function fetchModules(organizationId = orgStore.currentOrganizationId) {
    if (!organizationId) {
      modules.value = []
      return []
    }

    const response = await getCategoriesByOrganization(organizationId)
    modules.value = (response.data || []).map(item => ({
      id: item.id ?? null,
      key: normalizeModuleKey(item.key ?? item.moduleKey),
      name: normalizeModuleKey(item.name ?? item.displayName ?? item.key ?? item.moduleKey),
      caseCount: Number(item.caseCount ?? 0),
      deletable: Boolean(item.deletable ?? Number(item.caseCount ?? 0) === 0),
    })).filter(item => item.key && item.name)

    if (selectedModuleKey.value !== 'all' && !modules.value.some(item => item.key === selectedModuleKey.value)) {
      selectedModuleKey.value = 'all'
    }
    return modules.value
  }

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
      await fetchModules(organizationId)
    } catch (err) {
      error.value = err.message || '获取用例列表失败'
      console.error('获取UI测试用例失败:', err)
      cases.value = []
      modules.value = []
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

  async function addModule(moduleKey, organizationId = orgStore.currentOrganizationId) {
    const normalized = normalizeModuleKey(moduleKey)
    if (!normalized) {
      throw new Error('分类名称不能为空')
    }
    if (!organizationId) {
      throw new Error('请先选择所属组织')
    }
    const response = await createCategoryApi({
      organizationId: Number(organizationId),
      key: normalized,
      name: normalized,
    })
    await fetchModules(organizationId)
    return response.data
  }

  async function renameModule(id, name, organizationId = orgStore.currentOrganizationId) {
    const normalized = normalizeModuleKey(name)
    if (!id) {
      throw new Error('分类标识无效')
    }
    if (!normalized) {
      throw new Error('分类名称不能为空')
    }

    const response = await updateCategoryApi(id, { name: normalized })
    await fetchModules(organizationId)
    return response.data
  }

  async function removeModule(id, organizationId = orgStore.currentOrganizationId) {
    if (!id) {
      throw new Error('分类标识无效')
    }
    await deleteCategoryApi(id)
    await fetchModules(organizationId)
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
    fetchModules,
    addModule,
    renameModule,
    removeModule,
    setModule,
    findCaseById,
    createCase,
    updateCaseMeta,
    removeCase,
    getCaseById,
    fetchCases,
  }
})
