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
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import ActionPalette from '../components/ui-test/ActionPalette.vue'
import StepList from '../components/ui-test/StepList.vue'
import StepForm from '../components/ui-test/StepForm.vue'
import { actionGroups, createStepFromAction } from './uiTestActions'
import { useUiTestStore } from '../stores/uiTest'
import { useUserStore } from '../stores/user'

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
  teamId: '',
  moduleKey: '',
  name: '',
  summary: '',
  creator: '',
  steps: [],
})

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
    testCase.teamId = fallbackTeam?.id || ''
    testCase.moduleKey = fallbackModule?.key || ''
    testCase.name = '未命名用例'
    testCase.summary = ''
    testCase.creator = userStore.username || '未命名用户'
  } else {
    testCase.id = meta.id
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

function runCase() {
  ElMessage.info('目前为前端演示：执行逻辑将在 UI 测试后端完成后接入')
}

function goBack() {
  router.push('/ui-test')
}

function saveCase() {
  if (!caseId) return
  saveStepsForCase(caseId, testCase.steps)
  uiStore.updateCaseMeta(caseId, {
    name: testCase.name,
    summary: testCase.summary,
    teamId: testCase.teamId,
    moduleKey: testCase.moduleKey,
  })
  ElMessage.success('用例已保存')
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
</style>
