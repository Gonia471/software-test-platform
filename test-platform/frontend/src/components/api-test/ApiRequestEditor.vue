<template>
  <div class="api-request-editor">
    <div class="request-url-row">
      <el-select
        v-model="request.method"
        class="method-select"
        @change="saveRequest"
      >
        <el-option
          v-for="m in (httpMethods || [])"
          :key="m"
          :label="m"
          :value="m"
        >
          <span :style="{ color: methodColors[m] }">{{ m }}</span>
        </el-option>
      </el-select>
      <el-autocomplete
        v-model="request.url"
        :fetch-suggestions="queryUrlSuggestions"
        placeholder="输入请求 URL，支持 {{变量}}"
        class="url-input"
        @input="saveRequest"
        @change="saveRequest"
      />
      <el-button
        type="primary"
        :loading="sending"
        :disabled="!request.url?.trim()"
        class="send-btn"
        @click="$emit('send')"
      >
        <span v-if="!sending">发送</span>
      </el-button>
      <el-dropdown trigger="click" @command="handleCommand">
        <el-button class="more-btn">
          <el-icon><MoreFilled /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="import-curl" :icon="Upload">导入 CURL</el-dropdown-item>
            <el-dropdown-item command="export-curl" :icon="Download">导出 CURL</el-dropdown-item>
            <el-dropdown-item command="copy-url" divided :icon="Link">复制 URL</el-dropdown-item>
            <el-dropdown-item command="copy-as-fetch" :icon="Document">复制为 Fetch</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-tabs v-model="activeTab" class="request-tabs">
      <el-tab-pane label="参数" name="params">
        <KeyValueEditor
          v-model="request.params"
          :add-label="'添加参数'"
          @change="saveRequest"
        />
      </el-tab-pane>
      <el-tab-pane label="请求头" name="headers">
        <KeyValueEditor
          v-model="request.headers"
          :add-label="'添加请求头'"
          :key-placeholder="'请求头名称'"
          :value-placeholder="'值'"
          @change="saveRequest"
        />
      </el-tab-pane>
      <el-tab-pane label="请求体" name="body">
        <div class="body-editor">
          <el-radio-group v-model="request.bodyType" size="small" @change="saveRequest">
            <el-radio-button value="none">无</el-radio-button>
            <el-radio-button value="form-data">表单数据</el-radio-button>
            <el-radio-button value="x-www-form-urlencoded">URL编码</el-radio-button>
            <el-radio-button value="raw">原始数据</el-radio-button>
          </el-radio-group>
          <div v-if="request.bodyType === 'raw'" class="raw-body">
            <el-select v-model="request.bodyRawType" size="small" class="raw-type">
              <el-option label="JSON" value="json" />
              <el-option label="XML" value="xml" />
              <el-option label="纯文本" value="text" />
            </el-select>
            <el-input
              v-model="request.bodyRaw"
              type="textarea"
              :rows="10"
              placeholder='{"key": "value"}'
              font-monospace
              @input="saveRequest"
            />
          </div>
          <KeyValueEditor
            v-else-if="request.bodyType !== 'none'"
            v-model="request.bodyForm"
            :add-label="'添加字段'"
            @change="saveRequest"
          />
        </div>
      </el-tab-pane>
      <el-tab-pane label="认证" name="auth">
        <div class="auth-editor">
          <el-select v-model="request.authType" size="small" @change="saveRequest" class="auth-type-select">
            <el-option label="无认证" value="none" />
            <el-option label="Bearer 令牌" value="bearer" />
            <el-option label="基础账号" value="basic" />
            <el-option label="API 密钥" value="apikey" />
          </el-select>
          <template v-if="request.authType === 'bearer'">
            <el-input
              v-model="request.authConfig.token"
              placeholder="Token"
              size="small"
              class="auth-input"
              @input="saveRequest"
            />
          </template>
          <template v-else-if="request.authType === 'basic'">
            <el-input v-model="request.authConfig.username" placeholder="用户名" size="small" class="auth-input" @input="saveRequest" />
            <el-input v-model="request.authConfig.password" placeholder="密码" type="password" size="small" class="auth-input" @input="saveRequest" />
          </template>
          <template v-else-if="request.authType === 'apikey'">
            <el-input v-model="request.authConfig.key" placeholder="Key 名称（如 X-API-Key）" size="small" class="auth-input" @input="saveRequest" />
            <el-input v-model="request.authConfig.value" placeholder="Value" size="small" class="auth-input" @input="saveRequest" />
          </template>
        </div>
      </el-tab-pane>
      <el-tab-pane label="前置步骤" name="prescripts">
        <ApiPrescriptEditor
          v-model="request.prescripts"
          @change="saveRequest"
          @open-script-library="openScriptLibrary"
        />
      </el-tab-pane>
      <el-tab-pane label="断言" name="assertions">
        <ApiAssertionEditor
          v-model="request.assertions"
          @change="saveRequest"
        />
      </el-tab-pane>
    </el-tabs>

    <CurlImportModal
      v-model="curlImportVisible"
      :mode="'import'"
      @import="handleCurlImport"
    />

    <CurlImportModal
      v-model="curlExportVisible"
      :mode="'export'"
      :request="request"
    />

    <div class="request-history" v-if="history.length">
      <div class="history-header" @click="historyCollapsed = !historyCollapsed">
        <span class="history-title">最近请求</span>
        <el-icon class="history-toggle" :class="{ 'is-collapsed': historyCollapsed }">
          <ArrowDown />
        </el-icon>
      </div>
      <div class="history-list" v-show="!historyCollapsed">
        <div
          v-for="h in history.slice(0, 5)"
          :key="h.id"
          class="history-item"
          @click="$emit('apply-history', h)"
        >
          <span class="hist-method" :style="{ color: methodColors[h.method] }">{{ h.method }}</span>
          <span class="hist-url">{{ h.url }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { MoreFilled, Upload, Download, Link, Document } from '@element-plus/icons-vue'
import KeyValueEditor from './KeyValueEditor.vue'
import CurlImportModal from './CurlImportModal.vue'
import ApiPrescriptEditor from './ApiPrescriptEditor.vue'
import ApiAssertionEditor from './ApiAssertionEditor.vue'
import { requestToCurl } from '../../utils/CurlParser'

const props = defineProps({
  request: { type: Object, required: true },
  sending: { type: Boolean, default: false },
  history: { type: Array, default: () => [] },
  httpMethods: { type: Array, default: () => [] },
})

const emit = defineEmits(['send', 'update-request', 'apply-history', 'openScriptLibrary'])

const activeTab = ref('params')
const curlImportVisible = ref(false)
const curlExportVisible = ref(false)
const historyCollapsed = ref(true)

const methodColors = {
  GET: '#2563eb',
  POST: '#16a34a',
  PUT: '#ca8a04',
  DELETE: '#dc2626',
  PATCH: '#9333ea',
  HEAD: '#6b7280',
  OPTIONS: '#6b7280',
}

function saveRequest() {
  emit('update-request', buildRequestPayload(props.request))
}

function queryUrlSuggestions(queryString, cb) {
  const urls = [...new Set(props.history.map((h) => h.url).filter(Boolean))]
  const results = queryString
    ? urls.filter((u) => u.toLowerCase().includes(queryString.toLowerCase())).map((u) => ({ value: u }))
    : urls.slice(0, 10).map((u) => ({ value: u }))
  cb(results)
}

function handleCommand(command) {
  switch (command) {
    case 'import-curl':
      curlImportVisible.value = true
      break
    case 'export-curl':
      curlExportVisible.value = true
      break
    case 'copy-url':
      copyUrl()
      break
    case 'copy-as-fetch':
      copyAsFetch()
      break
  }
}

function openScriptLibrary() {
  emit('openScriptLibrary')
}

function handleCurlImport(parsedRequest) {
  Object.assign(props.request, parsedRequest)
  saveRequest()
  ElMessage.success('已从 CURL 导入')
}

function copyUrl() {
  let url = props.request.url || ''
  const params = (props.request.params || []).filter((p) => p.enabled !== false && p.key)
  if (params.length) {
    const qs = params.map((p) => `${encodeURIComponent(p.key)}=${encodeURIComponent(p.value || '')}`).join('&')
    url += (url.includes('?') ? '&' : '?') + qs
  }
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('URL 已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function copyAsFetch() {
  const req = props.request
  let url = req.url || ''
  const params = (req.params || []).filter((p) => p.enabled !== false && p.key)
  if (params.length) {
    const qs = params.map((p) => `${encodeURIComponent(p.key)}=${encodeURIComponent(p.value || '')}`).join('&')
    url += (url.includes('?') ? '&' : '?') + qs
  }

  const headers = {}
  ;(req.headers || []).filter((h) => h.enabled !== false && h.key).forEach((h) => {
    headers[h.key] = h.value || ''
  })

  if (req.authType === 'bearer' && req.authConfig?.token) {
    headers['Authorization'] = `Bearer ${req.authConfig.token}`
  } else if (req.authType === 'basic' && req.authConfig?.username) {
    headers['Authorization'] = 'Basic ' + btoa(`${req.authConfig.username}:${req.authConfig.password || ''}`)
  } else if (req.authType === 'apikey' && req.authConfig?.key) {
    headers[req.authConfig.key] = req.authConfig.value || ''
  }

  let body = ''
  if (req.bodyType === 'raw' && req.bodyRaw) {
    body = req.bodyRaw
  } else if ((req.bodyType === 'x-www-form-urlencoded' || req.bodyType === 'form-data') && req.bodyForm) {
    const form = req.bodyForm.filter((f) => f.key)
    body = form.map((f) => `${encodeURIComponent(f.key)}=${encodeURIComponent(f.value || '')}`).join('&')
  }

  let fetchCode = `fetch('${url}'`
  const options = []

  if (req.method !== 'GET') {
    options.push(`  method: '${req.method}'`)
  }

  if (Object.keys(headers).length > 0) {
    options.push(`  headers: ${JSON.stringify(headers, null, 2).replace(/^/gm, '  ')}`)
  }

  if (body && req.method !== 'GET') {
    options.push(`  body: ${JSON.stringify(body)}`)
  }

  if (options.length > 0) {
    fetchCode += ',\n  {\n' + options.join(',\n') + '\n  }'
  }

  fetchCode += ')'

  navigator.clipboard.writeText(fetchCode).then(() => {
    ElMessage.success('Fetch 代码已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function buildRequestPayload(source) {
  return {
    ...source,
    params: Array.isArray(source?.params) ? source.params.map((item) => ({ ...item })) : [],
    headers: Array.isArray(source?.headers) ? source.headers.map((item) => ({ ...item })) : [],
    bodyForm: Array.isArray(source?.bodyForm) ? source.bodyForm.map((item) => ({ ...item })) : [],
    authConfig: source?.authConfig ? { ...source.authConfig } : {},
    prescripts: Array.isArray(source?.prescripts) ? JSON.parse(JSON.stringify(source.prescripts)) : [],
    assertions: Array.isArray(source?.assertions) ? JSON.parse(JSON.stringify(source.assertions)) : [],
  }
}

watch(
  () => props.request.prescripts,
  (val) => {
    if (!val) {
      emit('update-request', buildRequestPayload({
        ...props.request,
        prescripts: [],
      }))
    }
  },
  { immediate: true }
)

watch(
  () => props.request.assertions,
  (val) => {
    if (!val) {
      emit('update-request', buildRequestPayload({
        ...props.request,
        assertions: [],
      }))
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.api-request-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.88) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.request-url-row {
  display: flex;
  gap: 10px;
  padding: 16px 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(135deg, #fbfdff 0%, #f3f8ff 100%);
}

.method-select {
  width: 110px;
}

.url-input {
  flex: 1;
}

.url-input :deep(.el-input__wrapper) {
  border-radius: 12px;
}

.send-btn {
  border-radius: 12px;
  height: 40px;
  padding: 0 22px;
  z-index: 10;
}

.send-btn:not(:disabled):hover {
  background: var(--primary-color);
  color: #fff;
}

.request-tabs {
  flex: 1;
  overflow: hidden;
  min-height: 0;
}

.request-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  background: rgba(255, 255, 255, 0.82);
}

.request-tabs :deep(.el-tabs__content) {
  height: calc(100% - 55px);
  min-height: 0;
  overflow: hidden;
  padding: 16px 18px;
}

.request-tabs :deep(.el-tab-pane) {
  height: 100%;
  min-height: 0;
  overflow: auto;
}

.body-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.raw-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.raw-type {
  width: 120px;
}

.raw-body :deep(textarea) {
  font-family: 'Courier New', monospace;
}

.auth-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.auth-type-select {
  width: 160px;
}

.auth-input {
  max-width: 400px;
}

.request-history {
  border-top: 1px solid rgba(226, 232, 240, 0.92);
  padding: 12px 18px 14px;
  background: rgba(255, 255, 255, 0.86);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  padding: 6px 0;
}

.history-title {
  font-size: 12px;
  color: var(--text-secondary);
}

.history-toggle {
  font-size: 12px;
  color: var(--text-secondary);
  transition: transform 0.2s;
}

.history-toggle.is-collapsed {
  transform: rotate(-90deg);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 200px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid transparent;
}

.history-item:hover {
  background: #f8fbff;
  border-color: rgba(226, 232, 240, 0.95);
}

.hist-method {
  font-size: 11px;
  font-weight: 600;
  min-width: 36px;
}

.hist-url {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
}

.more-btn {
  border-radius: 12px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-secondary);
}

.more-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

:deep(.el-dropdown-menu__item) {
  font-size: 13px;
  padding: 8px 16px;
}

:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 8px;
}
</style>
