<template>
  <div class="api-response-panel">
    <div v-if="!response" class="response-empty">
      <el-icon :size="48"><Promotion /></el-icon>
      <p>点击「发送」发起请求</p>
    </div>

    <template v-else>
      <div class="response-overview">
        <el-tag :type="statusTagType" size="large">{{ response.status }} {{ response.statusText || '' }}</el-tag>
        <span class="response-meta">耗时 {{ response.duration }}ms</span>
        <span class="response-meta">大小 {{ formatSize(response.size) }}</span>
        <el-button link size="small" @click="copyResponse">复制</el-button>
      </div>

      <el-tabs v-model="activeTab" class="response-tabs">
        <el-tab-pane label="Body" name="body">
          <div class="response-body">
            <pre v-if="response.body">{{ formattedBody }}</pre>
            <pre v-else class="empty">(空响应)</pre>
          </div>
        </el-tab-pane>
        <el-tab-pane label="Headers" name="headers">
          <div class="response-headers">
            <div
              v-for="(val, key) in (response.headers || {})"
              :key="key"
              class="header-row"
            >
              <span class="header-key">{{ key }}</span>
              <span class="header-val">{{ val }}</span>
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
import { Promotion, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  response: { type: Object, default: null },
  assertionResults: { type: Array, default: () => [] },
})

const activeTab = ref('body')

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
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}

.response-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  gap: 14px;
}

.response-empty p {
  margin: 0;
  font-size: 14px;
}

.response-overview {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.response-meta {
  font-size: 13px;
  color: #6b7280;
}

.response-tabs {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Tab 头部：与请求编辑器一致的间距 */
.response-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 20px;
  background: #fafafa;
  border-bottom: 1px solid #e5e7eb;
}

.response-tabs :deep(.el-tabs__item) {
  padding: 0 20px;
  height: 44px;
  line-height: 44px;
  font-size: 13px;
  font-weight: 500;
}

.response-tabs :deep(.el-tabs__item + .el-tabs__item) {
  margin-left: 4px;
}

.response-tabs :deep(.el-tabs__item.is-active) {
  color: #4f46e5;
}

.response-tabs :deep(.el-tabs__active-bar) {
  background-color: #4f46e5;
}

.response-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: auto;
}

.response-tabs :deep(.el-tabs__panel) {
  padding: 16px 20px;
}

.response-body {
  padding: 16px;
  overflow: auto;
  font-family: 'Consolas', 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  line-height: 1.6;
  background: #1e293b;
  color: #e2e8f0;
  border-radius: 8px;
}

.response-body pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.response-body .empty {
  color: #94a3b8;
}

.response-headers {
  padding: 0;
}

.header-row {
  display: flex;
  gap: 16px;
  padding: 10px 0;
  font-size: 13px;
  border-bottom: 1px solid #f3f4f6;
}

.header-row:last-child {
  border-bottom: none;
}

.header-key {
  font-weight: 500;
  min-width: 180px;
  color: #374151;
}

.header-val {
  color: #6b7280;
  word-break: break-all;
}

.empty-assertions {
  padding: 32px;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}

.assertion-list {
  padding: 0;
}

.assertion-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  font-size: 13px;
}

.assertion-item.pass {
  color: #16a34a;
}

.assertion-item.fail {
  color: #dc2626;
}
</style>
