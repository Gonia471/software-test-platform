<template>
  <div class="api-test-page">
    <div class="api-test-header">
      <div class="header-main">
        <h1 class="page-title">API 测试</h1>
        <p class="page-subtitle">统一管理接口集合、请求编辑、环境切换与响应结果，支持在一个工作台内完成接口调试与结果查看。</p>
      </div>
      <div class="header-actions-wrap">
        <div class="header-meta">
          <span class="meta-chip">{{ apiStore.environments.length }} 个环境</span>
          <span class="meta-chip meta-chip--subtle">{{ currentCase ? '已选接口' : '未选择接口' }}</span>
          <span v-if="currentCase?.creatorUsername" class="meta-chip meta-chip--subtle">创建人：{{ currentCase.creatorUsername }}</span>
        </div>
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
    </div>

    <div class="api-test-layout" :style="layoutStyle">
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

      <div class="resizer" @mousedown="startResize('left', $event)"></div>

      <div class="panel-center">
        <ApiRequestEditor
          v-if="apiStore.selectedCaseId"
          :request="currentRequest"
          :sending="sending"
          :history="apiStore.requestHistory"
          :http-methods="apiStore.HTTP_METHODS"
          @send="sendRequest"
          @update-request="onUpdateRequest"
          @apply-history="onApplyHistory"
          @open-script-library="scriptLibraryVisible = true"
        />
        <div v-else class="editor-empty">
          <div class="empty-content">
            <div class="empty-icon">
              <el-icon><Document /></el-icon>
            </div>
            <h3>开始测试</h3>
            <p>从左侧集合选择一个接口，或点击「新建」创建一个新的测试</p>
            <div class="empty-actions">
              <el-button type="primary" size="default" plain @click="onAddCase(null)">
                新建接口
              </el-button>
              <el-button size="default" @click="curlImportVisible = true">
                <el-icon><Upload /></el-icon>
                导入 CURL
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="resizer" @mousedown="startResize('right', $event)">
        <div class="collapse-trigger" @click.stop="toggleRightPanel">
          <el-icon>
            <component :is="rightPanelCollapsed ? ArrowLeft : ArrowRight" />
          </el-icon>
        </div>
      </div>

      <div class="panel-right" :class="{ 'is-collapsed': rightPanelCollapsed }">
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
      @add-env="handleAddEnvironment"
      @remove-env="handleRemoveEnvironment"
      @update-env="handleUpdateEnvironment"
      @set-current="(id) => apiStore.setCurrentEnv(id)"
    />

    <CurlImportModal
      v-model="curlImportVisible"
      @import="handleCurlImport"
    />

    <ScriptLibraryModal
      v-model="scriptLibraryVisible"
      @select="handleScriptSelect"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Upload, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import ApiCollectionTree from '../components/api-test/ApiCollectionTree.vue'
import ApiRequestEditor from '../components/api-test/ApiRequestEditor.vue'
import ApiResponsePanel from '../components/api-test/ApiResponsePanel.vue'
import ApiEnvironmentModal from '../components/api-test/ApiEnvironmentModal.vue'
import CurlImportModal from '../components/api-test/CurlImportModal.vue'
import ScriptLibraryModal from '../components/api-test/ScriptLibraryModal.vue'
import { useApiTestStore } from '../stores/apiTest'
import { useOrgStore } from '../stores/org'
import { sendRequest as apiSendRequest, saveApiExecution } from '../api/apiTest'

const apiStore = useApiTestStore()
const orgStore = useOrgStore()

const leftPanelWidth = ref(260)
const rightPanelWidth = ref(360)
const rightPanelCollapsed = ref(false)

const layoutStyle = computed(() => {
  const left = `${leftPanelWidth.value}px`
  const right = rightPanelCollapsed.value ? '0px' : `${rightPanelWidth.value}px`
  // 这里的 gridTemplateColumns 必须涵盖所有 5 列（左、拖拽1、中、拖拽2、右）
  return {
    display: 'grid',
    gridTemplateColumns: `${left} 12px 1fr 12px ${right}`,
    alignItems: 'stretch'
  }
})

function startResize(side, mouseEvent) {
  const startX = mouseEvent.clientX
  const startWidth = side === 'left' ? leftPanelWidth.value : rightPanelWidth.value

  const onMouseMove = (e) => {
    const delta = e.clientX - startX
    if (side === 'left') {
      leftPanelWidth.value = Math.max(200, Math.min(500, startWidth + delta))
    } else {
      rightPanelWidth.value = Math.max(300, Math.min(600, startWidth - delta))
    }
  }

  const onMouseUp = () => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
    document.body.style.cursor = ''
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
  document.body.style.cursor = 'col-resize'
}

function toggleRightPanel() {
  rightPanelCollapsed.value = !rightPanelCollapsed.value
}

const envModalVisible = ref(false)
const curlImportVisible = ref(false)
const scriptLibraryVisible = ref(false)
const sending = ref(false)
const response = ref(null)
const assertionResults = ref([])

async function handleAddEnvironment() {
  try {
    await apiStore.addEnvironment()
    ElMessage.success('已添加环境')
  } catch (err) {
    ElMessage.error('添加环境失败: ' + (err.message || '未知错误'))
  }
}

async function handleUpdateEnvironment(id, payload) {
  try {
    await apiStore.updateEnvironment(id, payload)
  } catch (err) {
    ElMessage.error('更新环境失败: ' + (err.message || '未知错误'))
  }
}

async function handleRemoveEnvironment(id) {
  try {
    await apiStore.removeEnvironment(id)
    ElMessage.success('已删除环境')
  } catch (err) {
    ElMessage.error('删除环境失败: ' + (err.message || '未知错误'))
  }
}

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
    prescripts: [],
    assertions: [],
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

async function onAddFolder(parentId) {
  try {
    const item = await apiStore.addCollection(parentId, '新文件夹', 'folder')
    ElMessage.success('已创建文件夹')
  } catch (err) {
    ElMessage.error('创建文件夹失败: ' + (err.message || '未知错误'))
  }
}

async function onAddCase(parentId) {
  try {
    const item = await apiStore.addCollection(parentId, '新接口', 'case')
    apiStore.selectedCaseId = item.id
    ElMessage.success('已创建接口')
  } catch (err) {
    ElMessage.error('创建接口失败: ' + (err.message || '未知错误'))
  }
}

async function handleCurlImport(parsedRequest) {
  try {
    const item = await apiStore.addCollection(null, '新导入接口', 'case')
    apiStore.selectedCaseId = item.id

    Object.assign(currentRequest, parsedRequest)
    await apiStore.updateCollection(item.id, {
      method: parsedRequest.method,
      url: parsedRequest.url,
      params: parsedRequest.params || [],
      headers: parsedRequest.headers || [],
      bodyType: parsedRequest.bodyType || 'none',
      bodyRaw: parsedRequest.bodyRaw || '',
      bodyRawType: parsedRequest.bodyRawType || 'json',
      bodyForm: parsedRequest.bodyForm || [],
      authType: parsedRequest.authType || 'none',
      authConfig: parsedRequest.authConfig || {},
    })

    ElMessage.success('已从 CURL 导入接口')
  } catch (err) {
    ElMessage.error('导入CURL失败: ' + (err.message || '未知错误'))
  }
}

function handleScriptSelect(script) {
  ElMessage.success(`已选择函数：${script.functionName}，可在前置脚本/后置脚本中引用`)
  scriptLibraryVisible.value = false
}

async function onRename(id, name) {
  try {
    await apiStore.updateCollection(id, { name })
    ElMessage.success('已重命名')
  } catch (err) {
    ElMessage.error('重命名失败: ' + (err.message || '未知错误'))
  }
}

async function onDelete(id) {
  try {
    await apiStore.removeCollection(id)
    if (apiStore.selectedCaseId === id) {
      apiStore.selectedCaseId = null
    }
    ElMessage.success('已删除')
  } catch (err) {
    ElMessage.error('删除失败: ' + (err.message || '未知错误'))
  }
}

async function onUpdateRequest(req) {
  Object.assign(currentRequest, req)
  if (apiStore.selectedCaseId) {
    try {
      await apiStore.updateCollection(apiStore.selectedCaseId, {
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
        prescripts: req.prescripts || [],
        assertions: req.assertions || [],
        stopOnFail: req.stopOnFail || false,
      })
    } catch (err) {
      console.error('更新请求失败:', err)
      ElMessage.error(err.response?.data?.message || err.message || '保存失败')
    }
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

    // 根据 HTTP 状态码判定测试结果
    // 2xx 判定为 SUCCESS，其余判定为 FAILED
    const testStatus = (data.status >= 200 && data.status < 300) ? 'SUCCESS' : 'FAILED'
    await saveExecutionRecord(data, headers, body, testStatus)
  } catch (err) {
    // 捕获到网络错误或代码异常
    const errorData = {
      status: err.response?.status || 0,
      statusText: err.response?.statusText || 'Error',
      headers: err.response?.headers || {},
      body: err.response?.data || '',
      duration: 0,
      size: 0,
    }
    
    response.value = {
      ...errorData,
      error: err.response?.data?.message || err.message || '请求失败',
    }
    ElMessage.error(response.value.error)

    await saveExecutionRecord(errorData, headers, body, 'FAILED')
  } finally {
    sending.value = false
  }
}

async function saveExecutionRecord(data, headers, body, status) {
  try {
    const currentCase = apiStore.findNodeById(apiStore.collections, apiStore.selectedCaseId)
    await saveApiExecution({
      collectionId: apiStore.selectedCaseId,
      collectionName: currentCase?.name || '未命名接口',
      status: status,
      request: {
        method: currentRequest.method,
        url: currentRequest.url,
        headers: headers,
        body: body,
      },
      response: {
        status: data.status,
        statusText: data.statusText,
        headers: data.headers,
        body: data.body,
        size: data.size,
        duration: data.duration,
      },
      assertions: assertionResults.value,
    })
  } catch (err) {
    console.error('保存执行记录失败:', err)
  }
}

function onApplyHistory(h) {
  currentRequest.method = h.method
  currentRequest.url = h.url
}

async function refreshCollectionsForOrganization() {
  await apiStore.fetchCollections()
  if (apiStore.selectedCaseId && !apiStore.findNodeById(apiStore.collections, apiStore.selectedCaseId)) {
    apiStore.selectedCaseId = null
    Object.assign(currentRequest, getDefaultRequest())
    response.value = null
    assertionResults.value = []
  }
}

async function saveCase() {
  if (!apiStore.selectedCaseId) {
    ElMessage.warning('请先选择或创建接口')
    return
  }
  await onUpdateRequest(currentRequest)
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
  // 获取API测试集合与环境数据
  refreshCollectionsForOrganization()
  apiStore.fetchEnvironments()
})

watch(
  () => orgStore.currentOrganizationId,
  async (orgId, previousOrgId) => {
    if (!orgId || orgId === previousOrgId) {
      return
    }
    await refreshCollectionsForOrganization()
  },
)

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
  padding: 0;
}

.api-test-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 20px 22px;
  border-radius: var(--border-radius);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, rgba(246, 250, 255, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: var(--card-shadow);
}

.header-main {
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.page-subtitle {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.header-actions-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.header-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.meta-chip--subtle {
  background: #f8fbff;
  color: var(--text-secondary);
}

.env-select {
  width: 150px;
}

.api-test-layout {
  flex: 1;
  min-height: 0;
  width: 100%;
  overflow: hidden;
}

.panel-left,
.panel-center,
.panel-right {
  min-height: 0;
  overflow: hidden;
  border-radius: var(--border-radius);
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: rgba(255, 255, 255, 0.95);
  transition: border-color 0.2s;
}

.panel-left:hover,
.panel-center:hover,
.panel-right:hover {
  border-color: rgba(59, 130, 246, 0.14);
}

.panel-right.is-collapsed {
  display: none;
}

.resizer {
  width: 12px;
  margin: 0 -6px;
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  position: relative;
  z-index: 10;
  background: transparent;
}

.resizer:hover {
  background: rgba(59, 130, 246, 0.08);
}

.resizer::after {
  content: '';
  width: 2px;
  height: 24px;
  background: #e2e8f0;
  border-radius: 1px;
  opacity: 0;
  transition: opacity 0.2s;
}

.resizer:hover::after {
  opacity: 1;
}

.collapse-trigger {
  width: 16px;
  height: 32px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #64748b;
  position: absolute;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  opacity: 0;
  transition: all 0.2s;
}

.resizer:hover .collapse-trigger {
  opacity: 1;
}

.collapse-trigger:hover {
   color: var(--el-color-primary);
   border-color: var(--el-color-primary);
}

.editor-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.9) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.empty-content {
  text-align: center;
  max-width: 320px;
}

.empty-icon {
  font-size: 48px;
  color: #cbd5e1;
  margin-bottom: 16px;
}

.empty-content h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.empty-content p {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 24px;
  line-height: 1.6;
}

.empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

:deep(.el-button--primary) {
  border-radius: 8px;
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-tabs__item) {
  font-weight: 500;
}
</style>
