<template>
  <div class="api-request-editor">
    <div class="request-url-row">
      <el-select
        v-model="request.method"
        class="method-select"
        size="default"
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
        :icon="Promotion"
        @click="$emit('send')"
      >
        发送
      </el-button>
    </div>

    <el-tabs v-model="activeTab" class="request-tabs">
      <el-tab-pane label="Params" name="params">
        <KeyValueEditor
          v-model="request.params"
          :add-label="'添加参数'"
          @change="saveRequest"
        />
      </el-tab-pane>
      <el-tab-pane label="Headers" name="headers">
        <KeyValueEditor
          v-model="request.headers"
          :add-label="'添加 Header'"
          :key-placeholder="'Header 名称'"
          :value-placeholder="'值'"
          @change="saveRequest"
        />
      </el-tab-pane>
      <el-tab-pane label="Body" name="body">
        <div class="body-editor">
          <el-radio-group v-model="request.bodyType" size="small" @change="saveRequest">
            <el-radio-button value="none">none</el-radio-button>
            <el-radio-button value="form-data">form-data</el-radio-button>
            <el-radio-button value="x-www-form-urlencoded">x-www-form-urlencoded</el-radio-button>
            <el-radio-button value="raw">raw</el-radio-button>
          </el-radio-group>
          <div v-if="request.bodyType === 'raw'" class="raw-body">
            <el-select v-model="request.bodyRawType" size="small" class="raw-type">
              <el-option label="JSON" value="json" />
              <el-option label="XML" value="xml" />
              <el-option label="Text" value="text" />
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
      <el-tab-pane label="Auth" name="auth">
        <div class="auth-editor">
          <el-select v-model="request.authType" size="small" @change="saveRequest" class="auth-type-select">
            <el-option label="No Auth" value="none" />
            <el-option label="Bearer Token" value="bearer" />
            <el-option label="Basic Auth" value="basic" />
            <el-option label="API Key" value="apikey" />
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
    </el-tabs>

    <div class="request-history" v-if="history.length">
      <div class="history-title">最近请求</div>
      <div class="history-list">
        <div
          v-for="h in history.slice(0, 8)"
          :key="h.id"
          class="history-item"
          @click="applyHistory(h)"
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
import { Promotion } from '@element-plus/icons-vue'
import KeyValueEditor from './KeyValueEditor.vue'

const props = defineProps({
  request: { type: Object, required: true },
  sending: { type: Boolean, default: false },
  history: { type: Array, default: () => [] },
  httpMethods: { type: Array, default: () => [] },
})

const emit = defineEmits(['send', 'update-request', 'apply-history'])

const activeTab = ref('params')

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
  emit('update-request', { ...props.request })
}

function queryUrlSuggestions(queryString, cb) {
  const urls = [...new Set(props.history.map((h) => h.url).filter(Boolean))]
  const results = queryString
    ? urls.filter((u) => u.toLowerCase().includes(queryString.toLowerCase())).map((u) => ({ value: u }))
    : urls.slice(0, 10).map((u) => ({ value: u }))
  cb(results)
}

function applyHistory(h) {
  emit('apply-history', h)
}
</script>

<style scoped>
.api-request-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}

.request-url-row {
  display: flex;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  align-items: center;
}

.method-select {
  width: 120px;
}

.url-input {
  flex: 1;
}

.request-tabs {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Tab 头部：左右间距合理 */
.request-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 20px;
  background: #fafafa;
  border-bottom: 1px solid #e5e7eb;
}

.request-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0;
}

.request-tabs :deep(.el-tabs__item) {
  padding: 0 20px;
  height: 44px;
  line-height: 44px;
  font-size: 13px;
  font-weight: 500;
}

.request-tabs :deep(.el-tabs__item + .el-tabs__item) {
  margin-left: 4px;
}

.request-tabs :deep(.el-tabs__item.is-active) {
  color: #4f46e5;
}

.request-tabs :deep(.el-tabs__active-bar) {
  background-color: #4f46e5;
}

.request-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: auto;
  padding: 0;
}

.request-tabs :deep(.el-tabs__panel) {
  padding: 16px 20px;
}

.body-editor {
  padding: 0;
}

.body-editor :deep(.el-radio-group) {
  display: flex;
  gap: 4px;
}

.raw-body {
  margin-top: 16px;
}

.raw-body :deep(.el-textarea__inner) {
  font-family: 'Consolas', 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  border-radius: 6px;
}

.raw-type {
  margin-bottom: 10px;
  width: 100px;
}

.auth-editor {
  padding: 0;
}

.auth-editor .auth-type-select {
  display: block;
}

.auth-editor .auth-input {
  margin-top: 12px;
  display: block;
}

.request-history {
  padding: 16px 20px;
  border-top: 1px solid #f3f4f6;
  background: #fafafa;
}

.history-title {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 10px;
  font-weight: 500;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-item {
  display: flex;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.15s ease;
}

.history-item:hover {
  background: #f3f4f6;
}

.hist-method {
  font-weight: 600;
  min-width: 52px;
}

.hist-url {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #6b7280;
}
</style>
