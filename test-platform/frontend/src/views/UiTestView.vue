<template>
  <div class="ui-test-page">
    <section class="editor-hero">
      <div class="editor-hero__main">
        <el-page-header
          content="UI 测试用例编排"
          class="page-header"
          @back="goBack"
        />
        <div class="hero-title-row">
          <h2 class="hero-title">{{ testCase.name || '未命名用例' }}</h2>
          <span class="hero-badge">{{ currentModuleName }}</span>
        </div>
        <p class="hero-desc">
          在统一工作台中编排步骤、配置参数并直接执行，整体风格已调整为更轻量、清晰的蓝灰色视觉体系。
        </p>
      </div>
      <div class="editor-hero__meta">
        <div class="hero-meta-card">
          <span class="hero-meta-card__label">所属组织</span>
          <strong class="hero-meta-card__value">{{ currentOrganizationName }}</strong>
        </div>
        <div class="hero-meta-card">
          <span class="hero-meta-card__label">步骤数</span>
          <strong class="hero-meta-card__value">{{ steps.length }}</strong>
        </div>
        <div class="hero-meta-card">
          <span class="hero-meta-card__label">执行模式</span>
          <strong class="hero-meta-card__value">{{ showBrowser ? '可视化' : '无头运行' }}</strong>
        </div>
      </div>
    </section>

    <el-card shadow="never" class="case-card">
      <el-collapse v-model="caseHeaderExpanded" class="case-header-collapse">
        <el-collapse-item title="用例信息" name="caseInfo">
          <div class="case-header">
            <div class="case-info">
              <div class="info-section">
                <div class="info-row">
                  <div class="info-item">
                    <span class="info-label">
                      <el-icon><OfficeBuilding /></el-icon>
                      组织空间
                    </span>
                    <el-select
                      v-model="selectedOrganizationId"
                      size="small"
                      class="info-select"
                      placeholder="选择组织"
                      :loading="loadingOrganizations"
                      @change="onOrganizationChange"
                    >
                      <el-option
                        v-for="org in organizations"
                        :key="org.id"
                        :label="org.name"
                        :value="org.id"
                      />
                    </el-select>
                  </div>
                  <div class="info-item">
                    <span class="info-label">
                      <el-icon><FolderOpened /></el-icon>
                      用例分类
                    </span>
                    <el-select
                      v-model="testCase.moduleKey"
                      size="small"
                      class="info-select"
                      placeholder="选择分类"
                    >
                      <el-option
                        v-for="m in uiStore.modules"
                        :key="m.key"
                        :label="m.name"
                        :value="m.key"
                      />
                    </el-select>
                  </div>
                  <div class="info-item">
                    <span class="info-label">
                      <el-icon><User /></el-icon>
                      创建人
                    </span>
                    <span class="info-value-text">{{ testCase.creator }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">
                      <el-icon><Collection /></el-icon>
                      用例 ID
                    </span>
                    <span class="info-value-text case-id">{{ testCase.seq }}</span>
                  </div>
                </div>
              </div>

              <div class="title-section">
                <div class="title-row">
                  <span class="title-label">
                    <el-icon><EditPen /></el-icon>
                    用例标题
                  </span>
                  <el-input
                    v-model="testCase.name"
                    size="default"
                    class="case-name-input"
                    placeholder="请输入用例名称，如：登录成功后进入仪表盘"
                  />
                </div>
              </div>

              <div class="summary-section">
                <div class="summary-label">
                  <el-icon><Document /></el-icon>
                  <span>用例简介</span>
                </div>
                <el-input
                  v-model="testCase.summary"
                  type="textarea"
                  :rows="2"
                  class="case-summary"
                  placeholder="简要说明该用例的业务场景和期望结果"
                />
              </div>
            </div>

            <div class="case-actions">
              <div class="actions-row">
                <el-button type="primary" size="large" class="action-btn" @click="saveCase">
                  <el-icon><Check /></el-icon>
                  保存用例
                </el-button>
                <el-button type="primary" plain size="large" class="action-btn" @click="runCase">
                  <el-icon><VideoPlay /></el-icon>
                  执行测试
                </el-button>
              </div>
              <div class="actions-meta">
                <el-switch
                  v-model="showBrowser"
                  size="small"
                  active-text="显示浏览器"
                  inactive-text="无头运行"
                />
                <el-switch
                  v-model="stopOnFailure"
                  size="small"
                  active-text="遇错终止"
                  inactive-text="遇错继续"
                />
                <div class="steps-info">
                  <el-icon><List /></el-icon>
                  步骤数：<strong>{{ steps.length }}</strong>
                </div>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>

      <div class="layout-3col">
        <el-row :gutter="12" class="layout-row">
          <el-col :span="6" class="col">
            <ActionPalette
              :groups="actionGroups"
              :drag-group="dragGroupPalette"
              :create-step-from-action="createStepFromAction"
            />
          </el-col>
          <el-col :span="10" class="col">
            <StepList
              ref="stepListRef"
              v-model:steps="testCase.steps"
              :selected-step-id="selectedStepId"
              :drag-group="dragGroupSteps"
              @select-step="onSelectStep"
              @duplicate-step="onDuplicateStep"
              @delete-step="onDeleteStep"
              @add-empty-step="onAddEmptyStep"
            />
          </el-col>
          <el-col :span="8" class="col">
            <StepForm
              :step="selectedStep"
              :index="selectedIndex"
              :total="testCase.steps.length"
              :page-url="currentPageUrl"
              @update-step="onUpdateStep"
              @change-order="onChangeOrder"
            />
          </el-col>
        </el-row>
      </div>
    </el-card>

    <el-dialog
      v-model="instanceDialogVisible"
      title="选择执行实例"
      width="420px"
      :close-on-click-modal="false"
    >
      <div v-if="instanceLoading">正在加载执行实例...</div>
      <div v-else>
        <div v-if="!instances.length">暂无可用执行实例</div>
        <el-radio-group v-else v-model="selectedInstanceId">
          <el-radio
            v-for="ins in instances"
            :key="ins.id"
            :label="ins.id"
          >
            {{ ins.name }}（{{ ins.type }}）<span v-if="!ins.enabled"> - 已禁用</span>
          </el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="instanceDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!selectedInstanceId || !instances.length"
          @click="confirmRunOnInstance"
        >
          开始运行
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="executionDialogVisible"
      :title="isExecutionRunning ? '正在执行 UI 自动化测试…' : 'UI 自动化测试报告'"
      direction="rtl"
      size="min(720px, 92vw)"
      :close-on-click-modal="false"
      class="execution-report-drawer"
      @close="onExecutionDrawerClose"
    >
      <div v-if="executionDetail" class="execution-drawer-body">
        <UiExecutionReportPanel :detail="executionDetail" />
      </div>
      <div v-else class="execution-drawer-empty">正在加载执行信息…</div>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="executionDialogVisible = false">关闭</el-button>
          <el-button
            v-if="isExecutionRunning"
            type="danger"
            @click="handleStopExecution"
          >
            停止执行
          </el-button>
          <el-button
            v-if="currentExecutionId && !isExecutionRunning"
            type="primary"
            plain
            @click="goToReportPage"
          >
            在报告中心查看
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  OfficeBuilding,
  FolderOpened,
  User,
  Collection,
  EditPen,
  Check,
  VideoPlay,
  List,
  Document,
} from '@element-plus/icons-vue'

import ActionPalette from '../components/ui-test/ActionPalette.vue'
import StepList from '../components/ui-test/StepList.vue'
import StepForm from '../components/ui-test/StepForm.vue'
import UiExecutionReportPanel from '../components/ui-test/UiExecutionReportPanel.vue'
import { actionGroups, createStepFromAction } from './uiTestActions'
import { useUiTestStore } from '../stores/uiTest'
import { useUserStore } from '../stores/user'
import { useOrgStore } from '../stores/org'
import {
  createCase,
  updateCase,
  getInstances,
  startExecution,
  getExecutionDetail,
  stopExecution,
} from '../api/uiTest'

const route = useRoute()
const router = useRouter()
const uiStore = useUiTestStore()
const userStore = useUserStore()
const orgStore = useOrgStore()

const caseId = route.params.id

const selectedOrganizationId = ref(null)
const organizations = computed(() => orgStore.organizations || [])
const loadingOrganizations = ref(false)
const initializingCase = ref(true)

const dragGroupSteps = {
  name: 'ui-steps-group',
  pull: false,
  put: true,
}

const dragGroupPalette = {
  name: 'ui-steps-group',
  pull: 'clone',
  put: false,
}

const stepListRef = ref(null)
const testCase = reactive({
  id: caseId,
  seq: '',
  backendId: null,
  moduleKey: '',
  name: '',
  summary: '',
  creator: '',
  steps: [],
})

const executionDialogVisible = ref(false)
const executionDetail = ref(null)
const currentExecutionId = ref(null)
let pollingTimer = null

const instanceDialogVisible = ref(false)
const instances = ref([])
const selectedInstanceId = ref(null)
const instanceLoading = ref(false)

// 显示浏览器窗口：true 表示可见浏览器，false 表示无头运行
const showBrowser = ref(true)

// 遇到错误时是否终止执行
const stopOnFailure = ref(false)

const caseHeaderExpanded = ref(['caseInfo'])

const STEPS_KEY_PREFIX = 'ui-test-case-steps-'

function loadStepsForCase(id) {
  try {
    const raw = localStorage.getItem(STEPS_KEY_PREFIX + id)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return null
    return parsed
  } catch {
    return null
  }
}

function saveStepsForCase(id, steps) {
  localStorage.setItem(STEPS_KEY_PREFIX + id, JSON.stringify(steps))
}

const selectedStepId = ref('')

const steps = computed(() => testCase.steps)

const selectedStep = computed(() =>
  testCase.steps.find((s) => s.id === selectedStepId.value) || null,
)

const selectedIndex = computed(() =>
  testCase.steps.findIndex((s) => s.id === selectedStepId.value),
)

const currentPageUrl = computed(() => {
  const index = selectedIndex.value
  if (index < 0) {
    return ''
  }
  for (let i = index; i >= 0; i -= 1) {
    const step = testCase.steps[i]
    if (step?.action === 'openPage' && step?.parameters?.url) {
      return String(step.parameters.url).trim()
    }
  }
  return ''
})

watch(
  () => testCase.steps,
  (val) => {
    if (!caseId) return
    saveStepsForCase(caseId, val)
  },
  { deep: true },
)

watch(
  () => ({
    name: testCase.name,
    summary: testCase.summary,
    moduleKey: testCase.moduleKey,
  }),
  (val) => {
    if (!caseId || initializingCase.value) return
    uiStore.updateCaseMeta(caseId, {
      name: val.name,
      description: val.summary,
      moduleKey: val.moduleKey,
    })
  },
)

const currentOrganizationName = computed(() => {
  if (selectedOrganizationId.value) {
    const org = organizations.value.find((o) => o.id === selectedOrganizationId.value)
    return org ? org.name : '未选择'
  }
  return '未选择'
})

const currentModuleName = computed(() => {
  const m = uiStore.modules.find((it) => it.key === testCase.moduleKey)
  return m ? m.name : testCase.moduleKey || uiStore.modules[0]?.name || ''
})

function resolveActionMeta(actionKey) {
  for (const group of actionGroups) {
    const action = group.actions.find((item) => item.key === actionKey)
    if (action) {
      return { groupType: group.type, action }
    }
  }
  return null
}

function normalizeStep(step) {
  const actionKey = step?.action || step?.actionType || ''
  const resolved = resolveActionMeta(actionKey)
  const groupType = step?.type || step?.stepType || resolved?.groupType || ''
  const base = resolved
    ? createStepFromAction({ key: resolved.action.key, label: resolved.action.label }, groupType)
    : {
        id: '',
        type: groupType,
        action: actionKey,
        description: '',
        parameters: {},
      }

  return {
    ...base,
    ...step,
    id: step?.id || base.id,
    type: groupType,
    action: actionKey,
    description: step?.description ?? base.description ?? '',
    parameters: {
      ...(base.parameters || {}),
      ...(step?.parameters || step?.params || {}),
    },
  }
}

function normalizeSteps(steps) {
  return (Array.isArray(steps) ? steps : []).map((step) => normalizeStep(step))
}

async function loadCaseDetail() {
  const fallbackModule = uiStore.modules[0]
  const fallbackMeta = uiStore.findCaseById(caseId)

  testCase.id = Number(caseId) || caseId
  testCase.seq = fallbackMeta?.id ?? ''
  testCase.backendId = Number(caseId) || null
  testCase.moduleKey = fallbackMeta?.moduleKey || fallbackModule?.key || ''
  testCase.name = fallbackMeta?.name || ''
  testCase.summary = fallbackMeta?.description || ''
  testCase.creator = fallbackMeta?.creator || (userStore.username || '未命名用户')
  selectedOrganizationId.value = fallbackMeta?.organizationId || orgStore.currentOrganizationId || organizations.value[0]?.id || null

  try {
    const detail = await uiStore.getCaseById(caseId)
    testCase.id = detail.id
    testCase.seq = detail.id
    testCase.backendId = detail.id
    testCase.moduleKey = detail.moduleKey || fallbackModule?.key || ''
    testCase.name = detail.name || ''
    testCase.summary = detail.description || ''
    testCase.creator = detail.creator || (userStore.username || '未命名用户')
    selectedOrganizationId.value = detail.organizationId || orgStore.currentOrganizationId || organizations.value[0]?.id || null

    const cachedSteps = loadStepsForCase(caseId)
    const remoteSteps = Array.isArray(detail.steps) ? normalizeSteps(detail.steps) : null
    const normalizedCachedSteps = Array.isArray(cachedSteps) ? normalizeSteps(cachedSteps) : null
    testCase.steps = remoteSteps ?? normalizedCachedSteps ?? []
  } catch (error) {
    const loadedSteps = loadStepsForCase(caseId)
    testCase.steps = Array.isArray(loadedSteps)
      ? normalizeSteps(loadedSteps)
      : []
    ElMessage.error(error.response?.data?.message || '加载用例详情失败')
  }

  selectedStepId.value = testCase.steps[0]?.id || ''
}

onMounted(async () => {
  // 获取用户有权限的组织列表
  loadingOrganizations.value = true
  initializingCase.value = true
  try {
    await orgStore.fetchOrganizations()
    await loadCaseDetail()
  } catch (error) {
    console.error('加载组织数据失败:', error)
    ElMessage.error('加载组织数据失败，请检查网络连接')
  } finally {
    initializingCase.value = false
    loadingOrganizations.value = false
  }
})

function onSelectStep(id) {
  selectedStepId.value = id
}

function onOrganizationChange(orgId) {
  selectedOrganizationId.value = orgId
  orgStore.setCurrentOrganization(orgId)
}

function onUpdateStep(newStep) {
  const index = testCase.steps.findIndex((s) => s.id === newStep.id)
  if (index !== -1) {
    testCase.steps[index] = {
      ...testCase.steps[index],
      ...newStep,
      parameters: {
        ...(testCase.steps[index].parameters || {}),
        ...(newStep.parameters || {}),
      },
    }
  }
}

function onDuplicateStep(id) {
  const index = testCase.steps.findIndex((s) => s.id === id)
  if (index === -1) return
  const source = testCase.steps[index]
  const copy = createStepFromAction(
    { key: source.action, label: source.description || source.action },
    source.type,
  )
  copy.parameters = { ...(source.parameters || {}) }
  copy.description = `${source.description || ''}（复制）`
  testCase.steps.splice(index + 1, 0, copy)
  selectedStepId.value = copy.id
}

function onDeleteStep(id) {
  const index = testCase.steps.findIndex((s) => s.id === id)
  if (index === -1) return
  testCase.steps.splice(index, 1)
  if (selectedStepId.value === id) {
    selectedStepId.value = testCase.steps[0]?.id || ''
  }
}

function onAddEmptyStep() {
  const step = createStepFromAction(
    { key: 'sleep', label: '强制等待' },
    'wait',
  )
  testCase.steps.push(step)
  selectedStepId.value = step.id

  nextTick(() => {
    if (stepListRef.value && stepListRef.value.scrollToStep) {
      stepListRef.value.scrollToStep(step.id)
    }
  })
}

function onChangeOrder(targetIndex) {
  if (!selectedStep.value) return
  const currentIndex = testCase.steps.findIndex(
    (s) => s.id === selectedStep.value.id,
  )
  if (currentIndex === -1 || targetIndex === currentIndex) return

  const to = Math.min(
    Math.max(targetIndex, 0),
    testCase.steps.length - 1,
  )
  const [moved] = testCase.steps.splice(currentIndex, 1)
  testCase.steps.splice(to, 0, moved)
}

const isExecutionRunning = computed(
  () =>
    executionDetail.value &&
    ['PENDING', 'RUNNING'].includes(executionDetail.value.status),
)

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

function onExecutionDrawerClose() {
  stopPolling()
  if (executionDetail.value && !isExecutionRunning.value) {
    executionDetail.value = null
    currentExecutionId.value = null
  }
}

async function pollExecution() {
  if (!currentExecutionId.value) return
  try {
    const res = await getExecutionDetail(currentExecutionId.value)
    executionDetail.value = res.data
    const s = res.data?.status
    if (s && !['PENDING', 'RUNNING'].includes(s)) stopPolling()
  } catch {
    stopPolling()
  }
}

async function saveCase() {
  if (!caseId) return
  saveStepsForCase(caseId, testCase.steps)
  uiStore.updateCaseMeta(caseId, {
    name: testCase.name,
    summary: testCase.summary,
    moduleKey: testCase.moduleKey,
  })

  const payload = {
    name: testCase.name || '未命名用例',
    description: testCase.summary || '',
    moduleKey: testCase.moduleKey,
    steps: JSON.parse(JSON.stringify(testCase.steps ?? [])),
    organizationId: selectedOrganizationId.value,
  }

  try {
    if (testCase.backendId) {
      await updateCase(testCase.backendId, payload)
    } else {
      const res = await createCase(payload)
      testCase.backendId = res.data.id
      testCase.id = res.data.id
      testCase.seq = res.data.id
    }
    ElMessage.success('用例已保存')
    return testCase.backendId
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '保存失败')
    return null
  }
}

async function runCase() {
  if (!testCase.steps?.length) {
    ElMessage.warning('请先添加测试步骤')
    return
  }

  // 每次执行前强制保存最新步骤到后端，避免执行旧版本
  const backendId = await saveCase()
  if (!backendId) return

  try {
    instanceLoading.value = true
    const instancesRes = await getInstances()
    const list = instancesRes.data || []
    if (!list.length) {
      ElMessage.error('无可用的执行实例')
      return
    }
    instances.value = list

    // 默认选择：优先已启用的本地实例（名称含“本地”或 type 为 LOCAL），否则第一个
    const preferred =
      list.find((i) => i.enabled && (i.type === 'LOCAL' || i.name?.includes('本地'))) ||
      list.find((i) => i.enabled) ||
      list[0]
    selectedInstanceId.value = preferred.id
    instanceDialogVisible.value = true
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '启动执行失败')
  } finally {
    instanceLoading.value = false
  }
}

async function confirmRunOnInstance() {
  if (!selectedInstanceId.value) return
  const backendId = testCase.backendId
  if (!backendId) {
    ElMessage.error('用例尚未保存，无法执行')
    return
  }
  try {
    const execRes = await startExecution({
      testCaseId: backendId,
      instanceId: selectedInstanceId.value,
      headless: !showBrowser.value,
      stopOnFailure: stopOnFailure.value,
      screenshotOnFailure: true,
      screenshotEveryStep: true,
    })
    instanceDialogVisible.value = false
    currentExecutionId.value = execRes.data.executionId
    executionDetail.value = { status: execRes.data.status }
    executionDialogVisible.value = true
    pollingTimer = setInterval(pollExecution, 1500)
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '启动执行失败，请检查后端服务是否正常'
    ElMessage.error(msg)
    console.error('[UI Test] startExecution failed:', err)
  }
}

async function handleStopExecution() {
  if (!currentExecutionId.value) return
  try {
    await stopExecution(currentExecutionId.value)
    ElMessage.info('已请求停止执行')
    pollExecution()
  } catch {
    ElMessage.error('停止请求失败')
  }
}

function goBack() {
  router.push('/ui-test')
}

function goToReportPage() {
  executionDialogVisible.value = false
  router.push({ path: '/reports', query: { uiExecution: String(currentExecutionId.value) } })
}
</script>

<style scoped>
.ui-test-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.editor-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
  border-radius: var(--border-radius);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, rgba(246, 250, 255, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: var(--card-shadow);
}

.editor-hero__main {
  min-width: 0;
}

.page-header {
  padding: 0;
  margin-bottom: 12px;
}

.hero-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.hero-title {
  font-size: 28px;
  line-height: 1.2;
  color: var(--text-primary);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.hero-desc {
  max-width: 720px;
  color: var(--text-secondary);
  line-height: 1.7;
  font-size: 14px;
}

.editor-hero__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 12px;
  min-width: 390px;
}

.hero-meta-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 16px;
  background: var(--primary-soft-gradient);
  border: 1px solid rgba(59, 130, 246, 0.12);
}

.hero-meta-card__label {
  font-size: 12px;
  color: var(--text-secondary);
}

.hero-meta-card__value {
  font-size: 20px;
  color: var(--text-primary);
}

.case-card {
  width: 100%;
  border-radius: var(--border-radius);
  overflow: hidden;
}

.case-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 22px;
  padding: 18px 20px 22px;
  background: linear-gradient(135deg, #fbfdff 0%, #f4f9ff 100%);
}

.case-header-collapse {
  border: none;
}

.case-header-collapse :deep(.el-collapse-item__header) {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  background: linear-gradient(135deg, #fbfdff 0%, #f4f9ff 100%);
  padding: 0 20px;
  height: 48px;
  line-height: 48px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.85);
}

.case-header-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

.case-header-collapse :deep(.el-collapse-item__content) {
  padding: 0;
}

.case-header-collapse :deep(.el-collapse-item__arrow) {
  color: var(--primary-color);
}

.case-info {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.info-section {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 16px 20px;
  border: 1px solid rgba(226, 232, 240, 0.72);
}

.info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px 24px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  background: #f8fbff;
}

.info-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  white-space: nowrap;
}

.info-label .el-icon {
  color: var(--primary-color);
}

.info-select {
  width: 140px;
}

.info-value-text {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
  background: #ffffff;
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.info-value-text.case-id {
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  color: var(--primary-color);
  font-weight: 600;
}

.title-section {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 16px 20px;
  border: 1px solid rgba(226, 232, 240, 0.72);
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  white-space: nowrap;
}

.title-label .el-icon {
  color: var(--primary-color);
}

.case-name-input {
  flex: 1;
}

.summary-section {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 16px 20px;
  border: 1px solid rgba(226, 232, 240, 0.72);
}

.summary-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
  margin-bottom: 8px;
}

.summary-label .el-icon {
  color: var(--primary-color);
}

.summary-section :deep(.el-textarea__inner) {
  border: none;
  background: #f8fbff;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--text-primary);
  border-radius: 12px;
  line-height: 1.6;
}

.case-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 16px;
  padding: 20px 16px;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.76);
  min-width: 220px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.actions-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.actions-row .action-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 0 !important;
}

.actions-meta {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  width: 100%;
}

.steps-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.steps-info .el-icon {
  color: var(--primary-color);
}

.steps-info strong {
  color: var(--primary-color);
  font-weight: 600;
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 500;
}

:deep(.el-switch) {
  --el-switch-on-color: var(--primary-color);
}

:deep(.el-textarea__inner) {
  border-radius: 8px;
}

.layout-3col {
  margin-top: 8px;
}

.layout-row {
  height: 620px;
}

.col {
  height: 100%;
}

:deep(.el-card) {
  border-radius: var(--border-radius);
}

.execution-drawer-body {
  padding: 0 4px 8px;
}

.execution-drawer-empty {
  padding: 24px;
  text-align: center;
  color: #94a3b8;
}

.drawer-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

:deep(.el-dialog) {
  border-radius: var(--border-radius);
}

:deep(.el-radio) {
  margin-bottom: 10px;
}

:deep(.el-drawer) {
  border-radius: var(--border-radius) 0 0 var(--border-radius);
}

@media (max-width: 1280px) {
  .editor-hero {
    flex-direction: column;
  }

  .editor-hero__meta {
    min-width: 0;
  }
}

@media (max-width: 900px) {
  .editor-hero__meta {
    grid-template-columns: 1fr;
  }

  .case-header {
    flex-direction: column;
  }
}
</style>
