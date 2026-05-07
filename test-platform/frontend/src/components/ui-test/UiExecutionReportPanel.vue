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
            <span class="stat-num">{{ passedCount }}</span>
            <span class="stat-label">通过</span>
          </div>
          <div class="stat stat-fail">
            <span class="stat-num">{{ failedCount }}</span>
            <span class="stat-label">失败</span>
          </div>
          <div class="stat">
            <span class="stat-num">{{ detail.steps?.length ?? 0 }}</span>
            <span class="stat-label">步骤</span>
          </div>
        </div>
      </div>
      <div class="summary-meta">
        <span v-if="detail.startTime != null">开始：{{ formatTime(detail.startTime) }}</span>
        <span v-if="detail.endTime != null">结束：{{ formatTime(detail.endTime) }}</span>
        <span v-if="durationMs != null">耗时：<strong>{{ durationMs }}</strong> ms</span>
      </div>
      <el-alert
        v-if="detail.errorMessage"
        type="error"
        :closable="false"
        show-icon
        class="report-alert"
        :title="detail.errorMessage"
      />
    </div>

    <div class="report-steps-title">步骤明细</div>
    <div class="report-steps">
      <div
        v-for="s in detail.steps"
        :key="s.index"
        class="step-card"
        :class="stepCardClass(s.status)"
      >
        <div class="step-head">
          <span class="step-index">{{ s.index }}</span>
          <div class="step-head-main">
            <span class="step-type">{{ s.stepType || '-' }}</span>
            <span class="step-action">{{ s.action || '-' }}</span>
          </div>
          <el-tag size="small" :type="stepStatusTagType(s.status)">{{ s.status }}</el-tag>
        </div>
        <div v-if="s.logText" class="step-log">{{ s.logText }}</div>
        <div v-if="s.errorMessage" class="step-err">{{ s.errorMessage }}</div>
        <div v-if="s.screenshotUrl" class="step-shot">
          <div class="shot-label">失败截图</div>
          <el-image
            :src="s.screenshotUrl"
            fit="contain"
            class="shot-img"
            :preview-src-list="[s.screenshotUrl]"
            preview-teleported
          />
        </div>
      </div>
      <!-- 执行中但还没有步骤完成时，显示等待提示而不是"暂无记录" -->
      <div v-if="!detail.steps?.length && isRunning" class="running-steps-placeholder">
        <div class="running-spinner running-spinner-sm" />
        <span>等待步骤执行结果…</span>
      </div>
      <el-empty v-else-if="!detail.steps?.length" description="暂无步骤记录" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  detail: { type: Object, default: null },
})

const isRunning = computed(() =>
  props.detail?.status === 'PENDING' || props.detail?.status === 'RUNNING',
)

const statusLabel = computed(() => {
  const s = props.detail?.status
  const map = {
    PASSED: '通过',
    FAILED: '失败',
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
</script>

<style scoped>
.report-panel {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 执行中横幅 */
.running-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #93c5fd;
  border-radius: 10px;
  margin-bottom: 4px;
}

.running-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.running-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d4ed8;
}

.running-sub {
  font-size: 12px;
  color: #3b82f6;
}

.running-spinner {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border: 3px solid #bfdbfe;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.running-spinner-sm {
  width: 16px;
  height: 16px;
  border-width: 2px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.running-steps-placeholder {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  color: #3b82f6;
  font-size: 13px;
  border: 1px dashed #93c5fd;
  border-radius: 8px;
  background: #eff6ff;
}

.report-summary {
  padding: 16px 18px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  margin-bottom: 8px;
}

.summary-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.summary-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-label {
  font-size: 13px;
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
  font-size: 20px;
  font-weight: 700;
  color: #16a34a;
  line-height: 1.2;
}

.stat-fail .stat-num {
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
}

.summary-meta strong {
  color: #0f172a;
  font-weight: 600;
}

.report-alert {
  margin-top: 12px;
}

.report-steps-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  margin: 12px 0 10px;
  padding-left: 2px;
}

.report-steps {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: min(52vh, 420px);
  overflow-y: auto;
  padding-right: 4px;
}

.step-card {
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-left: 4px solid #94a3b8;
}

.step-card.is-pass {
  border-left-color: #22c55e;
  background: #f0fdf4;
}

.step-card.is-fail {
  border-left-color: #ef4444;
  background: #fef2f2;
}

.step-card.is-pending {
  border-left-color: #64748b;
}

.step-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.step-index {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e2e8f0;
  color: #475569;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
}

.step-head-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.step-type {
  font-size: 12px;
  color: #64748b;
}

.step-action {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  word-break: break-word;
}

.step-log {
  margin-top: 10px;
  padding: 8px 10px;
  font-size: 12px;
  color: #475569;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 6px;
  font-family: ui-monospace, Consolas, monospace;
}

.step-err {
  margin-top: 8px;
  font-size: 12px;
  color: #b91c1c;
  line-height: 1.5;
}

.step-shot {
  margin-top: 10px;
}

.shot-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}

.shot-img {
  max-width: 100%;
  max-height: 220px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}
</style>
