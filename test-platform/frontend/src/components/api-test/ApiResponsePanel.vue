<template>
  <div class="api-response-panel">
    <div v-if="!response" class="response-empty">
      <div class="empty-illustration">
        <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="32" cy="32" r="28" stroke="#e2e8f0" stroke-width="2" stroke-dasharray="4 4"/>
          <path d="M32 20v12l8 4" stroke="#cbd5e1" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </div>
      <p class="empty-title">暂无响应数据</p>
      <p class="empty-desc">发送请求后可在此查看响应结果</p>
    </div>

    <template v-else>
      <div class="response-overview">
        <div class="overview-left">
          <el-tooltip :content="statusDescription" placement="top">
            <el-tag :type="statusTagType" size="large" class="status-tag">
              {{ response.status || '-' }} {{ chineseStatusText }}
            </el-tag>
          </el-tooltip>
          <div class="response-meta-item">
            <span class="meta-icon"><Clock /></span>
            <span>耗时 {{ response.duration || 0 }}ms</span>
          </div>
          <div class="response-meta-item">
            <span class="meta-icon"><Document /></span>
            <span>大小 {{ formatSize(response.size) }}</span>
          </div>
        </div>
        <el-button link size="small" @click="copyResponse" class="copy-btn">
          <CopyDocument /> 复制结果
        </el-button>
      </div>

      <el-tabs v-model="activeTab" class="response-tabs">
        <el-tab-pane label="响应体" name="body">
          <div class="response-body">
            <pre v-if="response.body || response.error">{{ response.error || formattedBody }}</pre>
            <div v-else class="empty-body">
              <el-icon><Document /></el-icon>
              <span>响应体为空</span>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="响应头" name="headers">
          <div class="response-headers">
            <div
              v-for="(val, key) in (response.headers || {})"
              :key="key"
              class="header-row"
            >
              <span class="header-key">{{ key }}</span>
              <span class="header-val">{{ val }}</span>
            </div>
            <div v-if="!response.headers || Object.keys(response.headers).length === 0" class="empty-headers">
              无响应头
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="断言结果" name="assertions">
          <div v-if="!assertionResults?.length" class="empty-assertions">
            暂无断言配置
          </div>
          <div v-else class="assertion-list">
            <div
              v-for="(a, i) in assertionResults"
              :key="i"
              class="assertion-item"
              :class="{ pass: a.pass, fail: !a.pass }"
            >
              <el-icon v-if="a.pass"><CircleCheck /></el-icon>
              <el-icon v-else><CircleClose /></el-icon>
              <span>{{ a.message }}</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Clock, Document, CopyDocument, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  response: { type: Object, default: null },
  assertionResults: { type: Array, default: () => [] },
})

const activeTab = ref('body')

const statusMap = {
  200: { text: '成功', desc: '请求已成功处理' },
  201: { text: '已创建', desc: '请求成功并且服务器创建了新的资源' },
  204: { text: '无内容', desc: '服务器成功处理了请求，但没有返回内容' },
  400: { text: '错误请求', desc: '服务器不理解请求的语法' },
  401: { text: '未授权', desc: '请求要求身份验证' },
  403: { text: '已禁止', desc: '服务器拒绝请求' },
  404: { text: '未找到', desc: '服务器找不到请求的资源' },
  405: { text: '方法禁用', desc: '禁用请求中指定的方法' },
  500: { text: '服务器错误', desc: '服务器内部错误' },
  502: { text: '错误网关', desc: '服务器作为网关或代理，从上游服务器收到无效响应' },
  503: { text: '服务不可用', desc: '服务器目前无法使用' },
  504: { text: '网关超时', desc: '服务器作为网关或代理，但是没有及时从上游服务器收到请求' }
}

const chineseStatusText = computed(() => {
  const s = props.response?.status
  return statusMap[s]?.text || props.response?.statusText || '未知状态'
})

const statusDescription = computed(() => {
  const s = props.response?.status
  return statusMap[s]?.desc || '无详细描述'
})

const statusTagType = computed(() => {
  const s = props.response?.status
  if (!s) return 'info'
  if (s >= 200 && s < 300) return 'success'
  if (s >= 400 && s < 500) return 'warning'
  if (s >= 500) return 'danger'
  return 'info'
})

const formattedBody = computed(() => {
  const body = props.response?.body
  if (!body) return ''
  try {
    const parsed = typeof body === 'string' ? JSON.parse(body) : body
    return JSON.stringify(parsed, null, 2)
  } catch {
    return body
  }
})

function formatSize(bytes) {
  if (bytes == null) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function copyResponse() {
  const text = formattedBody.value || ''
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}
</script>

<style scoped>
.api-response-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.9) 0%, rgba(255, 255, 255, 0.98) 100%);
  border-radius: var(--border-radius);
  overflow: hidden;
}

.response-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 12px;
}

.empty-illustration {
  margin-bottom: 8px;
  opacity: 0.7;
}

.response-empty p {
  margin: 0;
}

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.empty-desc {
  font-size: 13px;
  color: var(--text-secondary);
}

.response-overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(135deg, #fbfdff 0%, #f3f8ff 100%);
}

.overview-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-tag {
  font-weight: 600;
  font-size: 13px;
}

.response-meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-icon {
  display: inline-flex;
  color: #94a3b8;
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
}

.copy-btn:hover {
  color: var(--primary-color);
}

.response-tabs {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.response-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.84);
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
}

.response-tabs :deep(.el-tabs__item) {
  padding: 0 20px;
  height: 44px;
  line-height: 44px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.response-tabs :deep(.el-tabs__item + .el-tabs__item) {
  margin-left: 4px;
}

.response-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-color);
}

.response-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--primary-color);
}

.response-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: auto;
  padding: 16px 20px;
}

.response-body {
  min-height: 200px;
  max-height: calc(100vh - 440px);
  overflow: auto;
  font-family: 'JetBrains Mono', 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  background: #f8fbff;
  color: #334155;
  border-radius: 14px;
  padding: 16px;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.response-body pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.empty-body {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #94a3b8;
  padding: 32px;
  background: #f8fbff;
  border-radius: 14px;
  border: 1px dashed rgba(148, 163, 184, 0.28);
}

.response-headers {
  padding: 8px 0;
}

.header-row {
  display: flex;
  gap: 24px;
  padding: 12px 14px;
  font-size: 13px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  transition: var(--transition);
  border-radius: 12px;
}

.header-row:hover {
  background: #f8fbff;
}

.header-row:last-child {
  border-bottom: none;
}

.header-key {
  font-weight: 600;
  min-width: 140px;
  max-width: 200px;
  color: var(--text-primary);
  flex-shrink: 0;
}

.header-val {
  color: var(--text-secondary);
  word-break: break-all;
  line-height: 1.5;
  flex: 1;
}

.empty-headers {
  padding: 20px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
}

.empty-assertions {
  padding: 32px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.assertion-list {
  padding: 0;
}

.assertion-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  font-size: 13px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.assertion-item.pass {
  color: #16a34a;
}

.assertion-item.fail {
  color: #dc2626;
}
</style>
