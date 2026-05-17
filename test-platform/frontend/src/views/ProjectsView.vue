<template>
  <div class="page">
    <section class="project-hero">
      <div class="project-hero__main">
        <el-page-header :content="orgStore.currentOrganization?.name + ' - 合集管理'" class="hero-header" @back="goBack" />
        <h2 class="hero-title">{{ orgStore.currentOrganization?.name || '合集管理' }}</h2>
        <p class="hero-desc">
          创建项目即创建测试合集。UI 合集可编排当前组织下的 UI 合集，API 合集可编排接口测试用例，并支持执行顺序、定时任务、循环次数和执行实例配置。
        </p>
      </div>
      <div class="project-hero__side">
        <div class="hero-stat">
          <span class="hero-stat__label">当前组织</span>
          <strong class="hero-stat__value">{{ orgStore.currentOrganization?.name || '未选择' }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-stat__label">合集数量</span>
          <strong class="hero-stat__value">{{ projects.length }}</strong>
        </div>
      </div>
    </section>

    <div class="page-header">
      <el-button type="primary" @click="openCreateDialog" :disabled="!orgStore.currentOrganization">
        创建合集
      </el-button>
    </div>

    <div v-if="!orgStore.currentOrganization" class="no-org">
      <el-empty description="请先选择一个组织">
        <el-button type="primary" @click="goToOrganizations">选择组织</el-button>
      </el-empty>
    </div>

    <el-empty v-else-if="projects.length === 0" description="暂无合集，点击创建第一个合集">
      <el-button type="primary" @click="openCreateDialog">创建合集</el-button>
    </el-empty>

    <el-row :gutter="18" v-else class="project-grid">
      <el-col :span="8" v-for="project in projects" :key="project.id">
        <el-card class="project-card" shadow="hover">
          <div class="project-header">
            <div class="project-icon" :style="{ backgroundColor: project.color }">
              {{ project.name.charAt(0).toUpperCase() }}
            </div>
            <div class="project-info">
              <h3>{{ project.name }}</h3>
              <p v-if="project.description" class="description">{{ project.description }}</p>
              <p class="org-name">{{ project.organizationName }}</p>
            </div>
          </div>
          <div class="project-meta">
            <span class="meta-chip" :class="'meta-chip--' + project.type?.toLowerCase()">{{ project.type }} 合集</span>
            <span class="meta-chip meta-chip--subtle">编排 {{ projectItemCount(project) }} 项</span>
            <span class="meta-chip meta-chip--subtle">循环 {{ project.loopCount || 1 }} 次</span>
            <span class="meta-chip meta-chip--subtle">{{ describeCron(project.cronExpression) }}</span>
          </div>
          <div class="project-extra">
            <span>{{ isOwner(project) ? '合集所有者' : '协作成员' }}</span>
            <span v-if="project.type === 'UI'">实例 {{ instanceName(project.uiInstanceId) }}</span>
          </div>
          <div class="project-actions">
            <el-button type="primary" @click="enterProject(project)">进入</el-button>
            <el-button type="success" plain @click="runProject(project)">执行</el-button>
            <el-button plain @click="openEditDialog(project)">编辑</el-button>
            <el-button type="danger" plain @click="deleteProject(project)" v-if="isOwner(project)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="960px" destroy-on-close>
      <div v-loading="dialogLoading">
        <el-form :model="dialogForm" label-width="100px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="合集名称" required>
                <el-input v-model="dialogForm.name" placeholder="例如：核心业务回归测试" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="测试类型">
                <el-radio-group v-if="dialogMode === 'create'" v-model="dialogForm.type">
                  <el-radio-button label="API">API</el-radio-button>
                  <el-radio-button label="UI">UI</el-radio-button>
                </el-radio-group>
                <div v-else class="type-lock">
                  <el-tag>{{ dialogForm.type }}</el-tag>
                  <span class="tip">类型创建后不可修改</span>
                </div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="描述">
            <el-input v-model="dialogForm.description" type="textarea" :rows="2" placeholder="输入合集描述" />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="循环次数">
                <el-input-number v-model="dialogForm.loopCount" :min="1" :max="100" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="标识颜色">
                <el-color-picker v-model="dialogForm.color" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="启用合集">
                <el-switch v-model="dialogForm.enabled" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item v-if="dialogForm.type === 'UI'" label="执行实例">
            <el-select v-model="dialogForm.uiInstanceId" clearable placeholder="未选择时默认使用第一个可用实例" style="width: 100%">
              <el-option v-for="instance in instances" :key="instance.id" :label="instance.name" :value="instance.id" />
            </el-select>
          </el-form-item>

          <div class="schedule-panel">
            <div class="panel-title">定时任务</div>
            <el-radio-group v-model="scheduleForm.mode" class="schedule-mode">
              <el-radio-button label="none">不启用</el-radio-button>
              <el-radio-button label="daily">每天</el-radio-button>
              <el-radio-button label="weekly">每周</el-radio-button>
              <el-radio-button label="custom">自定义</el-radio-button>
            </el-radio-group>

            <div v-if="scheduleForm.mode === 'daily'" class="schedule-fields">
              <span class="field-label">执行时间</span>
              <el-time-picker
                v-model="scheduleForm.time"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="选择时间"
                style="width: 220px"
              />
            </div>

            <div v-if="scheduleForm.mode === 'weekly'" class="schedule-fields">
              <span class="field-label">执行时间</span>
              <el-time-picker
                v-model="scheduleForm.time"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="选择时间"
                style="width: 220px"
              />
              <el-checkbox-group v-model="scheduleForm.weekdays">
                <el-checkbox-button v-for="day in weekdayOptions" :key="day.value" :label="day.value">
                  {{ day.label }}
                </el-checkbox-button>
              </el-checkbox-group>
            </div>

            <div v-if="scheduleForm.mode === 'custom'" class="schedule-fields">
              <span class="field-label">Cron</span>
              <el-input v-model="scheduleForm.customCron" placeholder="例如：0 0 1 * * ?" />
            </div>

            <div class="schedule-preview">
              <span>当前配置：</span>
              <strong>{{ schedulePreview }}</strong>
            </div>
          </div>

          <div class="item-manager">
            <div class="panel-title">用例编排</div>
            <div class="item-manager__header">
              <span class="tip">
                无论是 UI 还是 API，合集都从当前组织已有的单个用例中选择。右侧支持拖拽调整执行顺序。
              </span>
            </div>
            <div class="item-manager__body">
              <div class="item-source">
                <div class="item-section__head">
                  <h4>{{ sourceTitle }}</h4>
                  <span>{{ sourceItems.length }} 项可选，可重复添加</span>
                </div>
                <div class="item-list">
                  <div v-for="item in sourceItems" :key="item.id" class="item-card">
                    <div class="item-card__content">
                      <span class="item-name">{{ item.name }}</span>
                      <span class="item-subtitle">{{ item.subtitle }}</span>
                    </div>
                    <el-button type="primary" link @click="addItem(item)">添加</el-button>
                  </div>
                  <el-empty v-if="sourceItems.length === 0" :description="emptySourceText" />
                </div>
              </div>
              <div class="item-target">
                <div class="item-section__head">
                  <h4>已编排项</h4>
                  <span>{{ selectedItems.length }} 项，支持单项循环</span>
                </div>
                <draggable v-model="selectedItems" item-key="entryKey" class="item-list" handle=".drag-handle">
                  <template #item="{ element, index }">
                    <div class="item-card is-selected">
                      <div class="item-card__content">
                        <span class="drag-handle">::</span>
                        <span class="item-index">{{ index + 1 }}</span>
                        <span class="item-name">{{ element.name }}</span>
                        <span class="item-subtitle">{{ element.subtitle }}</span>
                      </div>
                      <div class="item-card__actions">
                        <el-input-number
                          v-model="element.itemLoopCount"
                          :min="1"
                          :max="100"
                          size="small"
                          class="item-loop-input"
                        />
                        <el-button type="danger" link @click="removeItem(index)">移除</el-button>
                      </div>
                    </div>
                  </template>
                </draggable>
                <el-empty v-if="selectedItems.length === 0" description="尚未添加任何编排项" />
              </div>
            </div>
          </div>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDialog">
          {{ dialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import draggable from 'vuedraggable'
import { useOrgStore } from '../stores/org'
import { useProjectStore } from '../stores/project'
import { useUserStore } from '../stores/user'
import {
  createProject as createProjectApi,
  updateProject,
  deleteProject as deleteProjectApi,
  runProject as runProjectApi,
} from '../api/project'
import { getOrganizationProjects } from '../api/organization'
import { getCollections } from '../api/apiTest'
import { getCasesByOrganization } from '../api/uiTest'
import { getInstances } from '../api/uiTest'

const weekdayOptions = [
  { label: '周一', value: 'MON' },
  { label: '周二', value: 'TUE' },
  { label: '周三', value: 'WED' },
  { label: '周四', value: 'THU' },
  { label: '周五', value: 'FRI' },
  { label: '周六', value: 'SAT' },
  { label: '周日', value: 'SUN' },
]

const router = useRouter()
const orgStore = useOrgStore()
const projectStore = useProjectStore()
const userStore = useUserStore()

const projects = ref([])
const instances = ref([])
const rawAvailableItems = ref([])
const selectedItems = ref([])
const itemEntrySeed = ref(0)

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const dialogMode = ref('create')

const dialogForm = reactive(createEmptyProjectForm())
const scheduleForm = reactive(createEmptyScheduleForm())

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '创建项目合集' : '编辑项目合集'))

const sourceItems = computed(() => rawAvailableItems.value)

const sourceTitle = computed(() => (dialogForm.type === 'UI' ? '可选 UI 用例' : '可选 API 用例'))

const emptySourceText = computed(() => {
  if (dialogForm.type === 'UI') {
    return '当前组织下暂无可加入的 UI 用例'
  }
  return '当前组织下暂无可加入的 API 用例'
})

const schedulePreview = computed(() => describeCron(buildCronExpression()))

watch(
  () => orgStore.currentOrganizationId,
  () => {
    fetchProjects()
    if (dialogVisible.value) {
      loadAvailableItems()
    }
  }
)

watch(
  () => dialogForm.type,
  async (type) => {
    if (!dialogVisible.value) {
      return
    }
    if (type === 'API') {
      dialogForm.uiInstanceId = null
    }
    selectedItems.value = []
    await loadAvailableItems()
  }
)

onMounted(() => {
  fetchProjects()
  fetchInstances()
})

function createEmptyProjectForm() {
  return {
    id: null,
    name: '',
    description: '',
    color: '#409EFF',
    type: 'API',
    loopCount: 1,
    uiInstanceId: null,
    enabled: true,
  }
}

function createEmptyScheduleForm() {
  return {
    mode: 'none',
    time: '09:00',
    weekdays: ['MON'],
    customCron: '',
  }
}

function resetDialogState() {
  Object.assign(dialogForm, createEmptyProjectForm())
  Object.assign(scheduleForm, createEmptyScheduleForm())
  selectedItems.value = []
  rawAvailableItems.value = []
  itemEntrySeed.value = 0
}

async function fetchProjects() {
  if (!orgStore.currentOrganizationId) {
    projects.value = []
    return
  }
  try {
    const res = await getOrganizationProjects(orgStore.currentOrganizationId)
    projects.value = res.data || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '获取合集列表失败')
    projects.value = []
  }
}

async function fetchInstances() {
  try {
    const res = await getInstances()
    instances.value = res.data || []
  } catch (error) {
    instances.value = []
  }
}

async function loadAvailableItems() {
  if (!orgStore.currentOrganizationId) {
    rawAvailableItems.value = []
    return
  }

  try {
    if (dialogForm.type === 'UI') {
      const res = await getCasesByOrganization(orgStore.currentOrganizationId)
      rawAvailableItems.value = (res.data || [])
        .map((item) => ({
          id: item.id,
          name: item.name,
          itemType: 'CASE',
          subtitle: item.moduleKey || 'UI 用例',
        }))
      return
    }

    const res = await getCollections()
    rawAvailableItems.value = flattenApiCases(res.data || [], orgStore.currentOrganizationId).map((item) => ({
      id: item.id,
      name: item.name,
      itemType: 'CASE',
      subtitle: `${item.method || 'GET'} ${item.url || ''}`.trim(),
    }))
  } catch (error) {
    rawAvailableItems.value = []
    ElMessage.error(error.response?.data?.message || '获取可编排资源失败')
  }
}

function flattenApiCases(nodes, organizationId, bucket = []) {
  nodes.forEach((node) => {
    if (!node) {
      return
    }
    if (node.nodeType === 'CASE' && node.organizationId === organizationId) {
      bucket.push(node)
    }
    if (Array.isArray(node.children) && node.children.length > 0) {
      flattenApiCases(node.children, organizationId, bucket)
    }
  })
  return bucket
}

function goBack() {
  router.push('/dashboard')
}

function goToOrganizations() {
  router.push('/organizations')
}

function isOwner(project) {
  return Number(project.ownerId) === Number(userStore.userId)
}

function enterProject(project) {
  projectStore.setCurrentProject(project.id)
  openEditDialog(project)
}

async function runProject(project) {
  try {
    await runProjectApi(project.id)
    ElMessage.success('执行指令已发送')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '执行失败')
  }
}

function openCreateDialog() {
  resetDialogState()
  dialogMode.value = 'create'
  dialogVisible.value = true
  loadAvailableItems()
}

async function openEditDialog(project) {
  resetDialogState()
  dialogMode.value = 'edit'
  dialogLoading.value = true
  dialogVisible.value = true

  Object.assign(dialogForm, {
    id: project.id,
    name: project.name,
    description: project.description || '',
    color: project.color || '#409EFF',
    type: project.type || 'API',
    loopCount: project.loopCount || 1,
    uiInstanceId: project.uiInstanceId ?? null,
    enabled: project.enabled !== false,
  })
  applyCronToSchedule(project.cronExpression || '')

  await loadAvailableItems()
  selectedItems.value = restoreSelectedItems(project.itemsJson, rawAvailableItems.value, dialogForm.type)
  dialogLoading.value = false
}

function restoreSelectedItems(itemsJson, candidates, projectType) {
  if (!itemsJson) {
    return []
  }

  try {
    const parsed = JSON.parse(itemsJson)
    if (!Array.isArray(parsed)) {
      return []
    }

    return parsed
      .map((item) => {
        if (typeof item === 'number') {
          const matched = candidates.find((candidate) => candidate.id === item)
          return createSelectedEntry(
            matched || {
              id: item,
              name: `历史资源 #${item}`,
              itemType: projectType === 'UI' ? 'CASE' : 'CASE',
              subtitle: '旧版数据',
            },
            1
          )
        }

        const itemId = Number(item.itemId ?? item.id)
        if (!itemId) {
          return null
        }
        const matched = candidates.find((candidate) => candidate.id === itemId)
        return createSelectedEntry(
          matched || {
            id: itemId,
            name: item.name || `历史资源 #${itemId}`,
            itemType: item.itemType || 'CASE',
            subtitle: '旧版数据',
          },
          Number(item.itemLoopCount || item.loopCount || 1)
        )
      })
      .filter(Boolean)
  } catch {
    return []
  }
}

function nextEntryKey() {
  itemEntrySeed.value += 1
  return `entry-${Date.now()}-${itemEntrySeed.value}`
}

function createSelectedEntry(item, itemLoopCount = 1) {
  return {
    ...item,
    entryKey: nextEntryKey(),
    itemLoopCount: Math.max(1, Number(itemLoopCount || 1)),
  }
}

function addItem(item) {
  selectedItems.value.push(createSelectedEntry(item, 1))
}

function removeItem(index) {
  selectedItems.value.splice(index, 1)
}

function buildCronExpression() {
  if (scheduleForm.mode === 'none') {
    return ''
  }

  if (scheduleForm.mode === 'custom') {
    return (scheduleForm.customCron || '').trim()
  }

  const [hour, minute] = String(scheduleForm.time || '09:00').split(':')
  const safeHour = String(Number(hour || 0))
  const safeMinute = String(Number(minute || 0))

  if (scheduleForm.mode === 'daily') {
    return `0 ${safeMinute} ${safeHour} * * ?`
  }

  if (scheduleForm.mode === 'weekly') {
    const days = scheduleForm.weekdays.join(',')
    return days ? `0 ${safeMinute} ${safeHour} ? * ${days}` : ''
  }

  return ''
}

function applyCronToSchedule(cronExpression) {
  Object.assign(scheduleForm, createEmptyScheduleForm())
  const cron = (cronExpression || '').trim()
  if (!cron) {
    return
  }

  const dailyMatch = cron.match(/^0\s+(\d{1,2})\s+(\d{1,2})\s+\*\s+\*\s+\?$/)
  if (dailyMatch) {
    scheduleForm.mode = 'daily'
    scheduleForm.time = `${pad2(dailyMatch[2])}:${pad2(dailyMatch[1])}`
    return
  }

  const weeklyMatch = cron.match(/^0\s+(\d{1,2})\s+(\d{1,2})\s+\?\s+\*\s+([A-Z,]+)$/)
  if (weeklyMatch) {
    scheduleForm.mode = 'weekly'
    scheduleForm.time = `${pad2(weeklyMatch[2])}:${pad2(weeklyMatch[1])}`
    scheduleForm.weekdays = weeklyMatch[3].split(',').filter(Boolean)
    return
  }

  scheduleForm.mode = 'custom'
  scheduleForm.customCron = cron
}

function pad2(value) {
  return String(value).padStart(2, '0')
}

function describeCron(cronExpression) {
  const cron = (cronExpression || '').trim()
  if (!cron) {
    return '未设置定时任务'
  }

  const dailyMatch = cron.match(/^0\s+(\d{1,2})\s+(\d{1,2})\s+\*\s+\*\s+\?$/)
  if (dailyMatch) {
    return `每天 ${pad2(dailyMatch[2])}:${pad2(dailyMatch[1])}`
  }

  const weeklyMatch = cron.match(/^0\s+(\d{1,2})\s+(\d{1,2})\s+\?\s+\*\s+([A-Z,]+)$/)
  if (weeklyMatch) {
    const labels = weeklyMatch[3]
      .split(',')
      .map((value) => weekdayOptions.find((item) => item.value === value)?.label || value)
      .join('、')
    return `${labels} ${pad2(weeklyMatch[2])}:${pad2(weeklyMatch[1])}`
  }

  return `自定义 Cron: ${cron}`
}

function projectItemCount(project) {
  if (!project?.itemsJson) {
    return 0
  }
  try {
    const parsed = JSON.parse(project.itemsJson)
    return Array.isArray(parsed) ? parsed.length : 0
  } catch {
    return 0
  }
}

function instanceName(instanceId) {
  if (!instanceId) {
    return '默认实例'
  }
  return instances.value.find((item) => item.id === instanceId)?.name || `实例 #${instanceId}`
}

function buildPayload() {
  const cronExpression = buildCronExpression()
  return {
    name: dialogForm.name.trim(),
    description: dialogForm.description,
    color: dialogForm.color,
    type: dialogForm.type,
    loopCount: dialogForm.loopCount,
    cronExpression,
    uiInstanceId: dialogForm.type === 'UI' ? dialogForm.uiInstanceId : null,
    enabled: dialogForm.enabled,
    itemsJson: JSON.stringify(
      selectedItems.value.map((item) => ({
        itemId: item.id,
        itemType: item.itemType,
        name: item.name,
        itemLoopCount: Math.max(1, Number(item.itemLoopCount || 1)),
      }))
    ),
  }
}

async function submitDialog() {
  if (!dialogForm.name.trim()) {
    ElMessage.warning('请输入合集名称')
    return
  }

  if (!orgStore.currentOrganizationId) {
    ElMessage.warning('请先选择组织')
    return
  }

  if (scheduleForm.mode === 'weekly' && scheduleForm.weekdays.length === 0) {
    ElMessage.warning('请选择每周执行日期')
    return
  }

  if (scheduleForm.mode !== 'none' && !buildCronExpression()) {
    ElMessage.warning('请完善定时任务配置')
    return
  }

  try {
    const payload = buildPayload()
    if (dialogMode.value === 'create') {
      await createProjectApi({
        organizationId: orgStore.currentOrganizationId,
        ...payload,
      })
      ElMessage.success('创建成功')
    } else {
      await updateProject(dialogForm.id, payload)
      ElMessage.success('保存成功')
    }

    dialogVisible.value = false
    await fetchProjects()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || (dialogMode.value === 'create' ? '创建失败' : '保存失败'))
  }
}

async function deleteProject(project) {
  try {
    await ElMessageBox.confirm(`确定要删除合集 "${project.name}" 吗？此操作不可恢复。`, '删除确认', {
      type: 'warning',
    })
    await deleteProjectApi(project.id)
    ElMessage.success('删除成功')
    fetchProjects()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.project-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
  border-radius: var(--border-radius);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, rgba(246, 250, 255, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: var(--card-shadow);
}

.project-hero__main {
  min-width: 0;
}

.hero-header {
  padding: 0;
  margin-bottom: 12px;
}

.hero-title {
  margin: 0 0 10px;
  font-size: 28px;
  line-height: 1.2;
  color: var(--text-primary);
}

.hero-desc {
  max-width: 760px;
  color: var(--text-secondary);
  line-height: 1.7;
  font-size: 14px;
}

.project-hero__side {
  display: grid;
  gap: 12px;
  min-width: 260px;
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
  font-size: 20px;
  color: var(--text-primary);
}

.no-org {
  padding: 60px 0;
}

.project-card {
  margin-bottom: 18px;
  border-radius: 18px;
  overflow: hidden;
}

.project-header {
  display: flex;
  gap: 14px;
  margin-bottom: 16px;
}

.project-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  font-weight: bold;
  flex-shrink: 0;
}

.project-info {
  flex: 1;
  min-width: 0;
}

.project-info h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  color: var(--text-primary);
}

.project-info .description {
  margin: 0 0 4px 0;
  font-size: 13px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-info .org-name {
  margin: 0;
  font-size: 12px;
  color: var(--text-tertiary);
}

.project-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.project-extra {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 16px;
  color: var(--text-secondary);
  font-size: 12px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.meta-chip--api {
  background-color: #e1f3ff;
  color: #409eff;
}

.meta-chip--ui {
  background-color: #f0f9eb;
  color: #67c23a;
}

.meta-chip--subtle {
  background: #f8fbff;
  color: var(--text-secondary);
}

.project-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
}

.type-lock {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tip {
  font-size: 12px;
  color: #909399;
}

.panel-title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.schedule-panel {
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 16px;
  background: #fafcff;
}

.schedule-mode {
  margin-bottom: 14px;
}

.schedule-fields {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.field-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.schedule-preview {
  font-size: 13px;
  color: var(--text-secondary);
}

.item-manager {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-manager__body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  min-height: 420px;
}

.item-source,
.item-target {
  display: flex;
  flex-direction: column;
  border: 1px solid #ebeef5;
  border-radius: 16px;
  padding: 14px;
  background: #fff;
}

.item-section__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--text-secondary);
}

.item-section__head h4 {
  margin: 0;
  font-size: 14px;
  color: var(--text-primary);
}

.item-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background-color: #f8f9fb;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  font-size: 14px;
}

.item-card.is-selected {
  background-color: #fff;
  border-left: 3px solid #409eff;
}

.item-card__content {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.item-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.item-index {
  font-weight: bold;
  color: #409eff;
}

.item-name {
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-loop-input {
  width: 92px;
}

.drag-handle {
  cursor: move;
  color: #94a3b8;
}

@media (max-width: 1200px) {
  .project-hero {
    flex-direction: column;
  }

  .project-hero__side {
    min-width: 0;
    grid-template-columns: 1fr 1fr;
  }

  .item-manager__body {
    grid-template-columns: 1fr;
  }
}
</style>
