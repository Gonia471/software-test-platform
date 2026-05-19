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
      :organization-name="orgStore.currentOrganization?.name || ''"
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
import {
  listPrescripts,
  savePrescripts as savePrescriptsApi,
  getScriptByFunctionName,
  testScript,
} from '../api/apiTestAdvanced'

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
const AUTO_SAVE_DELAY = 400
let autoSaveTimer = null
let pendingSaveCaseId = null
let pendingSavePayload = null
let prescriptLoadVersion = 0
let prescriptDraftVersion = 0

function toRequestPayload(req) {
  return {
    method: req.method,
    url: req.url,
    params: Array.isArray(req.params) ? req.params.map((item) => ({ ...item })) : [],
    headers: Array.isArray(req.headers) ? req.headers.map((item) => ({ ...item })) : [],
    bodyType: req.bodyType,
    bodyRaw: req.bodyRaw,
    bodyRawType: req.bodyRawType,
    bodyForm: Array.isArray(req.bodyForm) ? req.bodyForm.map((item) => ({ ...item })) : [],
    authType: req.authType,
    authConfig: req.authConfig ? { ...req.authConfig } : {},
    prescripts: Array.isArray(req.prescripts) ? JSON.parse(JSON.stringify(req.prescripts)) : [],
    assertions: Array.isArray(req.assertions) ? JSON.parse(JSON.stringify(req.assertions)) : [],
    stopOnFail: req.stopOnFail || false,
  }
}

function getCollectionSavePayload(req) {
  return {
    method: req.method,
    url: req.url,
    params: Array.isArray(req.params) ? req.params.map((item) => ({ ...item })) : [],
    headers: Array.isArray(req.headers) ? req.headers.map((item) => ({ ...item })) : [],
    bodyType: req.bodyType,
    bodyRaw: req.bodyRaw,
    bodyRawType: req.bodyRawType,
    bodyForm: Array.isArray(req.bodyForm) ? req.bodyForm.map((item) => ({ ...item })) : [],
    authType: req.authType,
    authConfig: req.authConfig ? { ...req.authConfig } : {},
    assertions: Array.isArray(req.assertions) ? JSON.parse(JSON.stringify(req.assertions)) : [],
    stopOnFail: req.stopOnFail || false,
  }
}

function clearAutoSaveTimer() {
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
    autoSaveTimer = null
  }
}

function scheduleAutoSave(req) {
  if (!apiStore.selectedCaseId) {
    return
  }
  pendingSaveCaseId = apiStore.selectedCaseId
  pendingSavePayload = toRequestPayload(req)
  clearAutoSaveTimer()
  autoSaveTimer = setTimeout(() => {
    flushPendingSave({ silent: true }).catch(() => {})
  }, AUTO_SAVE_DELAY)
}

async function flushPendingSave(options = {}) {
  const { silent = false } = options
  clearAutoSaveTimer()

  const caseId = pendingSaveCaseId || apiStore.selectedCaseId
  const payload = pendingSavePayload || toRequestPayload(currentRequest)
  if (!caseId) {
    pendingSaveCaseId = null
    pendingSavePayload = null
    return
  }

  pendingSaveCaseId = null
  pendingSavePayload = null

  try {
    await apiStore.updateCollection(caseId, getCollectionSavePayload(payload))
    await savePrescriptsForCase(caseId, payload.prescripts || [])
  } catch (err) {
    console.error('更新请求失败:', err)
    if (!silent) {
      ElMessage.error(err.response?.data?.message || err.message || '保存失败')
    }
    throw err
  }
}

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
  async (id) => {
    const currentVersion = ++prescriptLoadVersion
    const loadDraftVersion = prescriptDraftVersion
    if (!id) {
      Object.assign(currentRequest, getDefaultRequest())
      assertionResults.value = []
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
      currentRequest.prescripts = []
      currentRequest.assertions = node.assertions || []
      assertionResults.value = []

      try {
        const loadedPrescripts = await loadPrescriptsForCase(id)
        if (
          currentVersion === prescriptLoadVersion
          && loadDraftVersion === prescriptDraftVersion
          && String(apiStore.selectedCaseId) === String(id)
        ) {
          currentRequest.prescripts = loadedPrescripts
        }
      } catch (err) {
        console.error('加载前置步骤失败:', err)
        if (
          currentVersion === prescriptLoadVersion
          && loadDraftVersion === prescriptDraftVersion
          && String(apiStore.selectedCaseId) === String(id)
        ) {
          currentRequest.prescripts = []
        }
      }
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
    const updated = await apiStore.updateCollection(id, { name })
    if (!updated) {
      throw new Error('重命名未生效，请刷新后重试')
    }
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
  const previousPrescripts = serializePrescripts(currentRequest.prescripts)
  const nextPrescripts = serializePrescripts(req.prescripts)
  Object.assign(currentRequest, req)
  if (previousPrescripts !== nextPrescripts) {
    prescriptDraftVersion += 1
  }
  if (apiStore.selectedCaseId) {
    scheduleAutoSave(req)
  }
}

function getEnvironmentVariableMap() {
  return Object.fromEntries(
    (apiStore.currentEnvironment?.variables || [])
      .filter((variable) => variable.key)
      .map((variable) => [variable.key, variable.value ?? '']),
  )
}

function resolveRuntimeValue(input, runtimeVariables = {}) {
  if (typeof input !== 'string' || !input) return input
  const variableMap = {
    ...getEnvironmentVariableMap(),
    ...runtimeVariables,
  }
  return input.replace(/\{\{\s*([^}]+?)\s*\}\}/g, (_, rawKey) => {
    const key = String(rawKey).trim()
    return Object.prototype.hasOwnProperty.call(variableMap, key)
      ? String(variableMap[key])
      : `{{${key}}}`
  })
}

function buildUrl(requestConfig = currentRequest, runtimeVariables = {}) {
  let url = resolveRuntimeValue(requestConfig.url, runtimeVariables)
  const params = (requestConfig.params || []).filter((p) => p.enabled !== false && p.key)
  if (params.length) {
    const qs = params
      .map((p) => {
        const key = resolveRuntimeValue(p.key, runtimeVariables)
        const value = resolveRuntimeValue(p.value || '', runtimeVariables)
        return `${encodeURIComponent(key)}=${encodeURIComponent(value || '')}`
      })
      .join('&')
    url += (url.includes('?') ? '&' : '?') + qs
  }
  return url
}

function buildHeaders(requestConfig = currentRequest, runtimeVariables = {}) {
  const headers = {}
  const list = (requestConfig.headers || []).filter((h) => h.enabled !== false && h.key)
  list.forEach((h) => {
    const key = resolveRuntimeValue(h.key, runtimeVariables)
    if (!key) return
    headers[key] = resolveRuntimeValue(h.value || '', runtimeVariables)
  })

  if (requestConfig.authType === 'bearer' && requestConfig.authConfig?.token) {
    headers['Authorization'] = `Bearer ${resolveRuntimeValue(requestConfig.authConfig.token, runtimeVariables)}`
  } else if (requestConfig.authType === 'basic' && requestConfig.authConfig?.username) {
    const u = resolveRuntimeValue(requestConfig.authConfig.username, runtimeVariables)
    const p = resolveRuntimeValue(requestConfig.authConfig.password || '', runtimeVariables)
    headers['Authorization'] = 'Basic ' + btoa(`${u}:${p}`)
  } else if (requestConfig.authType === 'apikey' && requestConfig.authConfig?.key) {
    const key = resolveRuntimeValue(requestConfig.authConfig.key, runtimeVariables)
    if (key) {
      headers[key] = resolveRuntimeValue(requestConfig.authConfig.value || '', runtimeVariables)
    }
  }

  if (requestConfig.bodyType === 'raw' && requestConfig.bodyRaw) {
    if (!headers['Content-Type']) {
      headers['Content-Type'] = requestConfig.bodyRawType === 'json'
        ? 'application/json'
        : requestConfig.bodyRawType === 'xml'
          ? 'application/xml'
          : 'text/plain'
    }
  } else if (requestConfig.bodyType === 'x-www-form-urlencoded' && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded'
  }
  return headers
}

function buildBody(requestConfig = currentRequest, runtimeVariables = {}) {
  if (requestConfig.bodyType === 'none') return ''
  if (requestConfig.bodyType === 'raw') {
    return resolveRuntimeValue(requestConfig.bodyRaw || '', runtimeVariables)
  }
  const form = (requestConfig.bodyForm || []).filter((f) => f.key)
  if (requestConfig.bodyType === 'x-www-form-urlencoded') {
    return form
      .map((f) => {
        const key = resolveRuntimeValue(f.key, runtimeVariables)
        const value = resolveRuntimeValue(f.value || '', runtimeVariables)
        return `${encodeURIComponent(key)}=${encodeURIComponent(value || '')}`
      })
      .join('&')
  }
  if (requestConfig.bodyType === 'form-data') {
    return new URLSearchParams(
      form.reduce((acc, f) => {
        const key = resolveRuntimeValue(f.key, runtimeVariables)
        if (!key) return acc
        acc[key] = resolveRuntimeValue(f.value || '', runtimeVariables)
        return acc
      }, {}),
    ).toString()
  }
  return ''
}

function buildRequestSnapshot(requestConfig = currentRequest, runtimeVariables = {}) {
  const url = buildUrl(requestConfig, runtimeVariables)
  if (!url) {
    throw new Error('请输入请求 URL')
  }
  return {
    method: requestConfig.method || 'GET',
    url,
    headers: buildHeaders(requestConfig, runtimeVariables),
    body: buildBody(requestConfig, runtimeVariables),
  }
}

function normalizeResponseData(result) {
  const data = result?.data || result || {}
  return {
    status: data.status ?? 0,
    statusText: data.statusText || '',
    headers: data.headers || {},
    body: data.body ?? '',
    duration: data.duration || 0,
    size: data.size || 0,
  }
}

function normalizeErrorResponse(err) {
  return {
    status: err.response?.status || 0,
    statusText: err.response?.statusText || 'Error',
    headers: err.response?.headers || {},
    body: err.response?.data || '',
    duration: 0,
    size: 0,
    error: err.response?.data?.message || err.message || '请求失败',
  }
}

async function dispatchRequest(requestConfig = currentRequest, runtimeVariables = {}) {
  const requestSnapshot = buildRequestSnapshot(requestConfig, runtimeVariables)
  const res = await apiSendRequest({
    method: requestSnapshot.method,
    url: requestSnapshot.url,
    headers: { ...requestSnapshot.headers },
    body: requestSnapshot.body,
  })
  return {
    requestSnapshot,
    responseData: normalizeResponseData(res),
  }
}

function setRuntimeVariable(runtimeVariables, key, value) {
  const normalizedKey = String(key || '').trim()
  if (!normalizedKey) return
  runtimeVariables[normalizedKey] = value == null ? '' : String(value)
}

function extractRuntimeValue(responseData, rawPath) {
  const path = String(rawPath || '').trim()
  if (!path) return undefined
  if (path.startsWith('$.')) {
    return readJsonPath(parseJsonBody(responseData?.body), path)
  }

  const normalized = path.replace(/^response\./, '')
  const tokens = normalized
    .replace(/\[(\d+)\]/g, '.$1')
    .split('.')
    .filter(Boolean)

  if (!tokens.length) return undefined

  const rootToken = tokens[0]
  if (['status', 'statusText', 'duration', 'size', 'error'].includes(rootToken)) {
    return tokens.length === 1 ? responseData?.[rootToken] : undefined
  }

  if (rootToken === 'headers') {
    if (tokens.length === 2) {
      return findHeaderValue(responseData?.headers || {}, tokens[1])
    }
    let current = responseData?.headers || {}
    for (const token of tokens.slice(1)) {
      if (current == null || !Object.prototype.hasOwnProperty.call(current, token)) {
        return undefined
      }
      current = current[token]
    }
    return current
  }

  const bodyJson = parseJsonBody(responseData?.body)
  if (rootToken === 'body') {
    return tokens.length === 1 ? bodyJson : readJsonPath(bodyJson, `$.${tokens.slice(1).join('.')}`)
  }
  if (rootToken === 'data') {
    return readJsonPath(bodyJson, `$.${tokens.join('.')}`)
  }
  return readJsonPath(bodyJson, `$.${tokens.join('.')}`)
}

function applyExtractParams(extractParams, responseData, runtimeVariables) {
  const extractedVariables = []
  for (const item of extractParams || []) {
    const name = String(item?.name || '').trim()
    const path = String(item?.path || '').trim()
    if (!name || !path) continue
    const value = extractRuntimeValue(responseData, path)
    if (value !== undefined) {
      const normalizedValue = stringifyAssertionValue(value)
      setRuntimeVariable(runtimeVariables, name, normalizedValue)
      extractedVariables.push({
        name,
        path,
        value: normalizedValue,
      })
    }
  }
  return extractedVariables
}

function createPrescriptExecutionError(message, prescriptResults) {
  const error = new Error(message)
  error.prescriptResults = Array.isArray(prescriptResults) ? [...prescriptResults] : []
  return error
}

function getPrescriptTitle(step, index) {
  const stepType = String(step?.stepType || '').toUpperCase()
  if (stepType === 'HTTP') {
    return `${step.method || 'GET'} ${step.url || `前置HTTP步骤 #${index + 1}`}`
  }
  if (stepType === 'SET_VARIABLE') {
    return `设置变量 ${step.varName || `#${index + 1}`}`
  }
  if (stepType === 'FUNCTION') {
    return `函数调用 ${step.functionName || `#${index + 1}`}`
  }
  return `前置步骤 #${index + 1}`
}

function splitFunctionParams(functionParams) {
  const source = String(functionParams || '').trim()
  if (!source) {
    return []
  }
  return source
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

async function executeFunctionPrescript(step, runtimeVariables) {
  const functionName = String(step?.functionName || '').trim()
  if (!functionName) {
    throw new Error('函数名不能为空')
  }

  const scriptRes = await getScriptByFunctionName(functionName)
  const script = scriptRes?.data
  if (!script?.content) {
    throw new Error(`函数不存在: ${functionName}`)
  }

  const resolvedParams = splitFunctionParams(step?.functionParams).map((item) =>
    resolveRuntimeValue(item, runtimeVariables),
  )
  const executeRes = await testScript(script.content, resolvedParams)
  const result = executeRes?.data || {}
  const outputParams = result.outputParams && typeof result.outputParams === 'object'
    ? result.outputParams
    : {}

  return {
    functionName,
    params: resolvedParams,
    success: Boolean(result.success),
    output: result.output || '',
    errorMessage: result.errorMessage || '',
    outputParams,
  }
}

async function executePrescripts() {
  const runtimeVariables = {}
  const prescriptResults = []
  const steps = Array.isArray(currentRequest.prescripts) ? currentRequest.prescripts : []

  for (let index = 0; index < steps.length; index += 1) {
    const step = steps[index]
    const stepType = String(step?.stepType || '').toUpperCase()
    const stopOnFail = Boolean(step?.stopOnFail)
    const title = getPrescriptTitle(step, index)

    if (stepType === 'SET_VARIABLE') {
      const name = String(step?.varName || '').trim()
      const resolvedValue = resolveRuntimeValue(step.varValue || '', runtimeVariables)
      if (name) {
        setRuntimeVariable(runtimeVariables, name, resolvedValue)
      }
      prescriptResults.push({
        index: index + 1,
        stepType,
        title,
        status: 'SUCCESS',
        stopOnFail,
        variableName: name,
        variableValue: resolvedValue,
        message: name ? `变量 ${name} 已设置` : '已跳过空变量名',
      })
      continue
    }

    if (stepType === 'FUNCTION') {
      try {
        const functionResult = await executeFunctionPrescript(step, runtimeVariables)
        const extractedVariables = []
        Object.entries(functionResult.outputParams).forEach(([key, value]) => {
          const normalizedValue = stringifyAssertionValue(value)
          setRuntimeVariable(runtimeVariables, key, normalizedValue)
          extractedVariables.push({
            name: key,
            path: 'function.output',
            value: normalizedValue,
          })
        })

        if (step?.outputVar) {
          const outputValue = functionResult.outputParams.result ?? functionResult.output
          const normalizedValue = stringifyAssertionValue(outputValue)
          setRuntimeVariable(runtimeVariables, step.outputVar, normalizedValue)
          extractedVariables.push({
            name: step.outputVar,
            path: 'function.return',
            value: normalizedValue,
          })
        }

        const stepPassed = functionResult.success
        const message = stepPassed
          ? `函数 ${functionResult.functionName} 执行成功`
          : (functionResult.errorMessage || `函数 ${functionResult.functionName} 执行失败`)

        prescriptResults.push({
          index: index + 1,
          stepType,
          title,
          status: stepPassed ? 'SUCCESS' : 'FAILED',
          stopOnFail,
          message,
          errorMessage: stepPassed ? '' : functionResult.errorMessage,
          variableName: step.outputVar || '',
          variableValue: step.outputVar
            ? stringifyAssertionValue(functionResult.outputParams.result ?? functionResult.output)
            : '',
          extractedVariables,
          response: {
            status: stepPassed ? 200 : 500,
            statusText: stepPassed ? 'OK' : 'FUNCTION_ERROR',
            headers: {},
            body: stringifyResponseBody(functionResult.output),
            size: stringifyResponseBody(functionResult.output).length,
            duration: 0,
          },
        })

        if (stopOnFail && !stepPassed) {
          throw createPrescriptExecutionError(message, prescriptResults)
        }
      } catch (err) {
        const message = err.message || `前置步骤 #${index + 1} 函数执行失败`
        prescriptResults.push({
          index: index + 1,
          stepType,
          title,
          status: 'FAILED',
          stopOnFail,
          message,
          errorMessage: message,
          variableName: step.outputVar || '',
          variableValue: '',
        })
        if (stopOnFail) {
          throw createPrescriptExecutionError(message, prescriptResults)
        }
      }
      continue
    }

    if (stepType !== 'HTTP') {
      continue
    }

    let stepRequestSnapshot = null
    let stepResponse
    let requestRejected = false
    try {
      stepRequestSnapshot = buildRequestSnapshot(step, runtimeVariables)
      const res = await apiSendRequest({
        method: stepRequestSnapshot.method,
        url: stepRequestSnapshot.url,
        headers: { ...stepRequestSnapshot.headers },
        body: stepRequestSnapshot.body,
      })
      stepResponse = normalizeResponseData(res)
    } catch (err) {
      requestRejected = true
      stepResponse = normalizeErrorResponse(err)
      if (!stepRequestSnapshot) {
        const message = `前置步骤 #${index + 1} 请求构造失败：${stepResponse.error}`
        prescriptResults.push({
          index: index + 1,
          stepType,
          title,
          status: 'FAILED',
          stopOnFail,
          message,
          errorMessage: stepResponse.error,
        })
        if (stopOnFail) {
          throw createPrescriptExecutionError(message, prescriptResults)
        }
        continue
      }
    }

    const stepAssertionResults = evaluateAssertions(step.assertions, stepResponse)
    const extractedVariables = applyExtractParams(step.extractParams, stepResponse, runtimeVariables)
    const failedAssertion = stepAssertionResults.find((item) => item.passed === false)
    const allAssertionsPassed = stepAssertionResults.every((item) => item.passed !== false)
    const requestSucceeded = !requestRejected && stepResponse.status >= 200 && stepResponse.status < 300
    const stepPassed = stepAssertionResults.length > 0
      ? requestSucceeded && allAssertionsPassed
      : requestSucceeded
    const stepMessage = failedAssertion
      ? failedAssertion.message
      : stepResponse.error || (stepPassed ? '执行成功' : `HTTP ${stepResponse.status || '请求失败'}`)

    prescriptResults.push({
      index: index + 1,
      stepType,
      title,
      status: stepPassed ? 'SUCCESS' : 'FAILED',
      stopOnFail,
      message: stepMessage,
      errorMessage: stepPassed ? '' : (stepResponse.error || stepMessage),
      request: stepRequestSnapshot,
      response: {
        ...stepResponse,
        body: stringifyResponseBody(stepResponse?.body),
      },
      assertions: stepAssertionResults,
      extractedVariables,
    })

    if (stopOnFail && !stepPassed) {
      throw createPrescriptExecutionError(`前置步骤 #${index + 1} 执行失败：${stepMessage}`, prescriptResults)
    }
  }

  return {
    runtimeVariables,
    prescriptResults,
  }
}

async function sendRequest() {
  if (!currentRequest.url?.trim()) {
    ElMessage.warning('请输入请求 URL')
    return
  }
  if (apiStore.selectedCaseId) {
    await flushPendingSave()
  }
  sending.value = true
  response.value = null
  assertionResults.value = []
  let requestSnapshot = null
  let prescriptResults = []
  try {
    const prescriptExecution = await executePrescripts()
    prescriptResults = prescriptExecution.prescriptResults || []
    const dispatched = await dispatchRequest(currentRequest, prescriptExecution.runtimeVariables)
    requestSnapshot = dispatched.requestSnapshot
    const data = dispatched.responseData
    response.value = {
      status: data.status,
      statusText: data.statusText,
      headers: data.headers,
      body: data.body,
      duration: data.duration || 0,
      size: data.size || 0,
    }
    assertionResults.value = evaluateAssertions(currentRequest.assertions, response.value)
    apiStore.addRequestToHistory({
      id: Date.now(),
      method: currentRequest.method,
      url: requestSnapshot.url,
      status: data.status,
    })

    // 根据 HTTP 状态码判定测试结果
    // 2xx 判定为 SUCCESS，其余判定为 FAILED
    const allAssertionsPassed = assertionResults.value.every((item) => item.passed !== false)
    const testStatus = (data.status >= 200 && data.status < 300 && allAssertionsPassed) ? 'SUCCESS' : 'FAILED'
    await saveExecutionRecord(data, requestSnapshot, testStatus, {
      prescriptResults,
      errorMessage: '',
    })
  } catch (err) {
    // 捕获到网络错误或代码异常
    const errorData = normalizeErrorResponse(err)
    prescriptResults = Array.isArray(err?.prescriptResults) ? err.prescriptResults : prescriptResults

    response.value = {
      ...errorData,
      error: errorData.error,
    }
    assertionResults.value = requestSnapshot ? evaluateAssertions(currentRequest.assertions, response.value) : []
    ElMessage.error(response.value.error)
    await saveExecutionRecord(errorData, requestSnapshot, 'FAILED', {
      prescriptResults,
      errorMessage: response.value.error,
    })
  } finally {
    sending.value = false
  }
}

async function saveExecutionRecord(data, requestSnapshot, status, options = {}) {
  try {
    const currentCase = apiStore.findNodeById(apiStore.collections, apiStore.selectedCaseId)
    const prescriptResults = Array.isArray(options.prescriptResults) ? options.prescriptResults : []
    await saveApiExecution({
      collectionId: apiStore.selectedCaseId,
      collectionName: currentCase?.name || '未命名接口',
      status: status,
      errorMessage: options.errorMessage || '',
      request: requestSnapshot ? {
        method: requestSnapshot.method || currentRequest.method,
        url: requestSnapshot.url || currentRequest.url,
        headers: requestSnapshot.headers || {},
        body: requestSnapshot.body || '',
      } : null,
      response: requestSnapshot ? {
        status: data.status,
        statusText: data.statusText,
        headers: data.headers,
        body: stringifyResponseBody(data.body),
        size: data.size,
        duration: data.duration,
      } : null,
      assertions: assertionResults.value,
      prescriptResults,
    })
  } catch (err) {
    console.error('保存执行记录失败:', err)
  }
}

function evaluateAssertions(definitions, executionResponse) {
  const assertionDefinitions = Array.isArray(definitions) ? definitions : []
  if (!assertionDefinitions.length) {
    return []
  }

  return assertionDefinitions
    .filter((assertion) => assertion?.enabled !== false)
    .map((assertion) => evaluateSingleAssertion(assertion, executionResponse))
}

function evaluateSingleAssertion(assertion, executionResponse) {
  const type = String(assertion?.assertionType || assertion?.type || '').toUpperCase()
  const expression = String(assertion?.expression || '').trim()
  const expected = String(assertion?.expected || '').trim()
  const responseHeaders = executionResponse?.headers || {}
  const responseBody = executionResponse?.body
  const responseDuration = Number(executionResponse?.duration || 0)
  const responseStatus = Number(executionResponse?.status || 0)

  let actual = ''
  let passed = false
  let description = expression || type || '断言'

  switch (type) {
    case 'STATUS': {
      actual = String(responseStatus || '')
      const targetStatus = Number(expression || expected || 0)
      passed = Number.isFinite(targetStatus) && targetStatus > 0 && responseStatus === targetStatus
      description = `状态码应为 ${expression || expected || '-'}`
      break
    }
    case 'CONTAINS': {
      const target = expected || expression
      const bodyText = stringifyResponseBody(responseBody)
      actual = bodyText
      passed = Boolean(target) && bodyText.includes(target)
      description = `响应体包含 ${target || '-'}`
      break
    }
    case 'DURATION': {
      const maxDuration = Number(expected || expression || 0)
      actual = `${responseDuration}ms`
      passed = Number.isFinite(maxDuration) && maxDuration > 0 && responseDuration <= maxDuration
      description = `响应时间不超过 ${expected || expression || '-'}ms`
      break
    }
    case 'HEADERS': {
      const raw = expected || expression
      const index = raw.indexOf(':')
      const headerName = index >= 0 ? raw.slice(0, index).trim() : raw.trim()
      const headerExpected = index >= 0 ? raw.slice(index + 1).trim() : ''
      const headerActual = findHeaderValue(responseHeaders, headerName)
      actual = headerActual || '(不存在)'
      passed = Boolean(headerName) && Boolean(headerExpected) && actual === headerExpected
      description = `响应头 ${headerName || '-'} 等于 ${headerExpected || '-'}`
      break
    }
    case 'JSONPATH': {
      const bodyJson = parseJsonBody(responseBody)
      const extracted = readJsonPath(bodyJson, expression)
      actual = extracted === undefined ? '(未找到)' : stringifyAssertionValue(extracted)
      passed = compareAssertionValue(extracted, expected)
      description = `JSONPath ${expression || '-'} 校验`
      break
    }
    default: {
      actual = '当前断言类型暂未在前端执行'
      passed = false
      description = `${type || '未知'} 断言`
      break
    }
  }

  return {
    type,
    description,
    expected: expected || expression || '',
    actual,
    passed,
    pass: passed,
    message: `${description}：${passed ? '通过' : '失败'}${actual ? `，实际值 ${actual}` : ''}`,
  }
}

function stringifyResponseBody(body) {
  if (body == null) return ''
  return typeof body === 'string' ? body : JSON.stringify(body)
}

function parseJsonBody(body) {
  if (body == null || body === '') return null
  if (typeof body === 'object') return body
  try {
    return JSON.parse(body)
  } catch {
    return null
  }
}

function readJsonPath(data, path) {
  if (!data || !path) return undefined
  const normalizedPath = String(path).trim()
  if (!normalizedPath.startsWith('$.')) {
    return undefined
  }

  const tokens = normalizedPath
    .replace(/^\$\./, '')
    .replace(/\[(\d+)\]/g, '.$1')
    .split('.')
    .filter(Boolean)

  let current = data
  for (const token of tokens) {
    if (current == null || !Object.prototype.hasOwnProperty.call(current, token)) {
      return undefined
    }
    current = current[token]
  }
  return current
}

function compareAssertionValue(actualValue, expectedRaw) {
  if (actualValue === undefined) return false
  if (!expectedRaw) return true

  const actualText = stringifyAssertionValue(actualValue)
  const candidate = String(expectedRaw).trim()

  if (candidate.startsWith('!=')) {
    return actualText !== candidate.slice(2).trim()
  }
  if (candidate.startsWith('>=')) {
    return Number(actualText) >= Number(candidate.slice(2).trim())
  }
  if (candidate.startsWith('<=')) {
    return Number(actualText) <= Number(candidate.slice(2).trim())
  }
  if (candidate.startsWith('>')) {
    return Number(actualText) > Number(candidate.slice(1).trim())
  }
  if (candidate.startsWith('<')) {
    return Number(actualText) < Number(candidate.slice(1).trim())
  }
  return actualText === candidate
}

function stringifyAssertionValue(value) {
  if (value == null) return ''
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}

function serializePrescripts(prescripts) {
  try {
    return JSON.stringify(Array.isArray(prescripts) ? prescripts : [])
  } catch {
    return '[]'
  }
}

function parseJsonSafely(value, fallback) {
  if (value == null || value === '') {
    return fallback
  }
  if (typeof value !== 'string') {
    return value
  }
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

function loadPrescriptBody(bodyJson) {
  const parsed = parseJsonSafely(bodyJson, null)
  if (parsed && typeof parsed === 'object' && ('bodyType' in parsed || 'bodyRaw' in parsed || 'bodyForm' in parsed)) {
    return {
      bodyType: parsed.bodyType || 'none',
      bodyRaw: parsed.bodyRaw || '',
      bodyRawType: parsed.bodyRawType || 'json',
      bodyForm: Array.isArray(parsed.bodyForm) ? parsed.bodyForm : [],
    }
  }
  if (typeof bodyJson === 'string' && bodyJson) {
    return {
      bodyType: 'raw',
      bodyRaw: bodyJson,
      bodyRawType: 'json',
      bodyForm: [],
    }
  }
  return {
    bodyType: 'none',
    bodyRaw: '',
    bodyRawType: 'json',
    bodyForm: [],
  }
}

function normalizePrescriptRecord(record) {
  const stepType = String(record?.stepType || 'HTTP').toUpperCase()
  if (stepType === 'HTTP') {
    const bodyConfig = loadPrescriptBody(record?.bodyJson)
    return {
      stepType: 'HTTP',
      method: record?.method || 'GET',
      url: record?.url || '',
      headers: Array.isArray(parseJsonSafely(record?.headersJson, [])) ? parseJsonSafely(record?.headersJson, []) : [],
      bodyType: bodyConfig.bodyType,
      bodyRaw: bodyConfig.bodyRaw,
      bodyRawType: bodyConfig.bodyRawType,
      bodyForm: bodyConfig.bodyForm,
      extractParams: Array.isArray(parseJsonSafely(record?.extractParamsJson, [])) ? parseJsonSafely(record?.extractParamsJson, []) : [],
      assertions: Array.isArray(parseJsonSafely(record?.assertionsJson, [])) ? parseJsonSafely(record?.assertionsJson, []) : [],
      stopOnFail: Boolean(record?.stopOnFail),
    }
  }

  if (stepType === 'FUNCTION') {
    const functionConfig = parseJsonSafely(record?.functionParamsJson, null)
    return {
      stepType: 'FUNCTION',
      functionName: record?.functionName || '',
      functionParams: functionConfig && typeof functionConfig === 'object'
        ? functionConfig.functionParams || ''
        : (typeof record?.functionParamsJson === 'string' ? record.functionParamsJson : ''),
      outputVar: functionConfig && typeof functionConfig === 'object' ? functionConfig.outputVar || '' : '',
      stopOnFail: Boolean(record?.stopOnFail),
    }
  }

  const variableConfig = parseJsonSafely(record?.setVariablesJson, {})
  return {
    stepType: 'SET_VARIABLE',
    varName: variableConfig?.varName || '',
    varValue: variableConfig?.varValue || '',
    stopOnFail: Boolean(record?.stopOnFail),
  }
}

async function loadPrescriptsForCase(caseId) {
  const res = await listPrescripts(Number(caseId))
  const items = Array.isArray(res?.data) ? res.data : []
  return items.map((item) => normalizePrescriptRecord(item))
}

function serializePrescriptRecord(item, index) {
  const stepType = String(item?.stepType || 'HTTP').toUpperCase()
  if (stepType === 'HTTP') {
    return {
      stepType: 'HTTP',
      method: item?.method || 'GET',
      url: item?.url || '',
      headersJson: JSON.stringify(Array.isArray(item?.headers) ? item.headers : []),
      bodyJson: JSON.stringify({
        bodyType: item?.bodyType || 'none',
        bodyRaw: item?.bodyRaw || '',
        bodyRawType: item?.bodyRawType || 'json',
        bodyForm: Array.isArray(item?.bodyForm) ? item.bodyForm : [],
      }),
      extractParamsJson: JSON.stringify(Array.isArray(item?.extractParams) ? item.extractParams : []),
      assertionsJson: JSON.stringify(Array.isArray(item?.assertions) ? item.assertions : []),
      functionName: null,
      functionParamsJson: null,
      setVariablesJson: null,
      stopOnFail: Boolean(item?.stopOnFail),
      sortOrder: index,
    }
  }

  if (stepType === 'FUNCTION') {
    return {
      stepType: 'FUNCTION',
      method: null,
      url: null,
      headersJson: null,
      bodyJson: null,
      extractParamsJson: null,
      assertionsJson: null,
      functionName: item?.functionName || '',
      functionParamsJson: JSON.stringify({
        functionParams: item?.functionParams || '',
        outputVar: item?.outputVar || '',
      }),
      setVariablesJson: null,
      stopOnFail: Boolean(item?.stopOnFail),
      sortOrder: index,
    }
  }

  return {
    stepType: 'SET_VARIABLE',
    method: null,
    url: null,
    headersJson: null,
    bodyJson: null,
    extractParamsJson: null,
    assertionsJson: null,
    functionName: null,
    functionParamsJson: null,
    setVariablesJson: JSON.stringify({
      varName: item?.varName || '',
      varValue: item?.varValue || '',
    }),
    stopOnFail: Boolean(item?.stopOnFail),
    sortOrder: index,
  }
}

async function savePrescriptsForCase(caseId, prescripts) {
  await savePrescriptsApi(
    Number(caseId),
    (Array.isArray(prescripts) ? prescripts : []).map((item, index) => serializePrescriptRecord(item, index)),
  )
}

function findHeaderValue(headers, targetName) {
  if (!targetName || !headers || typeof headers !== 'object') {
    return ''
  }
  const matchedKey = Object.keys(headers).find(key => key.toLowerCase() === targetName.toLowerCase())
  return matchedKey ? headers[matchedKey] : ''
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
  await flushPendingSave()
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
  clearAutoSaveTimer()
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
