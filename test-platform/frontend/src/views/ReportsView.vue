<template>
  <div class="page">
    <section class="report-hero">
      <div class="report-hero__main">
        <el-page-header content="测试报告" class="page-header" />
        <h2 class="hero-title">统一查看 UI 与 API 测试执行结果</h2>
        <p class="hero-desc">
          报告中心整合最近执行记录、响应信息、断言结果与 UI 自动化截图，方便展示完整测试闭环。
        </p>
      </div>
      <div class="report-hero__side">
        <div class="hero-stat">
          <span class="hero-stat__label">UI 记录</span>
          <strong class="hero-stat__value">{{ uiExecutions.length }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-stat__label">API 记录</span>
          <strong class="hero-stat__value">{{ apiExecutions.length }}</strong>
        </div>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="report-tabs">
      <el-tab-pane label="UI 自动化" name="ui">
        <div class="ui-report-layout">
          <el-card shadow="never" class="list-card">
            <template #header>
              <div class="card-head">
                <span>最近执行</span>
                <el-button size="small" :icon="Refresh" @click="loadUiList">刷新</el-button>
              </div>
            </template>
            <div v-loading="uiListLoading" class="list-body">
              <el-table
                :data="uiExecutions"
                stripe
                row-key="id"
                :row-class-name="uiRowClassName"
                @row-click="onUiRowClick"
                max-height="calc(100vh - 260px)"
                empty-text="暂无 UI 自动化执行记录"
              >
                <el-table-column prop="id" label="执行 ID" width="88" />
                <el-table-column prop="testCaseName" label="用例名称" min-width="160" show-overflow-tooltip />
                <el-table-column prop="projectName" label="所属项目" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">
                    <el-tag v-if="row.projectName" size="small" effect="plain">{{ row.projectName }}</el-tag>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="uiStatusType(row.status)" size="small">
                      {{ row.status === 'PASSED' ? '测试通过' : row.status === 'FAILED' ? '测试失败' : row.status === 'STOPPED' ? '已停止' : row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="创建时间" width="170">
                  <template #default="{ row }">
                    {{ formatTime(row.createdAt) }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-card>

          <el-card shadow="never" class="detail-card">
            <template #header>
              <span>报告详情</span>
            </template>
            <div v-loading="uiDetailLoading" class="detail-body">
              <UiExecutionReportPanel v-if="uiDetail" :detail="uiDetail" />
              <el-empty v-else description="请从左侧选择一条执行记录" />
            </div>
          </el-card>
        </div>
      </el-tab-pane>
      <el-tab-pane label="接口测试" name="api">
        <div class="api-report-layout">
          <el-card shadow="never" class="list-card">
            <template #header>
              <div class="card-head">
                <span>最近执行</span>
                <el-button size="small" :icon="Refresh" @click="loadApiList">刷新</el-button>
              </div>
            </template>
            <div v-loading="apiListLoading" class="list-body">
              <el-table
                :data="apiExecutions"
                stripe
                row-key="id"
                :row-class-name="apiRowClassName"
                @row-click="onApiRowClick"
                max-height="calc(100vh - 260px)"
                empty-text="暂无接口测试执行记录"
              >
                <el-table-column prop="id" label="执行 ID" width="80" />
                <el-table-column prop="collectionName" label="接口名称" min-width="180" show-overflow-tooltip />
                <el-table-column prop="projectName" label="所属合集" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">
                    <el-tag v-if="row.projectName" size="small" effect="plain">{{ row.projectName }}</el-tag>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="httpStatus" label="HTTP状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="httpStatusType(row.httpStatus)" size="small">
                      {{ row.httpStatus || '-' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="测试结果" width="100">
                  <template #default="{ row }">
                    <el-tag :type="apiStatusType(row.status)" size="small">
                      {{ row.status === 'SUCCESS' ? '通过' : row.status === 'FAILED' ? '失败' : row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="duration" label="耗时(ms)" width="90">
                  <template #default="{ row }">
                    {{ row.duration || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="执行时间" width="170">
                  <template #default="{ row }">
                    {{ formatTime(row.createdAt) }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-card>

          <el-card shadow="never" class="detail-card">
            <template #header>
              <span>执行详情</span>
            </template>
            <div v-loading="apiDetailLoading" class="detail-body">
              <div v-if="apiDetail" class="api-detail-content">
                <div class="detail-header">
                  <div class="detail-info">
                    <el-tag :type="apiStatusType(apiDetail.status)" size="large">
                      {{ apiDetail.status === 'SUCCESS' ? '通过' : apiDetail.status === 'FAILED' ? '失败' : apiDetail.status }}
                    </el-tag>
                    <span class="detail-url">{{ apiDetail.request?.url }}</span>
                  </div>
                  <div class="detail-meta">
                    <span>所属合集 {{ apiDetail.projectName || '-' }}</span>
                    <span>HTTP {{ apiDetail.httpStatus }} {{ apiDetail.statusText }}</span>
                    <span>耗时 {{ apiDetail.duration }}ms</span>
                  </div>
                </div>

                <el-tabs>
                  <el-tab-pane label="请求信息" name="request">
                    <div class="request-section">
                      <div class="section-item">
                        <div class="section-label">请求方法</div>
                        <el-tag :type="methodTagType(apiDetail.request?.method)">
                          {{ apiDetail.request?.method || 'GET' }}
                        </el-tag>
                      </div>
                      <div class="section-item">
                        <div class="section-label">请求地址</div>
                        <div class="code-block">{{ apiDetail.request?.url }}</div>
                      </div>
                      <div v-if="apiDetail.request?.headers" class="section-item">
                        <div class="section-label">请求头</div>
                        <pre class="code-block">{{ formatJson(apiDetail.request.headers) }}</pre>
                      </div>
                      <div v-if="apiDetail.request?.body" class="section-item">
                        <div class="section-label">请求体</div>
                        <pre class="code-block">{{ formatBody(apiDetail.request.body) }}</pre>
                      </div>
                    </div>
                  </el-tab-pane>

                  <el-tab-pane label="响应信息" name="response">
                    <div class="response-section">
                      <div v-if="apiDetail.response?.headers" class="section-item">
                        <div class="section-label">响应头</div>
                        <pre class="code-block">{{ formatJson(apiDetail.response.headers) }}</pre>
                      </div>
                      <div v-if="apiDetail.response?.body" class="section-item">
                        <div class="section-label">响应体</div>
                        <pre class="code-block response-body">{{ formatBody(apiDetail.response.body) }}</pre>
                      </div>
                      <div v-if="apiDetail.errorMessage" class="section-item">
                        <div class="section-label">错误信息</div>
                        <pre class="code-block error-body">{{ apiDetail.errorMessage }}</pre>
                      </div>
                    </div>
                  </el-tab-pane>

                  <el-tab-pane label="断言结果" name="assertions">
                    <div v-if="apiDetail.assertions && apiDetail.assertions.length > 0" class="assertions-section">
                      <div
                        v-for="(assertion, idx) in apiDetail.assertions"
                        :key="idx"
                        class="assertion-item"
                        :class="{ passed: assertion.passed, failed: !assertion.passed }"
                      >
                        <el-icon><Check v-if="assertion.passed" /><Close v-else /></el-icon>
                        <div class="assertion-content">
                          <div class="assertion-description">{{ assertion.description || assertion.type }}</div>
                          <div class="assertion-details">
                            <span>期望: {{ assertion.expected }}</span>
                            <span>实际: {{ assertion.actual }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                    <el-empty v-else description="暂无断言结果" />
                  </el-tab-pane>

                  <el-tab-pane label="前置步骤" name="prescripts">
                    <div v-if="apiDetail.prescriptResults && apiDetail.prescriptResults.length > 0" class="prescripts-section">
                      <div
                        v-for="(step, idx) in apiDetail.prescriptResults"
                        :key="idx"
                        class="prescript-card"
                      >
                        <div class="prescript-card__header">
                          <div class="prescript-card__title">
                            <span class="prescript-index">#{{ step.index || idx + 1 }}</span>
                            <span>{{ step.title || step.stepType || '前置步骤' }}</span>
                          </div>
                          <div class="prescript-card__meta">
                            <el-tag size="small" effect="plain">{{ step.stepType || '-' }}</el-tag>
                            <el-tag :type="prescriptStatusType(step.status)" size="small">
                              {{ prescriptStatusText(step.status) }}
                            </el-tag>
                            <el-tag v-if="step.stopOnFail" type="warning" size="small" effect="plain">失败终止</el-tag>
                          </div>
                        </div>

                        <div v-if="step.message" class="section-item">
                          <div class="section-label">执行结果</div>
                          <div class="code-block">{{ step.message }}</div>
                        </div>

                        <div v-if="step.variableName" class="section-item">
                          <div class="section-label">变量设置</div>
                          <div class="code-block">{{ step.variableName }} = {{ step.variableValue || '' }}</div>
                        </div>

                        <div v-if="step.request" class="section-item">
                          <div class="section-label">步骤请求</div>
                          <div class="mini-section">
                            <div class="mini-row">
                              <el-tag :type="methodTagType(step.request.method)" size="small">
                                {{ step.request.method || 'GET' }}
                              </el-tag>
                              <span class="detail-url">{{ step.request.url }}</span>
                            </div>
                            <pre v-if="step.request.headers && Object.keys(step.request.headers).length > 0" class="code-block">{{ formatJson(step.request.headers) }}</pre>
                            <pre v-if="step.request.body" class="code-block">{{ formatBody(step.request.body) }}</pre>
                          </div>
                        </div>

                        <div v-if="step.response" class="section-item">
                          <div class="section-label">步骤响应</div>
                          <div class="mini-section">
                            <div class="mini-row">
                              <el-tag :type="httpStatusType(step.response.status)" size="small">
                                {{ step.response.status || '-' }}
                              </el-tag>
                              <span>{{ step.response.statusText || '-' }}</span>
                              <span>耗时 {{ step.response.duration || 0 }}ms</span>
                            </div>
                            <pre v-if="step.response.headers && Object.keys(step.response.headers).length > 0" class="code-block">{{ formatJson(step.response.headers) }}</pre>
                            <pre v-if="step.response.body" class="code-block response-body">{{ formatBody(step.response.body) }}</pre>
                          </div>
                        </div>

                        <div v-if="step.extractedVariables && step.extractedVariables.length > 0" class="section-item">
                          <div class="section-label">提取变量</div>
                          <div class="extracted-list">
                            <div
                              v-for="(item, extractedIdx) in step.extractedVariables"
                              :key="`${idx}-${extractedIdx}`"
                              class="extracted-item"
                            >
                              <span class="extracted-name">{{ item.name }}</span>
                              <span class="extracted-path">{{ item.path }}</span>
                              <span class="extracted-value">{{ item.value }}</span>
                            </div>
                          </div>
                        </div>

                        <div v-if="step.assertions && step.assertions.length > 0" class="section-item">
                          <div class="section-label">步骤断言</div>
                          <div class="assertions-section">
                            <div
                              v-for="(assertion, assertionIdx) in step.assertions"
                              :key="`${idx}-${assertionIdx}`"
                              class="assertion-item"
                              :class="{ passed: assertion.passed, failed: !assertion.passed }"
                            >
                              <el-icon><Check v-if="assertion.passed" /><Close v-else /></el-icon>
                              <div class="assertion-content">
                                <div class="assertion-description">{{ assertion.description || assertion.type }}</div>
                                <div class="assertion-details">
                                  <span>期望: {{ assertion.expected }}</span>
                                  <span>实际: {{ assertion.actual }}</span>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>

                        <div v-if="step.errorMessage" class="section-item">
                          <div class="section-label">错误信息</div>
                          <pre class="code-block error-body">{{ step.errorMessage }}</pre>
                        </div>
                      </div>
                    </div>
                    <el-empty v-else description="暂无前置步骤结果" />
                  </el-tab-pane>
                </el-tabs>
              </div>
              <el-empty v-else description="请从左侧选择一条执行记录" />
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh, Check, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import UiExecutionReportPanel from '../components/ui-test/UiExecutionReportPanel.vue'
import { listExecutions, getExecutionDetail } from '../api/uiTest'
import { listApiExecutions, getApiExecutionDetail } from '../api/apiTest'

const route = useRoute()
const router = useRouter()

const activeTab = ref('ui')

const uiListLoading = ref(false)
const uiDetailLoading = ref(false)
const uiExecutions = ref([])
const selectedUiId = ref(null)
const uiDetail = ref(null)

const apiListLoading = ref(false)
const apiDetailLoading = ref(false)
const apiExecutions = ref([])
const selectedApiId = ref(null)
const apiDetail = ref(null)

function uiStatusType(status) {
  if (status === 'PASSED') return 'success'
  if (status === 'FAILED' || status === 'STOPPED') return 'danger'
  return 'info'
}

function apiStatusType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

function httpStatusType(status) {
  if (!status) return 'info'
  if (status >= 200 && status < 300) return 'success'
  if (status >= 400 && status < 500) return 'warning'
  if (status >= 500) return 'danger'
  return 'info'
}

function prescriptStatusType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'SKIPPED') return 'info'
  return 'warning'
}

function prescriptStatusText(status) {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'SKIPPED') return '已跳过'
  return status || '未知'
}

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

function formatTime(iso) {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return String(iso)
  return d.toLocaleString('zh-CN', { hour12: false })
}

function formatJson(obj) {
  if (!obj) return ''
  if (typeof obj === 'string') {
    try {
      return JSON.stringify(JSON.parse(obj), null, 2)
    } catch {
      return obj
    }
  }
  return JSON.stringify(obj, null, 2)
}

function formatBody(body) {
  if (!body) return ''
  if (typeof body === 'string') {
    try {
      return JSON.stringify(JSON.parse(body), null, 2)
    } catch {
      return body
    }
  }
  return JSON.stringify(body, null, 2)
}

async function loadUiList() {
  uiListLoading.value = true
  try {
    const res = await listExecutions()
    uiExecutions.value = res.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载执行列表失败')
    uiExecutions.value = []
  } finally {
    uiListLoading.value = false
  }
}

async function loadUiDetail(id) {
  if (!id) {
    uiDetail.value = null
    return
  }
  uiDetailLoading.value = true
  try {
    const res = await getExecutionDetail(id)
    uiDetail.value = res.data
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载报告详情失败')
    uiDetail.value = null
  } finally {
    uiDetailLoading.value = false
  }
}

function uiRowClassName({ row }) {
  return row.id === selectedUiId.value ? 'ui-exec-row-selected' : ''
}

function onUiRowClick(row) {
  selectedUiId.value = row.id
  router.replace({ query: { ...route.query, uiExecution: String(row.id) } })
  loadUiDetail(row.id)
}

async function loadApiList() {
  apiListLoading.value = true
  try {
    const res = await listApiExecutions(50)
    apiExecutions.value = res.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载API执行列表失败')
    apiExecutions.value = []
  } finally {
    apiListLoading.value = false
  }
}

async function loadApiDetail(id) {
  if (!id) {
    apiDetail.value = null
    return
  }
  apiDetailLoading.value = true
  try {
    const res = await getApiExecutionDetail(id)
    apiDetail.value = res.data
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载执行详情失败')
    apiDetail.value = null
  } finally {
    apiDetailLoading.value = false
  }
}

function apiRowClassName({ row }) {
  return row.id === selectedApiId.value ? 'ui-exec-row-selected' : ''
}

function onApiRowClick(row) {
  selectedApiId.value = row.id
  router.replace({ query: { ...route.query, apiExecution: String(row.id) } })
  loadApiDetail(row.id)
}

onMounted(() => {
  loadUiList()
})

watch(
  () => route.query.uiExecution,
  (q) => {
    const id = q ? Number(q) : null
    if (id && !Number.isNaN(id)) {
      selectedUiId.value = id
      loadUiDetail(id)
    } else {
      selectedUiId.value = null
      uiDetail.value = null
    }
  },
  { immediate: true }
)

watch(
  () => route.query.apiExecution,
  (q) => {
    const id = q ? Number(q) : null
    if (id && !Number.isNaN(id)) {
      selectedApiId.value = id
      loadApiDetail(id)
    } else {
      selectedApiId.value = null
      apiDetail.value = null
    }
  },
  { immediate: true }
)

watch(activeTab, (tab) => {
  if (tab === 'api' && apiExecutions.value.length === 0) {
    loadApiList()
  }
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  padding: 0;
  margin-bottom: 12px;
}

.report-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
  border-radius: var(--border-radius);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, rgba(246, 250, 255, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: var(--card-shadow);
}

.report-hero__main {
  min-width: 0;
}

.hero-title {
  margin: 0 0 10px;
  font-size: 28px;
  line-height: 1.2;
  color: var(--text-primary);
}

.hero-desc {
  max-width: 740px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.report-hero__side {
  display: grid;
  gap: 12px;
  min-width: 220px;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 16px;
  background: var(--primary-soft-gradient);
  border: 1px solid rgba(59, 130, 246, 0.12);
}

.hero-stat__label {
  font-size: 12px;
  color: var(--text-secondary);
}

.hero-stat__value {
  font-size: 24px;
  color: var(--text-primary);
}

.report-tabs {
  height: calc(100vh - 190px);
}

.ui-report-layout,
.api-report-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 250px);
}

.list-card {
  width: 480px;
  flex-shrink: 0;
}

.detail-card {
  flex: 1;
  overflow: hidden;
}

.list-body {
  overflow-y: auto;
}

.detail-body {
  overflow-y: auto;
  height: calc(100vh - 220px);
}

.detail-header {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.95);
}

.detail-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.detail-url {
  font-size: 14px;
  color: var(--el-text-color-regular);
  word-break: break-all;
}

.detail-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.request-section,
.response-section,
.assertions-section,
.prescripts-section {
  padding: 12px 4px;
}

.section-item {
  margin-bottom: 16px;
}

.section-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.code-block {
  background: #f8fbff;
  padding: 12px;
  border-radius: 12px;
  font-size: 12px;
  font-family: 'Monaco', 'Menlo', monospace;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.response-body {
  background: #f0f9ff;
}

.error-body {
  background: #fef2f2;
  color: #dc2626;
}

.assertion-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  margin-bottom: 8px;
}

.assertion-item.passed {
  background: #f0fdf4;
}

.assertion-item.failed {
  background: #fef2f2;
}

.assertion-content {
  flex: 1;
}

.assertion-description {
  font-weight: 500;
  margin-bottom: 4px;
}

.assertion-details {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  display: flex;
  gap: 16px;
}

.prescript-card {
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
  margin-bottom: 12px;
}

.prescript-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.prescript-card__title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  font-weight: 600;
  color: var(--text-primary);
}

.prescript-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.prescript-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  background: #f1f5f9;
  color: var(--text-secondary);
  font-size: 12px;
}

.mini-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mini-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.extracted-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.extracted-item {
  display: grid;
  grid-template-columns: 120px 1fr 1fr;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.95);
  font-size: 12px;
}

.extracted-name {
  font-weight: 600;
  color: var(--text-primary);
}

.extracted-path,
.extracted-value {
  color: var(--el-text-color-secondary);
  word-break: break-all;
}

@media (max-width: 1200px) {
  .report-hero {
    flex-direction: column;
  }

  .report-hero__side {
    min-width: 0;
    grid-template-columns: 1fr 1fr;
  }

  .prescript-card__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .extracted-item {
    grid-template-columns: 1fr;
  }
}
</style>
