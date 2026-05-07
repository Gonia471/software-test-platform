<template>
  <div class="api-test-page">
    <div class="api-test-header">
      <h1 class="page-title">API 测试</h1>
      <div class="header-actions">
        <el-select
          v-model="apiStore.currentEnvId"
          placeholder="选择环境"
          size="small"
          class="env-select"
          @change="(id) => apiStore.setCurrentEnv(id)"
        >
          <el-option
            v-for="env in apiStore.environments"
            :key="env.id"
            :label="env.name"
            :value="env.id"
          />
        </el-select>
        <el-button size="small" @click="saveCase" :disabled="!currentCase">保存</el-button>
        <el-button size="small" type="primary" plain @click="envModalVisible = true">
          环境管理
        </el-button>
      </div>
    </div>

    <div class="api-test-layout">
      <div class="panel-left">
        <ApiCollectionTree
          :collections="apiStore.collections"
          :selected-case-id="apiStore.selectedCaseId"
          @select="onSelectCase"
          @add-folder="onAddFolder"
          @add-case="onAddCase"
          @rename="onRename"
          @delete="onDelete"
          @open-env="envModalVisible = true"
        />
      </div>

      <div class="panel-center">
        <ApiRequestEditor
          :request="currentRequest"
          :sending="sending"
          :history="apiStore.requestHistory"
          :http-methods="apiStore.HTTP_METHODS"
          @send="sendRequest"
          @update-request="onUpdateRequest"
          @apply-history="onApplyHistory"
        />
      </div>

      <div class="panel-right">
        <ApiResponsePanel
          :response="response"
          :assertion-results="assertionResults"
        />
      </div>
    </div>

    <ApiEnvironmentModal
      v-model="envModalVisible"
      :environments="apiStore.environments"
      :current-env-id="apiStore.currentEnvId"
      @add-env="apiStore.addEnvironment()"
      @remove-env="apiStore.removeEnvironment"
      @update-env="apiStore.updateEnvironment"
      @set-current="(id) => apiStore.setCurrentEnv(id)"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import ApiCollectionTree from '../components/api-test/ApiCollectionTree.vue'
import ApiRequestEditor from '../components/api-test/ApiRequestEditor.vue'
import ApiResponsePanel from '../components/api-test/ApiResponsePanel.vue'
import ApiEnvironmentModal from '../components/api-test/ApiEnvironmentModal.vue'
import { useApiTestStore } from '../stores/apiTest'
import { sendRequest as apiSendRequest } from '../api/apiTest'

const apiStore = useApiTestStore()

const envModalVisible = ref(false)
const sending = ref(false)
const response = ref(null)
const assertionResults = ref([])

function getDefaultRequest() {
  return {
    method: 'GET',
    url: '',
    params: [],
    headers: [],
    bodyType: 'none',
    bodyRaw: '',
    bodyRawType: 'json',
    bodyForm: [],
    authType: 'none',
    authConfig: {},
  }
}

const currentRequest = reactive(getDefaultRequest())

const currentCase = computed(() => {
  if (!apiStore.selectedCaseId) return null
  return apiStore.findNodeById(apiStore.collections, apiStore.selectedCaseId)
})

watch(
  () => apiStore.selectedCaseId,
  (id) => {
    if (!id) {
      Object.assign(currentRequest, getDefaultRequest())
      return
    }
    const node = apiStore.findNodeById(apiStore.collections, id)
    if (node && node.type === 'case') {
      currentRequest.method = node.method || 'GET'
      currentRequest.url = node.url || ''
      currentRequest.params = node.params || []
      currentRequest.headers = node.headers || []
      currentRequest.bodyType = node.bodyType || 'none'
      currentRequest.bodyRaw = node.bodyRaw || ''
      currentRequest.bodyRawType = node.bodyRawType || 'json'
      currentRequest.bodyForm = node.bodyForm || []
      currentRequest.authType = node.authType || 'none'
      currentRequest.authConfig = node.authConfig || {}
    }
  },
  { immediate: true },
)

function onSelectCase(id) {
  apiStore.selectedCaseId = id
}

function onAddFolder(parentId) {
  const item = apiStore.addCollection(parentId, '新文件夹', 'folder')
  ElMessage.success('已创建文件夹')
}

function onAddCase(parentId) {
  const item = apiStore.addCollection(parentId, '新接口', 'case')
  apiStore.selectedCaseId = item.id
  ElMessage.success('已创建接口')
}

function onRename(id, name) {
  apiStore.updateCollection(id, { name })
  ElMessage.success('已重命名')
}

function onDelete(id) {
  apiStore.removeCollection(id)
  if (apiStore.selectedCaseId === id) {
    apiStore.selectedCaseId = null
  }
  ElMessage.success('已删除')
}

function onUpdateRequest(req) {
  Object.assign(currentRequest, req)
  if (apiStore.selectedCaseId) {
    apiStore.updateCollection(apiStore.selectedCaseId, {
      method: req.method,
      url: req.url,
      params: req.params,
      headers: req.headers,
      bodyType: req.bodyType,
      bodyRaw: req.bodyRaw,
      bodyRawType: req.bodyRawType,
      bodyForm: req.bodyForm,
      authType: req.authType,
      authConfig: req.authConfig,
    })
  }
}

function buildUrl() {
  let url = apiStore.resolveVariables(currentRequest.url)
  const params = (currentRequest.params || []).filter((p) => p.enabled !== false && p.key)
  if (params.length) {
    const qs = params.map((p) => `${encodeURIComponent(p.key)}=${encodeURIComponent(p.value || '')}`).join('&')
    url += (url.includes('?') ? '&' : '?') + qs
  }
  return url
}

function buildHeaders() {
  const headers = {}
  const list = (currentRequest.headers || []).filter((h) => h.enabled !== false && h.key)
  list.forEach((h) => { headers[h.key] = h.value || '' })

  if (currentRequest.authType === 'bearer' && currentRequest.authConfig?.token) {
    headers['Authorization'] = `Bearer ${apiStore.resolveVariables(currentRequest.authConfig.token)}`
  } else if (currentRequest.authType === 'basic' && currentRequest.authConfig?.username) {
    const u = apiStore.resolveVariables(currentRequest.authConfig.username)
    const p = apiStore.resolveVariables(currentRequest.authConfig.password || '')
    headers['Authorization'] = 'Basic ' + btoa(`${u}:${p}`)
  } else if (currentRequest.authType === 'apikey' && currentRequest.authConfig?.key) {
    headers[currentRequest.authConfig.key] = apiStore.resolveVariables(currentRequest.authConfig.value || '')
  }

  if (currentRequest.bodyType === 'raw' && currentRequest.bodyRaw) {
    if (!headers['Content-Type']) {
      headers['Content-Type'] = currentRequest.bodyRawType === 'json'
        ? 'application/json'
        : currentRequest.bodyRawType === 'xml'
          ? 'application/xml'
          : 'text/plain'
    }
  }
  return headers
}

function buildBody() {
  if (currentRequest.bodyType === 'none') return ''
  if (currentRequest.bodyType === 'raw') return currentRequest.bodyRaw || ''
  const form = (currentRequest.bodyForm || []).filter((f) => f.key)
  if (currentRequest.bodyType === 'x-www-form-urlencoded') {
    return form.map((f) => `${encodeURIComponent(f.key)}=${encodeURIComponent(f.value || '')}`).join('&')
  }
  if (currentRequest.bodyType === 'form-data') {
    return new URLSearchParams(form.reduce((acc, f) => { acc[f.key] = f.value || ''; return acc }, {})).toString()
  }
  return ''
}

async function sendRequest() {
  const url = buildUrl()
  if (!url) {
    ElMessage.warning('请输入请求 URL')
    return
  }
  sending.value = true
  response.value = null
  try {
    const headers = buildHeaders()
    const body = buildBody()
    const res = await apiSendRequest({
      method: currentRequest.method,
      url,
      headers: Object.keys(headers).reduce((acc, k) => { acc[k] = headers[k]; return acc }, {}),
      body,
    })
    const data = res.data || res
    response.value = {
      status: data.status,
      statusText: data.statusText,
      headers: data.headers,
      body: data.body,
      duration: data.duration || 0,
      size: data.size || 0,
    }
    apiStore.addRequestToHistory({
      id: Date.now(),
      method: currentRequest.method,
      url,
      status: data.status,
    })
  } catch (err) {
    response.value = {
      status: 0,
      statusText: 'Error',
      error: err.response?.data?.message || err.message || '请求失败',
      body: '',
      duration: 0,
      size: 0,
    }
    ElMessage.error(response.value.error)
  } finally {
    sending.value = false
  }
}

function onApplyHistory(h) {
  currentRequest.method = h.method
  currentRequest.url = h.url
}

function saveCase() {
  if (!apiStore.selectedCaseId) {
    ElMessage.warning('请先选择或创建接口')
    return
  }
  onUpdateRequest(currentRequest)
  apiStore.saveCollections()
  ElMessage.success('已保存')
}

function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    sendRequest()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    saveCase()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.api-test-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  min-height: 500px;
  padding: 0 4px;
}

.api-test-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 0 4px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  letter-spacing: -0.02em;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.env-select {
  width: 150px;
}

.api-test-layout {
  flex: 1;
  display: grid;
  grid-template-columns: 280px 1fr 400px;
  gap: 16px;
  min-height: 0;
}

.panel-left,
.panel-center,
.panel-right {
  min-height: 0;
  overflow: hidden;
}

.panel-left {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
}

.panel-center,
.panel-right {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
}
</style>
