import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getCollections,
  saveCollection as saveCollectionApi,
  deleteCollection as deleteCollectionApi,
  getEnvironments,
  saveEnvironment as saveEnvironmentApi,
  deleteEnvironment as deleteEnvironmentApi,
} from '../api/apiTest'
import { useOrgStore } from './org'

const HTTP_METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS']
const REQUEST_HISTORY_KEY = 'api-test-request-history-v1'

export const useApiTestStore = defineStore('apiTest', () => {
  const orgStore = useOrgStore()
  const rawCollections = ref([])
  const environments = ref([])
  const loading = ref(false)
  const error = ref(null)

  const selectedCollectionId = ref(null)
  const selectedCaseId = ref(null)
  const currentEnvId = ref('')
  const requestHistory = ref(loadRequestHistory())

  function normalizeEnvironment(env) {
    if (!env || typeof env !== 'object') {
      return {
        id: String(Date.now()),
        name: '未命名环境',
        variables: [],
      }
    }
    return {
      ...env,
      id: String(env.id ?? Date.now()),
      name: env.name || '未命名环境',
      variables: Array.isArray(env.variables)
        ? env.variables.map((variable) => ({
            key: variable.key || '',
            value: variable.value || '',
          }))
        : [],
    }
  }

  // 从后端API获取集合列表
  async function fetchCollections() {
    loading.value = true
    error.value = null
    try {
      const response = await getCollections()
      const collectionList = extractResponseData(response)
      // 统一转换 nodeType 为 type，方便前端组件使用
      const mapNode = (node) => ({
        ...node,
        type: node.nodeType?.toLowerCase(),
        children: node.children ? node.children.map(mapNode) : []
      })
      rawCollections.value = (Array.isArray(collectionList) ? collectionList : []).map(mapNode)
    } catch (err) {
      error.value = err.message || '获取集合列表失败'
      console.error('获取API测试集合失败:', err)
      rawCollections.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchEnvironments() {
    loading.value = true
    error.value = null
    try {
      const response = await getEnvironments()
      const environmentList = extractResponseData(response)
      environments.value = (Array.isArray(environmentList) ? environmentList : []).map(normalizeEnvironment)
      if (!currentEnvId.value && environments.value.length > 0) {
        currentEnvId.value = environments.value[0].id
      }
    } catch (err) {
      error.value = err.message || '获取环境列表失败'
      console.error('获取API测试环境失败:', err)
      environments.value = []
    } finally {
      loading.value = false
    }
  }

  // 添加或更新集合
  async function saveCollection(data) {
    loading.value = true
    error.value = null
    try {
      const response = await saveCollectionApi(data)
      // 保存成功后重新获取列表
      await fetchCollections()
      return extractResponseData(response)
    } catch (err) {
      error.value = err.message || '保存集合失败'
      console.error('保存API测试集合失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  // 添加集合（简化版）
  async function addCollection(parentId, name = '新集合', type = 'folder') {
    const item = {
      parentId: parentId || null,
      name,
      nodeType: type.toUpperCase(), // 发送给后端使用 nodeType
      children: type === 'folder' ? [] : undefined,
      method: type === 'case' ? 'GET' : undefined,
      url: type === 'case' ? '' : undefined,
      params: type === 'case' ? [] : undefined,
      headers: type === 'case' ? [] : undefined,
      bodyType: type === 'case' ? 'none' : undefined,
      bodyRaw: type === 'case' ? '' : undefined,
      authType: type === 'case' ? 'none' : undefined,
      authConfig: type === 'case' ? {} : undefined,
      assertions: type === 'case' ? [] : undefined,
      // 设置组织ID，使用当前选择的组织或第一个有权限的组织
      organizationId: orgStore.currentOrganizationId || (orgStore.organizations[0]?.id || null)
    }
    return await saveCollection(item)
  }

  // 更新集合
  async function updateCollection(id, payload) {
    const node = findNodeById(rawCollections.value, id)
    if (!node) return null
    
    const updatedData = { ...node, ...payload }
    return await saveCollection(updatedData)
  }

  // 删除集合
  async function removeCollection(id) {
    loading.value = true
    error.value = null
    try {
      await deleteCollectionApi(id)
      // 删除成功后重新获取列表
      await fetchCollections()
    } catch (err) {
      error.value = err.message || '删除集合失败'
      console.error('删除API测试集合失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  // 查找节点
  function findNodeById(nodes, id) {
    if (!nodes) return null
    for (const n of nodes) {
      if (n.id === id) return n
      const found = findNodeById(n.children, id)
      if (found) return found
    }
    return null
  }

  // 设置当前选中的集合
  function setSelectedCollection(id) {
    selectedCollectionId.value = id
  }

  // 设置当前选中的用例
  function setSelectedCase(id) {
    selectedCaseId.value = id
  }

  function setCurrentEnv(id) {
    currentEnvId.value = id ? String(id) : ''
  }

  const collections = computed(() => {
    const currentOrgId = orgStore.currentOrganizationId ? Number(orgStore.currentOrganizationId) : null
    if (!currentOrgId) {
      return []
    }
    return filterCollectionsByOrganization(rawCollections.value, currentOrgId)
  })

  async function addEnvironment() {
    const payload = {
      name: `新环境 ${environments.value.length + 1}`,
      variables: [],
    }
    const response = await saveEnvironmentApi(payload)
    let env = normalizeEnvironment(extractResponseData(response))
    if (!env.id || env.id === 'undefined') {
      await fetchEnvironments()
      env = environments.value[environments.value.length - 1] || normalizeEnvironment(payload)
    } else {
      environments.value.push(env)
    }
    currentEnvId.value = env.id
    return env
  }

  async function updateEnvironment(id, payload) {
    const current = environments.value.find((env) => env.id === String(id))
    if (!current) return null
    const response = await saveEnvironmentApi({
      ...current,
      ...payload,
      id: Number(id),
    })
    const nextEnv = normalizeEnvironment(extractResponseData(response))
    const index = environments.value.findIndex((env) => env.id === String(id))
    if (index !== -1) {
      environments.value.splice(index, 1, nextEnv)
    }
    return nextEnv
  }

  async function removeEnvironment(id) {
    await deleteEnvironmentApi(id)
    environments.value = environments.value.filter((env) => env.id !== String(id))
    if (currentEnvId.value === String(id)) {
      currentEnvId.value = environments.value[0]?.id || ''
    }
  }

  const currentEnvironment = computed(() =>
    environments.value.find((env) => env.id === currentEnvId.value) || null,
  )

  function resolveVariables(input) {
    if (typeof input !== 'string' || !input) return input
    const variableMap = Object.fromEntries(
      (currentEnvironment.value?.variables || [])
        .filter((variable) => variable.key)
        .map((variable) => [variable.key, variable.value ?? '']),
    )
    return input.replace(/\{\{\s*([^}]+?)\s*\}\}/g, (_, rawKey) => {
      const key = String(rawKey).trim()
      return Object.prototype.hasOwnProperty.call(variableMap, key)
        ? String(variableMap[key])
        : `{{${key}}}`
    })
  }

  function addRequestToHistory(record) {
    if (!record?.url) return
    const nextHistory = [record, ...requestHistory.value.filter((item) => item.url !== record.url || item.method !== record.method)]
      .slice(0, 20)
    requestHistory.value = nextHistory
    localStorage.setItem(REQUEST_HISTORY_KEY, JSON.stringify(nextHistory))
  }

  return {
    HTTP_METHODS,
    collections,
    rawCollections,
    environments,
    loading,
    error,
    selectedCollectionId,
    selectedCaseId,
    currentEnvId,
    currentEnvironment,
    requestHistory,
    fetchCollections,
    fetchEnvironments,
    saveCollection,
    addCollection,
    updateCollection,
    removeCollection,
    findNodeById,
    setSelectedCollection,
    setSelectedCase,
    setCurrentEnv,
    addEnvironment,
    updateEnvironment,
    removeEnvironment,
    resolveVariables,
    addRequestToHistory,
  }
})

function filterCollectionsByOrganization(nodes, organizationId) {
  if (!Array.isArray(nodes) || !organizationId) {
    return []
  }

  return nodes
    .map((node) => {
      const children = filterCollectionsByOrganization(node.children || [], organizationId)
      const matched = Number(node.organizationId) === Number(organizationId)
      if (!matched && children.length === 0) {
        return null
      }
      return {
        ...node,
        children,
      }
    })
    .filter(Boolean)
}

function loadRequestHistory() {
  try {
    const raw = localStorage.getItem(REQUEST_HISTORY_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function extractResponseData(response) {
  if (!response) return null
  const payload = response.data
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return payload.data
  }
  return payload
}
