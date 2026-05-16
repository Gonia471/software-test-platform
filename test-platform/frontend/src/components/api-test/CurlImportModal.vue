<template>
  <el-dialog
    v-model="dialogVisible"
    :title="mode === 'import' ? '导入 CURL 命令' : '导出 CURL 命令'"
    width="800px"
    :close-on-click-modal="false"
    append-to-body
    class="curl-dialog"
  >
    <div class="curl-modal">
      <div v-if="mode === 'import'" class="import-section">
        <el-alert
          title="粘贴 CURL 命令"
          type="info"
          :closable="false"
          class="mb-16"
        >
          <template #default>
            支持常见的 curl 命令格式，包括：
            <br />• GET/POST/PUT/DELETE/PATCH 请求
            <br />• -H/--header 设置请求头
            <br />• -d/--data/--data-raw 设置请求体
            <br />• -u/--user 设置 Basic Auth
            <br />• Bearer Token 认证
          </template>
        </el-alert>

        <el-input
          v-model="curlCommand"
          type="textarea"
          :rows="8"
          placeholder="例如：curl -X POST 'https://api.example.com/users' -H 'Content-Type: application/json' -d '{&quot;name&quot;:&quot;张三&quot;}'"
          class="curl-input"
          @keydown.ctrl.enter="handleImport"
          @keydown.meta.enter="handleImport"
        />

        <div v-if="previewData && parseSuccess" class="preview-section">
          <div class="preview-title">
            <el-icon color="#22c55e"><Check /></el-icon>
            解析成功，预览：
          </div>
          <div class="preview-content">
            <div class="preview-item">
              <span class="preview-label">方法：</span>
              <el-tag :type="methodTagType(previewData.method)" size="small">
                {{ previewData.method }}
              </el-tag>
            </div>
            <div class="preview-item">
              <span class="preview-label">URL：</span>
              <span class="preview-value url-text">{{ previewData.url }}</span>
            </div>
            <div v-if="previewData.headers.length" class="preview-item">
              <span class="preview-label">Headers：</span>
              <div class="preview-list">
                <div v-for="(header, idx) in previewData.headers.slice(0, 3)" :key="idx" class="preview-list-item">
                  {{ header.key }}: {{ header.value }}
                </div>
                <div v-if="previewData.headers.length > 3" class="preview-list-more">
                  还有 {{ previewData.headers.length - 3 }} 个 header...
                </div>
              </div>
            </div>
            <div v-if="previewData.bodyRaw" class="preview-item">
              <span class="preview-label">Body：</span>
              <pre class="preview-body">{{ previewData.bodyRaw.slice(0, 200) }}{{ previewData.bodyRaw.length > 200 ? '...' : '' }}</pre>
            </div>
            <div v-if="previewData.authType !== 'none'" class="preview-item">
              <span class="preview-label">认证：</span>
              <el-tag size="small">{{ previewData.authType }}</el-tag>
            </div>
          </div>
        </div>

        <div v-if="parseError" class="error-section">
          <el-icon color="#ef4444"><WarningFilled /></el-icon>
          {{ parseError }}
        </div>
      </div>

      <div v-else class="export-section">
        <el-alert
          title="CURL 命令已生成"
          type="success"
          :closable="false"
          class="mb-16"
        >
          <template #default>
            可以直接复制以下命令到终端使用，或复制到其他 API 测试工具中
          </template>
        </el-alert>

        <div class="curl-output">
          <el-input
            v-model="curlCommand"
            type="textarea"
            :rows="10"
            readonly
            class="curl-code"
          />
          <div class="curl-actions">
            <el-button type="primary" size="small" @click="copyCurl">
              <el-icon><CopyDocument /></el-icon>
              复制 CURL
            </el-button>
            <el-button size="small" @click="copyFormatted">
              <el-icon><Document /></el-icon>
              复制格式化版本
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">关闭</el-button>
        <el-button v-if="mode === 'import'" @click="handleClear">清空</el-button>
        <el-button v-if="mode === 'import'" type="primary" :disabled="!curlCommand.trim() || !parseSuccess" :loading="importing" @click="handleImport">
          导入
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, WarningFilled, CopyDocument, Document } from '@element-plus/icons-vue'
import { curlToRequest, requestToCurl } from '../../utils/CurlParser'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  mode: {
    type: String,
    default: 'import',
    validator: (value) => ['import', 'export'].includes(value)
  },
  request: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'import', 'export'])

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const curlCommand = ref('')
const previewData = ref(null)
const parseSuccess = ref(false)
const parseError = ref('')
const importing = ref(false)

// 监听模式变化
watch(() => props.mode, (newMode) => {
  if (newMode === 'export' && props.request) {
    curlCommand.value = requestToCurl(props.request)
    parseSuccess.value = true
  }
}, { immediate: true })

// 监听请求变化（导出模式）
watch(() => props.request, (newRequest) => {
  if (props.mode === 'export' && newRequest) {
    curlCommand.value = requestToCurl(newRequest)
    parseSuccess.value = true
  }
}, { deep: true })

// 导入模式下的解析
watch(curlCommand, (newVal) => {
  if (props.mode !== 'import') return

  if (!newVal.trim()) {
    previewData.value = null
    parseSuccess.value = false
    parseError.value = ''
    return
  }

  const result = curlToRequest(newVal)
  if (result.success) {
    previewData.value = result.data
    parseSuccess.value = true
    parseError.value = ''
  } else {
    previewData.value = null
    parseSuccess.value = false
    parseError.value = result.error
  }
})

function methodTagType(method) {
  const types = {
    GET: '',
    POST: 'success',
    PUT: 'warning',
    DELETE: 'danger',
    PATCH: 'info',
    HEAD: 'info',
    OPTIONS: 'info',
  }
  return types[method] || ''
}

function handleImport() {
  if (!curlCommand.value.trim()) {
    ElMessage.warning('请输入 curl 命令')
    return
  }

  if (!parseSuccess.value) {
    ElMessage.error('curl 命令格式错误，请检查')
    return
  }

  importing.value = true

  setTimeout(() => {
    emit('import', { ...previewData.value })
    ElMessage.success('成功导入 CURL 命令')
    handleCancel()
    importing.value = false
  }, 300)
}

function handleCancel() {
  dialogVisible.value = false
  curlCommand.value = ''
  previewData.value = null
  parseSuccess.value = false
  parseError.value = ''
}

function handleClear() {
  curlCommand.value = ''
  previewData.value = null
  parseSuccess.value = false
  parseError.value = ''
}

function copyCurl() {
  navigator.clipboard.writeText(curlCommand.value).then(() => {
    ElMessage.success('CURL 命令已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function copyFormatted() {
  const formatted = curlCommand.value.replace(/ \\\n/g, ' \\\n  ')
  navigator.clipboard.writeText(formatted).then(() => {
    ElMessage.success('格式化版本已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}
</script>

<style scoped>
.curl-modal {
  padding: 4px 2px;
}

.mb-16 {
  margin-bottom: 16px;
}

.curl-input :deep(.el-textarea__inner),
.curl-code :deep(.el-textarea__inner) {
  font-family: 'Courier New', Consolas, Monaco, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.curl-code :deep(.el-textarea__inner) {
  background: #f8fbff;
}

.preview-section {
  margin-top: 16px;
  padding: 16px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 16px;
}

.preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #166534;
  margin-bottom: 12px;
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
}

.preview-label {
  color: #64748b;
  min-width: 60px;
  flex-shrink: 0;
}

.preview-value {
  color: #1e293b;
  word-break: break-all;
}

.url-text {
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.preview-list {
  flex: 1;
}

.preview-list-item {
  color: #1e293b;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  padding: 2px 0;
  border-bottom: 1px solid #d1fae5;
}

.preview-list-item:last-child {
  border-bottom: none;
}

.preview-list-more {
  color: #64748b;
  font-size: 12px;
  padding: 4px 0;
  font-style: italic;
}

.preview-body {
  flex: 1;
  margin: 0;
  padding: 12px;
  background: #fff;
  border: 1px solid #d1fae5;
  border-radius: 12px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 120px;
}

.error-section {
  margin-top: 16px;
  padding: 14px 16px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 16px;
  color: #dc2626;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.curl-output {
  margin-top: 8px;
}

.curl-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:deep(.el-alert__title) {
  font-size: 14px;
  line-height: 1.8;
}

:deep(.curl-dialog .el-dialog) {
  border-radius: 22px;
  overflow: hidden;
}

:deep(.curl-dialog .el-dialog__header) {
  padding: 22px 24px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(135deg, #fbfdff 0%, #f3f8ff 100%);
}

:deep(.curl-dialog .el-dialog__body) {
  padding: 20px 24px 24px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.92) 0%, rgba(255, 255, 255, 0.98) 100%);
}
</style>
