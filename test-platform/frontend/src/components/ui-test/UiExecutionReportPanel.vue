<template>
  <div class="report-panel" v-if="detail">

    <!-- 执行中：醒目的进行状态横幅 -->
    <div v-if="isRunning" class="running-banner">
      <div class="running-spinner" />
      <div class="running-text">
        <span class="running-title">正在执行 UI 自动化测试…</span>
        <span class="running-sub">浏览器正在运行测试步骤，请稍候</span>
      </div>
    </div>

    <div class="report-summary">
      <div class="summary-row">
        <div class="summary-main">
          <span class="summary-label">执行状态</span>
          <el-tag :type="statusTagType" size="large" effect="dark" round>
            {{ statusLabel }}
          </el-tag>
        </div>
        <div class="summary-stats">
          <div class="stat">
            <span class="stat-num pass">{{ passedCount }}</span>
            <span class="stat-label">通过</span>
          </div>
          <div class="stat stat-fail">
            <span class="stat-num fail">{{ failedCount }}</span>
            <span class="stat-label">失败</span>
          </div>
          <div class="stat">
            <span class="stat-num">{{ totalCount }}</span>
            <span class="stat-label">步骤</span>
          </div>
        </div>
      </div>
      <div class="summary-meta">
        <span v-if="detail.startTime != null">开始：{{ formatTime(detail.startTime) }}</span>
        <span v-if="detail.endTime != null">结束：{{ formatTime(detail.endTime) }}</span>
        <span v-if="durationMs != null">耗时：<strong>{{ formatDuration(durationMs) }}</strong></span>
      </div>
      <div v-if="summaryError" class="summary-error">
        <div class="error-icon">⚠️</div>
        <div class="error-summary">{{ summaryError }}</div>
      </div>
    </div>

    <div class="report-steps-title">步骤明细</div>
    <div class="report-steps">
      <div
        v-for="s in detail.steps"
        :key="s.index"
        class="step-card"
        :class="stepCardClass(s.status)"
      >
        <div class="step-head" @click="toggleExpand(s.index)" style="cursor: pointer;">
          <span class="step-index">{{ s.index }}</span>
          <div class="step-head-main">
            <span class="step-type">{{ getStepTypeLabel(s.stepType) }}</span>
            <span class="step-action">{{ getActionLabel(s.action) }}</span>
          </div>
          <el-tag size="small" :type="stepStatusTagType(s.status)" effect="dark">
            {{ getStatusLabel(s.status) }}
          </el-tag>
          <span class="expand-icon" :class="{ expanded: expandedSteps.has(s.index) }">▼</span>
        </div>

        <!-- 简洁的摘要信息 -->
        <div v-if="s.status === 'PASSED'" class="step-success-summary">
          ✅ {{ s.logText || '执行成功' }}
        </div>

        <div v-if="s.status === 'FAILED'" class="step-fail-summary">
          <div class="fail-reason">
            <span class="fail-icon">❌</span>
            <span>{{ parseErrorReason(s.errorMessage) }}</span>
          </div>
        </div>

        <!-- 可展开的详细信息 -->
        <div v-if="isStepExpanded(s.index)" class="step-detail">
          <div v-if="s.logText" class="detail-section">
            <div class="detail-label">📝 执行日志</div>
            <pre class="detail-content log-content">{{ s.logText }}</pre>
          </div>

          <div v-if="s.errorMessage" class="detail-section">
            <div class="detail-label">🔍 错误详情</div>
            <pre class="detail-content error-content">{{ formatErrorDetail(s.errorMessage) }}</pre>
          </div>

          <div v-if="s.rawStepJson" class="detail-section">
            <div class="detail-label">📋 步骤参数</div>
            <pre class="detail-content">{{ formatStepParams(s.rawStepJson) }}</pre>
          </div>
        </div>

        <div v-if="s.screenshotUrl && isStepExpanded(s.index) && s.status !== 'PENDING' && s.status !== 'RUNNING'" class="step-shot">
          <div class="shot-label">{{ shotLabel(s.status) }}</div>
          <el-image
            :src="absoluteShotUrl(s.screenshotUrl)"
            fit="contain"
            class="shot-img"
            :preview-src-list="[absoluteShotUrl(s.screenshotUrl)]"
            preview-teleported
          >
            <template #error>
              <div class="shot-error">
                <el-icon><Picture /></el-icon>
                <span>未捕获到快照</span>
              </div>
            </template>
          </el-image>
        </div>
      </div>

      <div v-if="!detail.steps?.length && isRunning" class="running-steps-placeholder">
        <div class="running-spinner running-spinner-sm" />
        <span>等待步骤执行结果…</span>
      </div>
      <el-empty v-else-if="!detail.steps?.length" description="暂无步骤记录" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Picture } from '@element-plus/icons-vue'

const props = defineProps({
  detail: { type: Object, default: null },
})

const expandedSteps = ref(new Set())

function isStepExpanded(index) {
  // 兼容数字和字符串类型的 index
  return expandedSteps.value.has(index) || expandedSteps.value.has(String(index))
}

function toggleExpand(index) {
  if (expandedSteps.value.has(index)) {
    expandedSteps.value.delete(index)
  } else {
    expandedSteps.value.add(index)
  }
}

function absoluteShotUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  const envBase =
    typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL
      ? String(import.meta.env.VITE_API_BASE_URL).replace(/\/+$/, '')
      : ''
  if (envBase) {
    return `${envBase}${url}`
  }
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return origin ? `${origin}${url}` : url
}

function shotLabel(status) {
  if (status === 'FAILED' || status === 'SKIPPED') return '📸 失败时截图'
  return '📸 步骤截图'
}

const isRunning = computed(() =>
  props.detail?.status === 'PENDING' || props.detail?.status === 'RUNNING',
)

const statusLabel = computed(() => {
  const s = props.detail?.status
  const map = {
    PASSED: '全部通过',
    FAILED: '执行失败',
    RUNNING: '执行中',
    PENDING: '等待中',
    STOPPED: '已停止',
  }
  return map[s] || s || '-'
})

const statusTagType = computed(() => {
  const s = props.detail?.status
  if (s === 'PASSED') return 'success'
  if (s === 'FAILED' || s === 'STOPPED') return 'danger'
  return 'info'
})

const passedCount = computed(() =>
  (props.detail?.steps || []).filter((x) => x.status === 'PASSED').length,
)

const failedCount = computed(() =>
  (props.detail?.steps || []).filter((x) => x.status === 'FAILED' || x.status === 'SKIPPED').length,
)

const totalCount = computed(() =>
  (props.detail?.steps || []).length,
)

const durationMs = computed(() => {
  const d = props.detail
  if (!d?.startTime || !d?.endTime) return null
  const a = new Date(d.startTime).getTime()
  const b = new Date(d.endTime).getTime()
  if (Number.isNaN(a) || Number.isNaN(b)) return null
  return b - a
})

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return String(iso)
  return d.toLocaleString('zh-CN', { hour12: false })
}

function formatDuration(ms) {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}秒`
  return `${(ms / 60000).toFixed(1)}分钟`
}

const summaryError = computed(() => {
  if (props.detail?.status !== 'FAILED') return null
  const failedSteps = (props.detail?.steps || []).filter(s => s.status === 'FAILED')
  if (failedSteps.length === 1) {
    return parseErrorReason(failedSteps[0].errorMessage)
  }
  return `共 ${failedSteps.length} 个步骤执行失败`
})

function stepStatusTagType(status) {
  if (status === 'PASSED') return 'success'
  if (status === 'FAILED' || status === 'SKIPPED') return 'danger'
  return 'info'
}

function stepCardClass(status) {
  if (status === 'PASSED') return 'is-pass'
  if (status === 'FAILED' || status === 'SKIPPED') return 'is-fail'
  return 'is-pending'
}

function getStepTypeLabel(type) {
  const map = {
    browser: '浏览器操作',
    element: '元素操作',
    wait: '等待操作',
    assert: '断言验证',
    ai: 'AI 智能',
  }
  return map[type] || type || '-'
}

function getActionLabel(action) {
  const map = {
    openPage: '打开网页',
    refreshPage: '刷新页面',
    goBack: '后退',
    goForward: '前进',
    closeWindow: '关闭窗口',
    clickElement: '点击元素',
    inputText: '输入文本',
    clearText: '清空文本',
    getText: '获取文本',
    selectOption: '下拉选择',
    sleep: '强制等待',
    waitVisible: '等待可见',
    waitClickable: '等待可点击',
    waitDisappear: '等待消失',
    assertTitle: '断言标题',
    assertUrl: '断言 URL',
    assertTextContains: '断言文本',
    assertElementExist: '断言元素存在',
    assertElementVisible: '断言元素可见',
    aiNaturalLanguage: 'AI 指令',
    aiImageClick: 'AI 图像点击',
  }
  return map[action] || action || '-'
}

function getStatusLabel(status) {
  const map = {
    PASSED: '通过',
    FAILED: '失败',
    SKIPPED: '跳过',
    RUNNING: '进行中',
  }
  return map[status] || status || '-'
}

function parseErrorReason(errorMsg) {
  if (!errorMsg) return '未知错误'

  if (errorMsg.includes('no such element')) {
    return '元素未找到：请检查页面是否加载完成，或定位表达式是否正确'
  }
  if (errorMsg.includes('element not interactable')) {
    return '元素不可操作：元素可能被遮挡或当前不可交互'
  }
  if (errorMsg.includes('StaleElementReferenceException')) {
    return '元素已过期：页面可能已刷新，请重新定位元素'
  }
  if (errorMsg.includes('TimeoutException')) {
    return '操作超时：等待元素或页面加载超时'
  }
  if (errorMsg.includes('NoSuchWindowException')) {
    return '窗口不存在：浏览器窗口可能已关闭'
  }
  if (errorMsg.includes('UnexpectedAlertPresentException')) {
    return '意外弹窗：页面上出现了弹窗，需要先处理'
  }
  if (errorMsg.includes('assertion failed') || errorMsg.includes('AssertionError')) {
    const match = errorMsg.match(/断言[失败]*[：:](.+)/i)
    return match ? `断言失败：${match[1]}` : '断言验证失败'
  }

  const shortMsg = errorMsg.split('\n')[0]
  return shortMsg.length > 50 ? shortMsg.substring(0, 50) + '...' : shortMsg
}

function formatErrorDetail(errorMsg) {
  if (!errorMsg) return ''

  const lines = errorMsg.split('\n')
  const result = []

  for (const line of lines) {
    if (line.includes('Selenium') || line.includes('at org.openqa')) {
      result.push(`    ${line}`)
    } else if (line.includes('Session info:') || line.includes('Build info:')) {
      continue
    } else {
      result.push(line)
    }
  }

  return result.slice(0, 10).join('\n')
}

function formatStepParams(rawJson) {
  try {
    const params = JSON.parse(rawJson)
    const stepType = params.type || ''
    const action = params.action || ''
    const description = params.description || ''

    let result = `类型：${getStepTypeLabel(stepType)}\n动作：${getActionLabel(action)}`
    if (description) {
      result += `\n描述：${description}`
    }

    const paramPairs = Object.entries(params.parameters || {})
      .filter(([k]) => !['locatorValue', 'locatorType'].includes(k))
      .map(([k, v]) => `${k}: ${v}`)
    if (paramPairs.length) {
      result += `\n参数：\n  ${paramPairs.join('\n  ')}`
    }

    if (params.parameters?.locatorValue) {
      result += `\n\n定位表达式：\n  ${params.parameters.locatorType}: ${params.parameters.locatorValue}`
    }

    return result
  } catch {
    return rawJson || ''
  }
}
</script>

<style scoped>
.report-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  max-height: 70vh;
  overflow-y: auto;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.9) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.running-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%);
  border-radius: 12px;
  border: 1px solid #7dd3fc;
}

.running-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.running-title {
  font-weight: 600;
  color: #0369a1;
}

.running-sub {
  font-size: 13px;
  color: #0891b2;
}

.report-summary {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  padding: 16px 20px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.summary-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-label {
  font-size: 14px;
  color: #64748b;
}

.summary-stats {
  display: flex;
  gap: 24px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 48px;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-num.pass {
  color: #16a34a;
}

.stat-num.fail {
  color: #dc2626;
}

.stat-label {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 13px;
  color: #64748b;
  margin-top: 12px;
}

.summary-meta strong {
  color: #0f172a;
  font-weight: 600;
}

.summary-error {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 14px;
  padding: 14px 16px;
  background: #fef2f2;
  border-radius: 10px;
  border: 1px solid #fecaca;
}

.error-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.error-summary {
  color: #991b1b;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
}

.report-steps-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  margin: 8px 0 6px;
  padding-left: 2px;
}

.report-steps {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.step-card {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: rgba(255, 255, 255, 0.96);
  border-left: 4px solid #94a3b8;
  transition: var(--transition);
}

.step-card.is-pass {
  border-left-color: #22c55e;
  background: linear-gradient(to right, #f0fdf4 0%, #ffffff 22px);
}

.step-card.is-fail {
  border-left-color: #ef4444;
  background: linear-gradient(to right, #fef2f2 0%, #ffffff 22px);
}

.step-card.is-pending {
  border-left-color: #64748b;
}

.step-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.step-index {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e2e8f0;
  color: #475569;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
}

.step-head-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.step-type {
  font-size: 12px;
  color: #64748b;
}

.step-action {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.expand-icon {
  font-size: 10px;
  color: #94a3b8;
  transition: transform 0.2s;
  flex-shrink: 0;
  align-self: center;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.step-success-summary {
  margin-top: 10px;
  padding: 10px 14px;
  font-size: 13px;
  color: #166534;
  background: #dcfce7;
  border-radius: 8px;
}

.step-fail-summary {
  margin-top: 10px;
  padding: 12px 14px;
  background: #fef2f2;
  border-radius: 8px;
}

.fail-reason {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 14px;
  color: #991b1b;
  font-weight: 500;
}

.fail-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.step-detail {
  margin-top: 12px;
  border-top: 1px dashed #e2e8f0;
  padding-top: 12px;
}

.detail-section {
  margin-bottom: 14px;
}

.detail-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 6px;
}

.detail-content {
  font-size: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-x: auto;
  margin: 0;
  font-family: ui-monospace, Consolas, 'Courier New', monospace;
}

.log-content {
  background: #f8fafc;
  color: #334155;
  border: 1px solid #e2e8f0;
}

.error-content {
  background: #1e1e1e;
  color: #e5e5e5;
  border: 1px solid #333;
}

.step-shot {
  margin-top: 12px;
  border-top: 1px dashed #e2e8f0;
  padding-top: 10px;
}

.shot-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.shot-img {
  max-width: 100%;
  max-height: 280px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  cursor: zoom-in;
}

.shot-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 200px;
  background-color: #f8fafc;
  border-radius: 8px;
  color: #94a3b8;
  font-size: 14px;
  gap: 8px;
}

.shot-error .el-icon {
  font-size: 32px;
}

.running-steps-placeholder {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px;
  color: #64748b;
  font-size: 14px;
}

.running-spinner {
  width: 36px;
  height: 36px;
  border: 4px solid #e2e8f0;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.running-spinner-sm {
  width: 20px;
  height: 20px;
  border-width: 3px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
