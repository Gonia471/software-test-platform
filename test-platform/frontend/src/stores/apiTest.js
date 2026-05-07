import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

const STORAGE_KEYS = {
  collections: 'api-test-collections-v1',
  environments: 'api-test-environments-v1',
  currentEnvId: 'api-test-current-env-v1',
  requestHistory: 'api-test-request-history-v1',
}

const HTTP_METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS']

function loadJson(key, defaultValue = []) {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return defaultValue
    const parsed = JSON.parse(raw)
    return parsed ?? defaultValue
  } catch {
    return defaultValue
  }
}

function saveJson(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

function generateId() {
  return `id-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

export const useApiTestStore = defineStore('apiTest', () => {
  const collections = ref(loadJson(STORAGE_KEYS.collections, [
    { id: 'root', name: '我的接口', type: 'folder', children: [] },
  ]))

  const environments = ref(loadJson(STORAGE_KEYS.environments, [
    { id: 'env-dev', name: '开发环境', variables: [{ key: 'base_url', value: 'http://localhost:8080' }] },
    { id: 'env-test', name: '测试环境', variables: [{ key: 'base_url', value: 'https://test.example.com' }] },
  ]))

  const currentEnvId = ref(localStorage.getItem(STORAGE_KEYS.currentEnvId) || environments.value[0]?.id || '')

  const requestHistory = ref(loadJson(STORAGE_KEYS.requestHistory, []))

  const selectedCollectionId = ref(null)
  const selectedCaseId = ref(null)

  const currentEnv = computed(() =>
    environments.value.find((e) => e.id === currentEnvId.value) || null,
  )

  const currentVariables = computed(() => {
    const env = currentEnv.value
    if (!env?.variables) return {}
    return env.variables.reduce((acc, v) => {
      acc[v.key] = v.value
      return acc
    }, {})
  })

  function resolveVariables(str) {
    if (!str || typeof str !== 'string') return str
    return str.replace(/\{\{(\w+)\}\}/g, (_, key) => currentVariables.value[key] ?? `{{${key}}}`)
  }

  function setCurrentEnv(id) {
    currentEnvId.value = id
    localStorage.setItem(STORAGE_KEYS.currentEnvId, id)
  }

  function addEnvironment(name = '新环境') {
    const env = {
      id: generateId(),
      name,
      variables: [{ key: 'base_url', value: '' }],
    }
    environments.value.push(env)
    saveJson(STORAGE_KEYS.environments, environments.value)
    return env
  }

  function updateEnvironment(id, payload) {
    const idx = environments.value.findIndex((e) => e.id === id)
    if (idx === -1) return
    environments.value[idx] = { ...environments.value[idx], ...payload }
    saveJson(STORAGE_KEYS.environments, environments.value)
  }

  function removeEnvironment(id) {
    environments.value = environments.value.filter((e) => e.id !== id)
    if (currentEnvId.value === id) {
      currentEnvId.value = environments.value[0]?.id || ''
    }
    saveJson(STORAGE_KEYS.environments, environments.value)
  }

  function addCollection(parentId, name = '新集合', type = 'folder') {
    const item = {
      id: generateId(),
      parentId: parentId || 'root',
      name,
      type,
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
    }
    const parent = findNodeById(collections.value, parentId || 'root')
    if (parent && parent.children) {
      parent.children = parent.children || []
      parent.children.push(item)
    } else {
      collections.value.push(item)
    }
    saveCollections()
    return item
  }

  function findNodeById(nodes, id) {
    if (!nodes) return null
    for (const n of nodes) {
      if (n.id === id) return n
      const found = findNodeById(n.children, id)
      if (found) return found
    }
    return null
  }

  function saveCollections() {
    saveJson(STORAGE_KEYS.collections, collections.value)
  }

  function updateCollection(id, payload) {
    const node = findNodeById(collections.value, id)
    if (!node) return
    Object.assign(node, payload)
    saveCollections()
  }

  function removeCollection(id) {
    const removeFrom = (nodes, parent) => {
      if (!nodes) return
      for (let i = 0; i < nodes.length; i++) {
        if (nodes[i].id === id) {
          nodes.splice(i, 1)
          return true
        }
        if (removeFrom(nodes[i].children, nodes[i])) return true
      }
      return false
    }
    removeFrom(collections.value)
    saveCollections()
  }

  function addRequestToHistory(record) {
    const max = 50
    requestHistory.value = [record, ...requestHistory.value.slice(0, max - 1)]
    saveJson(STORAGE_KEYS.requestHistory, requestHistory.value)
  }

  return {
    HTTP_METHODS,
    collections,
    environments,
    currentEnvId,
    currentEnv,
    currentVariables,
    requestHistory,
    selectedCollectionId,
    selectedCaseId,
    resolveVariables,
    setCurrentEnv,
    addEnvironment,
    updateEnvironment,
    removeEnvironment,
    addCollection,
    findNodeById,
    updateCollection,
    removeCollection,
    saveCollections,
    addRequestToHistory,
  }
})
