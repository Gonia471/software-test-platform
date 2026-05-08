<template>
  <div class="ui-test-page">
    <el-page-header
      content="UI 测试用例编排"
      class="page-header"
      @back="goBack"
    />

    <el-card shadow="never" class="case-card">
      <div class="case-header">
        <div class="case-info">
          <div class="case-meta-row">
            <div class="meta-item">
              <span class="meta-label">组织空间</span>
              <el-select
                v-model="testCase.teamId"
                size="small"
                class="meta-select"
              >
                <el-option
                  v-for="team in uiStore.teams"
                  :key="team.id"
                  :label="team.name"
                  :value="team.id"
                />
              </el-select>
            </div>
            <div class="meta-item">
              <span class="meta-label">用例分类</span>
              <el-select
                v-model="testCase.moduleKey"
                size="small"
                class="meta-select"
              >
                <el-option
                  v-for="m in uiStore.modules"
                  :key="m.key"
                  :label="m.name"
                  :value="m.key"
                />
              </el-select>
            </div>
            <div class="meta-item">
              <span class="meta-label">创建人</span>
              <span class="meta-value meta-value-box">{{ testCase.creator }}</span>
            </div>
          </div>
          <div class="case-title-row">
            <span class="meta-label">用例 ID</span>
            <el-input
              :value="testCase.seq"
              size="small"
              class="case-id-input"
              disabled
            />
            <span class="meta-label">用例标题</span>
            <el-input
              v-model="testCase.name"
              size="small"
              class="case-name-input"
              placeholder="用例名称，如：登录成功后进入仪表盘"
            />
          </div>
          <el-input
            v-model="testCase.summary"
            type="textarea"
            :rows="2"
            class="case-summary"
            placeholder="用例简介（可选）：简要说明该用例的业务场景和期望结果"
          />
        </div>
        <div class="case-actions">
          <div class="case-actions-row">
            <el-button type="primary" @click="saveCase">
              保存用例
            </el-button>
            <el-button type="success" @click="runCase">
              执行测试
            </el-button>
          </div>
          <div class="case-actions-meta">
            <el-switch
              v-model="showBrowser"
              size="small"
              active-text="显示浏览器窗口"
              inactive-text="无头运行"
            />
            <span class="steps-tip">当前步骤数：{{ steps.length }}</span>
          </div>
        </div>
      </div>

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
      destroy-on-close
      class="execution-report-drawer"
      @close="stopPolling"
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
import { computed, reactive, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import ActionPalette from '../components/ui-test/ActionPalette.vue'
import StepList from '../components/ui-test/StepList.vue'
import StepForm from '../components/ui-test/StepForm.vue'
import UiExecutionReportPanel from '../components/ui-test/UiExecutionReportPanel.vue'
import { actionGroups, createStepFromAction } from './uiTestActions'
import { useUiTestStore } from '../stores/uiTest'
import { useUserStore } from '../stores/user'
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

const caseId = route.params.id

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
  teamId: '',
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
    teamId: testCase.teamId,
    moduleKey: testCase.moduleKey,
  }),
  (val) => {
    if (!caseId) return
    uiStore.updateCaseMeta(caseId, {
      name: val.name,
      summary: val.summary,
      teamId: val.teamId,
      moduleKey: val.moduleKey,
    })
  },
)

const currentTeamName = computed(() => {
  const team = uiStore.teams.find((t) => t.id === testCase.teamId)
  return team ? team.name : testCase.teamId || '未选择'
})

const currentModuleName = computed(() => {
  const m = uiStore.modules.find((it) => it.key === testCase.moduleKey)
  return m ? m.name : testCase.moduleKey || '未分类'
})

function initFromStore() {
  const meta = uiStore.getCaseById(caseId)
  if (!meta) {
    const fallbackTeam = uiStore.teams[0]
    const fallbackModule = uiStore.modules[0]
    testCase.id = caseId
    testCase.seq = ''
    testCase.teamId = fallbackTeam?.id || ''
    testCase.moduleKey = fallbackModule?.key || ''
    testCase.name = '未命名用例'
    testCase.summary = ''
    testCase.creator = userStore.username || '未命名用户'
  } else {
    testCase.id = meta.id
    testCase.seq = meta.seq ?? ''
    testCase.backendId = meta.backendId ?? null
    testCase.teamId = meta.teamId
    testCase.moduleKey = meta.moduleKey
    testCase.name = meta.name
    testCase.summary = meta.summary || ''
    testCase.creator = meta.creator || (userStore.username || '未命名用户')
  }

  const loadedSteps = loadStepsForCase(caseId)
  testCase.steps = loadedSteps && loadedSteps.length
    ? loadedSteps
    : [
        createStepFromAction(
          { key: 'openPage', label: '打开网页' },
          'browser',
        ),
      ]

  selectedStepId.value = testCase.steps[0]?.id || ''
}

initFromStore()

function onSelectStep(id) {
  selectedStepId.value = id
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
    teamId: testCase.teamId,
    moduleKey: testCase.moduleKey,
  })

  const payload = {
    name: testCase.name || '未命名用例',
    description: testCase.summary || '',
    steps: JSON.parse(JSON.stringify(testCase.steps ?? [])),
  }

  try {
    if (testCase.backendId) {
      await updateCase(testCase.backendId, payload)
    } else {
      const res = await createCase(payload)
      testCase.backendId = res.data.id
      uiStore.updateCaseMeta(caseId, { backendId: res.data.id })
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
      stopOnFailure: false,
      screenshotOnFailure: true,
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
  gap: 12px;
}

.page-header {
  padding: 0;
}

.case-card {
  width: 100%;
}

.case-header {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.case-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.case-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-label {
  font-size: 12px;
  color: #6b7280;
}

.meta-value {
  font-size: 13px;
  color: #111827;
}

.meta-value-box {
  display: inline-flex;
  align-items: center;
  min-width: 220px;
  height: 28px;
  padding: 0 8px;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
  background-color: #f9fafb;
}

.meta-select {
  width: 220px;
}

.case-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.case-id-input {
  width: 90px;
}

.case-name-input {
  flex: 1;
}

.case-meta {
  font-size: 12px;
  color: #6b7280;
}

.steps-tip {
  font-size: 12px;
  color: #6b7280;
}

.case-summary {
  margin-top: 4px;
}

.case-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.case-actions-row {
  display: flex;
  gap: 8px;
}

.case-actions-meta {
  font-size: 12px;
  color: #6b7280;
}

.layout-3col {
  margin-top: 4px;
}

.layout-row {
  height: 600px;
}

.col {
  height: 100%;
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
  gap: 8px;
  justify-content: flex-end;
}
</style>
