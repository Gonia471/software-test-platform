<template>
  <div class="dashboard" v-loading="loading">
    <section class="hero">
      <div class="hero-main">
        <el-page-header content="仪表盘" class="page-header" />
        <div class="hero-caption">当前组织测试运营总览</div>
        <div class="hero-title-row">
          <div class="hero-org-mark" :style="{ background: currentOrgColor }">
            <el-icon><OfficeBuilding /></el-icon>
          </div>
          <div class="hero-text">
            <h2 class="hero-title">{{ scope.organizationName || '组织总览' }}</h2>
            <p class="hero-desc">
              {{ scope.organizationDescription || '围绕当前组织的资源、执行质量、近期动态与风险提醒，集中展示测试平台首页需要关注的核心信息。' }}
            </p>
          </div>
        </div>
        <div class="hero-tags">
          <el-tag effect="plain">当前组织：{{ scope.organizationName || '未选择' }}</el-tag>
          <el-tag effect="plain" type="success">负责人 {{ scope.ownerUsername || '未知' }}</el-tag>
          <el-tag effect="plain" type="info">成员 {{ scope.memberCount || 0 }}</el-tag>
          <el-tag effect="plain" :type="metrics.failedCount7d > 0 ? 'danger' : 'success'">
            近 7 天失败 {{ metrics.failedCount7d || 0 }}
          </el-tag>
        </div>
        <div class="hero-glance">
          <div v-for="item in heroGlanceItems" :key="item.key" class="hero-glance__item">
            <span class="hero-glance__label">{{ item.label }}</span>
            <strong class="hero-glance__value">{{ item.value }}</strong>
            <span class="hero-glance__hint">{{ item.hint }}</span>
          </div>
        </div>
      </div>
      <div class="hero-side">
        <div class="hero-side-card">
          <div class="hero-side__header">
            <span class="hero-side__label">当前组织状态</span>
            <span class="hero-side__badge" :class="`is-${dashboardStatusTone}`">{{ dashboardStatusLabel }}</span>
          </div>
          <strong class="hero-side__value">{{ metrics.successRate7d || 0 }}%</strong>
          <span class="hero-side__tip">{{ dashboardStatusDescription }}</span>
          <div class="hero-side__stats">
            <div class="hero-side__stat">
              <span>执行次数</span>
              <strong>{{ metrics.executionCount7d || 0 }}</strong>
            </div>
            <div class="hero-side__stat">
              <span>失败次数</span>
              <strong>{{ metrics.failedCount7d || 0 }}</strong>
            </div>
          </div>
        </div>
        <div class="hero-side-card hero-side-card--secondary">
          <span class="hero-side__label">自动化资源概况</span>
          <strong class="hero-side__value hero-side__value--small">{{ totalCaseCount }}</strong>
          <span class="hero-side__tip">当前组织累计沉淀的 API / UI 用例总量</span>
          <div class="hero-side__stats">
            <div class="hero-side__stat">
              <span>启用合集</span>
              <strong>{{ resourceHealth.enabledProjectCount || 0 }}</strong>
            </div>
            <div class="hero-side__stat">
              <span>最近执行</span>
              <strong>{{ shortDateTime(metrics.latestExecutionAt) }}</strong>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="metric-grid">
      <el-card v-for="item in metricCards" :key="item.key" shadow="hover" class="metric-card" :class="item.className">
        <div class="metric-card__icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div class="metric-card__meta">
          <span class="metric-card__label">{{ item.label }}</span>
          <strong class="metric-card__value">{{ item.value }}</strong>
          <span class="metric-card__hint">{{ item.hint }}</span>
        </div>
      </el-card>
    </section>

    <section class="dashboard-grid dashboard-grid--primary">
      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="panel-head">
            <span>资源构成</span>
            <span class="panel-tip">当前组织自动化资产与配置概况</span>
          </div>
        </template>
        <div class="resource-health">
          <div class="resource-health__main">
            <div class="resource-figure">
              <span class="resource-figure__label">合集总数</span>
              <strong class="resource-figure__value">{{ metrics.projectCount || 0 }}</strong>
            </div>
            <div class="resource-pills">
              <span class="resource-pill">API 合集 {{ resourceHealth.apiProjectCount || 0 }}</span>
              <span class="resource-pill">UI 合集 {{ resourceHealth.uiProjectCount || 0 }}</span>
              <span class="resource-pill">启用定时 {{ metrics.scheduledProjectCount || 0 }}</span>
            </div>
          </div>
          <div class="resource-overview">
            <div v-for="item in resourceOverviewItems" :key="item.key" class="resource-overview__item">
              <div class="resource-overview__icon" :class="item.className">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div class="resource-overview__content">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <p>{{ item.hint }}</p>
              </div>
            </div>
          </div>
          <div class="resource-bars">
            <div v-for="item in resourceBars" :key="item.key" class="resource-bar">
              <div class="resource-bar__meta">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
              <div class="resource-bar__track">
                <div class="resource-bar__fill" :style="{ width: `${item.percent}%` }"></div>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="panel-head">
            <span>执行质量</span>
            <span class="panel-tip">近 7 天组织执行质量与风险</span>
          </div>
        </template>
        <div class="quality-panel">
          <div class="quality-summary">
            <div class="quality-score" :class="`is-${dashboardStatusTone}`">
              <span class="quality-score__label">成功率</span>
              <strong class="quality-score__value">{{ metrics.successRate7d || 0 }}%</strong>
              <span class="quality-score__desc">{{ dashboardStatusDescription }}</span>
            </div>
            <div class="quality-insight">
              <div class="quality-insight__row">
                <span>最近执行时间</span>
                <strong>{{ formatDateTime(metrics.latestExecutionAt) }}</strong>
              </div>
              <div class="quality-insight__row">
                <span>风险判断</span>
                <strong>{{ dashboardStatusLabel }}</strong>
              </div>
              <div class="quality-insight__row">
                <span>运行中 / 待执行</span>
                <strong>{{ resourceHealth.runningCount || 0 }}</strong>
              </div>
            </div>
          </div>
          <div class="quality-grid">
            <div class="quality-item">
              <span class="quality-item__label">API 执行</span>
              <strong class="quality-item__value">{{ resourceHealth.apiExecutionCount7d || 0 }}</strong>
            </div>
            <div class="quality-item">
              <span class="quality-item__label">UI 执行</span>
              <strong class="quality-item__value">{{ resourceHealth.uiExecutionCount7d || 0 }}</strong>
            </div>
            <div class="quality-item">
              <span class="quality-item__label">通过</span>
              <strong class="quality-item__value quality-item__value--success">{{ resourceHealth.passedCount7d || 0 }}</strong>
            </div>
            <div class="quality-item">
              <span class="quality-item__label">失败</span>
              <strong class="quality-item__value quality-item__value--danger">{{ resourceHealth.failedCount7d || 0 }}</strong>
            </div>
          </div>
          <div class="quality-highlights">
            <div v-for="item in qualityHighlights" :key="item.label" class="quality-highlight">
              <span class="quality-highlight__label">{{ item.label }}</span>
              <strong class="quality-highlight__value">{{ item.value }}</strong>
              <span class="quality-highlight__hint">{{ item.hint }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </section>

  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Connection,
  FolderOpened,
  Histogram,
  Monitor,
  OfficeBuilding,
  User,
  WarningFilled,
} from '@element-plus/icons-vue'
import { useOrgStore } from '../stores/org'
import { getDashboardOverview } from '../api/dashboard'

const orgStore = useOrgStore()
const loading = ref(false)
const overview = reactive({
  scope: {},
  metrics: {},
  resourceHealth: {},
})

const scope = computed(() => overview.scope || {})
const metrics = computed(() => overview.metrics || {})
const resourceHealth = computed(() => overview.resourceHealth || {})
const currentOrgColor = computed(() => scope.value.organizationColor || 'linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%)')
const totalCaseCount = computed(() => Number(metrics.value.apiCaseCount || 0) + Number(metrics.value.uiCaseCount || 0))

const dashboardStatusTone = computed(() => {
  const executionCount = Number(metrics.value.executionCount7d || 0)
  const failedCount = Number(metrics.value.failedCount7d || 0)
  const successRate = Number(metrics.value.successRate7d || 0)
  if (!executionCount) return 'info'
  if (failedCount >= 3 || successRate < 70) return 'danger'
  if (failedCount > 0 || successRate < 90) return 'warning'
  return 'success'
})

const dashboardStatusLabel = computed(() => {
  if (dashboardStatusTone.value === 'danger') return '需重点关注'
  if (dashboardStatusTone.value === 'warning') return '运行有波动'
  if (dashboardStatusTone.value === 'success') return '运行稳定'
  return '等待数据沉淀'
})

const dashboardStatusDescription = computed(() => {
  const executionCount = Number(metrics.value.executionCount7d || 0)
  if (!executionCount) return '当前组织近 7 天还没有执行数据，建议先完成一次回归执行。'
  if (dashboardStatusTone.value === 'danger') return '近期失败较多，建议优先排查异常环境、断言或脚本问题。'
  if (dashboardStatusTone.value === 'warning') return '近期已有执行沉淀，但成功率仍有优化空间。'
  return '近 7 天执行表现稳定，可继续扩大自动化覆盖范围。'
})

const heroGlanceItems = computed(() => ([
  {
    key: 'collections',
    label: '合集规模',
    value: metrics.value.projectCount || 0,
    hint: `启用中 ${resourceHealth.value.enabledProjectCount || 0}`,
  },
  {
    key: 'api-assets',
    label: '接口资产',
    value: metrics.value.apiCaseCount || 0,
    hint: `目录 ${resourceHealth.value.apiFolderCount || 0}`,
  },
  {
    key: 'ui-assets',
    label: '界面资产',
    value: metrics.value.uiCaseCount || 0,
    hint: '当前组织 UI 用例沉淀',
  },
  {
    key: 'schedule',
    label: '定时能力',
    value: metrics.value.scheduledProjectCount || 0,
    hint: '已配置定时合集',
  },
]))

const metricCards = computed(() => ([
  {
    key: 'api-cases',
    label: 'API 用例',
    value: metrics.value.apiCaseCount || 0,
    hint: '当前组织接口用例总数',
    icon: Connection,
    className: 'is-blue',
  },
  {
    key: 'ui-cases',
    label: 'UI 用例',
    value: metrics.value.uiCaseCount || 0,
    hint: '当前组织自动化用例总数',
    icon: Monitor,
    className: 'is-cyan',
  },
  {
    key: 'projects',
    label: '项目合集',
    value: metrics.value.projectCount || 0,
    hint: `其中定时合集 ${metrics.value.scheduledProjectCount || 0}`,
    icon: FolderOpened,
    className: 'is-violet',
  },
  {
    key: 'members',
    label: '组织成员',
    value: scope.value.memberCount || 0,
    hint: '当前组织协作人数',
    icon: User,
    className: 'is-green',
  },
  {
    key: 'executions',
    label: '近 7 天执行',
    value: metrics.value.executionCount7d || 0,
    hint: `成功率 ${metrics.value.successRate7d || 0}%`,
    icon: Histogram,
    className: 'is-amber',
  },
  {
    key: 'failed',
    label: '近 7 天失败',
    value: metrics.value.failedCount7d || 0,
    hint: '建议及时查看测试报告',
    icon: WarningFilled,
    className: 'is-rose',
  },
]))

const resourceBars = computed(() => {
  const entries = [
    { key: 'apiProjectCount', label: 'API 合集', value: Number(resourceHealth.value.apiProjectCount || 0) },
    { key: 'uiProjectCount', label: 'UI 合集', value: Number(resourceHealth.value.uiProjectCount || 0) },
    { key: 'apiFolderCount', label: 'API 目录', value: Number(resourceHealth.value.apiFolderCount || 0) },
    { key: 'apiCaseCount', label: 'API 用例', value: Number(resourceHealth.value.apiCaseCount || 0) },
    { key: 'uiCaseCount', label: 'UI 用例', value: Number(resourceHealth.value.uiCaseCount || 0) },
    { key: 'enabledProjectCount', label: '已启用合集', value: Number(resourceHealth.value.enabledProjectCount || 0) },
  ]
  const max = Math.max(...entries.map(item => item.value), 1)
  return entries.map(item => ({
    ...item,
    percent: Math.round((item.value / max) * 100),
  }))
})

const resourceOverviewItems = computed(() => ([
  {
    key: 'api',
    label: 'API 资产',
    value: `${metrics.value.apiCaseCount || 0} 个用例`,
    hint: `覆盖 ${resourceHealth.value.apiFolderCount || 0} 个目录，适合接口回归与单接口调试`,
    icon: Connection,
    className: 'is-blue',
  },
  {
    key: 'ui',
    label: 'UI 资产',
    value: `${metrics.value.uiCaseCount || 0} 个用例`,
    hint: '覆盖页面流程与业务操作，适合端到端回归场景',
    icon: Monitor,
    className: 'is-cyan',
  },
  {
    key: 'project',
    label: '合集与调度',
    value: `${metrics.value.projectCount || 0} 个合集`,
    hint: `其中 ${metrics.value.scheduledProjectCount || 0} 个已接入定时任务`,
    icon: FolderOpened,
    className: 'is-violet',
  },
]))

const qualityHighlights = computed(() => ([
  {
    label: '执行覆盖',
    value: `${metrics.value.executionCount7d || 0} 次`,
    hint: '近 7 天组织执行总次数',
  },
  {
    label: '失败风险',
    value: `${metrics.value.failedCount7d || 0} 次`,
    hint: Number(metrics.value.failedCount7d || 0) > 0 ? '建议及时查看失败记录' : '近期未发现失败执行',
  },
  {
    label: '用例沉淀',
    value: `${totalCaseCount.value} 个`,
    hint: '当前组织累计可执行用例规模',
  },
]))

async function loadOverview() {
  loading.value = true
  try {
    const res = await getDashboardOverview({
      organizationId: orgStore.currentOrganizationId || undefined,
    })
    Object.assign(overview, {
      scope: res.data?.scope || {},
      metrics: res.data?.metrics || {},
      resourceHealth: res.data?.resourceHealth || {},
    })
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载仪表盘失败')
  } finally {
    loading.value = false
  }
}

function formatDateTime(value) {
  if (!value) return '暂无记录'
  return new Date(value).toLocaleString()
}

function shortDateTime(value) {
  if (!value) return '暂无'
  return new Date(value).toLocaleDateString()
}

onMounted(async () => {
  if (!orgStore.organizations.length) {
    await orgStore.fetchOrganizations()
  }
  await loadOverview()
})

watch(
  () => orgStore.currentOrganizationId,
  async (value, oldValue) => {
    if (value === oldValue) return
    await loadOverview()
  },
)
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 0.8fr);
  gap: 18px;
  padding: 24px;
  border-radius: var(--border-radius);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(244, 248, 255, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: var(--card-shadow);
}

.page-header {
  padding: 0;
  margin-bottom: 12px;
}

.hero-caption {
  margin-bottom: 12px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-title-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.hero-org-mark {
  width: 58px;
  height: 58px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  flex-shrink: 0;
  box-shadow: 0 16px 30px rgba(59, 130, 246, 0.22);
}

.hero-text {
  min-width: 0;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 30px;
  line-height: 1.2;
  color: var(--text-primary);
}

.hero-desc {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.8;
  max-width: 760px;
}

.hero-tags {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-glance {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.hero-glance__item {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(226, 232, 240, 0.92);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-glance__label {
  color: var(--text-secondary);
  font-size: 12px;
}

.hero-glance__value {
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 700;
}

.hero-glance__hint {
  color: var(--text-tertiary);
  font-size: 12px;
}

.hero-side {
  display: grid;
  gap: 12px;
}

.hero-side-card {
  border-radius: 18px;
  padding: 20px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 132px;
}

.hero-side-card--secondary {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.hero-side__label {
  font-size: 12px;
  opacity: 0.86;
}

.hero-side__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hero-side__badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.16);
}

.hero-side__badge.is-success {
  background: rgba(16, 185, 129, 0.18);
}

.hero-side__badge.is-warning {
  background: rgba(245, 158, 11, 0.22);
}

.hero-side__badge.is-danger {
  background: rgba(239, 68, 68, 0.22);
}

.hero-side__badge.is-info {
  background: rgba(148, 163, 184, 0.22);
}

.hero-side__value {
  font-size: 34px;
  font-weight: 700;
}

.hero-side__value--small {
  font-size: 20px;
  line-height: 1.4;
}

.hero-side__tip {
  font-size: 12px;
  opacity: 0.78;
}

.hero-side__stats {
  margin-top: auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.hero-side__stat {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hero-side__stat span {
  font-size: 11px;
  opacity: 0.82;
}

.hero-side__stat strong {
  font-size: 16px;
  font-weight: 700;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  min-height: 144px;
}

.metric-card::before {
  content: '';
  position: absolute;
  inset: 0 auto auto 0;
  width: 100%;
  height: 4px;
}

.metric-card.is-blue::before {
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
}

.metric-card.is-cyan::before {
  background: linear-gradient(135deg, #0891b2 0%, #22d3ee 100%);
}

.metric-card.is-violet::before {
  background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%);
}

.metric-card.is-green::before {
  background: linear-gradient(135deg, #059669 0%, #34d399 100%);
}

.metric-card.is-amber::before {
  background: linear-gradient(135deg, #d97706 0%, #fbbf24 100%);
}

.metric-card.is-rose::before {
  background: linear-gradient(135deg, #e11d48 0%, #fb7185 100%);
}

.metric-card__icon {
  position: absolute;
  top: 18px;
  right: 16px;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: rgba(59, 130, 246, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  font-size: 20px;
}

.metric-card__meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-card__label {
  color: var(--text-secondary);
  font-size: 13px;
}

.metric-card__value {
  color: var(--text-primary);
  font-size: 32px;
  font-weight: 700;
}

.metric-card__hint {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
  max-width: 90%;
}

.dashboard-grid {
  display: grid;
  gap: 18px;
}

.dashboard-grid--primary {
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
}

.panel-card {
  height: 100%;
  border-radius: 18px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-tip {
  color: var(--text-tertiary);
  font-size: 12px;
}

.resource-health {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.resource-health__main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 18px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fbff 100%);
  border-radius: 16px;
  border: 1px solid rgba(191, 219, 254, 0.9);
}

.resource-figure {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.resource-figure__label {
  font-size: 13px;
  color: var(--text-secondary);
}

.resource-figure__value {
  font-size: 30px;
  font-weight: 700;
  color: var(--text-primary);
}

.resource-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.resource-pill {
  padding: 8px 12px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.95);
  color: var(--text-secondary);
  font-size: 12px;
}

.resource-bars {
  display: grid;
  gap: 12px;
}

.resource-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.resource-overview__item {
  padding: 16px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.95);
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.resource-overview__icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.resource-overview__icon.is-blue {
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
}

.resource-overview__icon.is-cyan {
  background: rgba(6, 182, 212, 0.12);
  color: #0891b2;
}

.resource-overview__icon.is-violet {
  background: rgba(124, 58, 237, 0.12);
  color: #7c3aed;
}

.resource-overview__content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.resource-overview__content span {
  color: var(--text-secondary);
  font-size: 12px;
}

.resource-overview__content strong {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
}

.resource-overview__content p {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}

.resource-bar__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.resource-bar__meta strong {
  color: var(--text-primary);
}

.resource-bar__track {
  height: 10px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.88);
  overflow: hidden;
}

.resource-bar__fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
}

.quality-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.quality-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.quality-summary {
  display: grid;
  grid-template-columns: minmax(180px, 0.8fr) minmax(0, 1fr);
  gap: 14px;
}

.quality-score,
.quality-insight,
.quality-highlight,
.quality-item {
  padding: 18px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.quality-score {
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

.quality-score.is-success {
  background: linear-gradient(135deg, rgba(236, 253, 245, 0.96) 0%, rgba(255, 255, 255, 0.98) 100%);
  border-color: rgba(16, 185, 129, 0.22);
}

.quality-score.is-warning {
  background: linear-gradient(135deg, rgba(255, 251, 235, 0.96) 0%, rgba(255, 255, 255, 0.98) 100%);
  border-color: rgba(245, 158, 11, 0.24);
}

.quality-score.is-danger {
  background: linear-gradient(135deg, rgba(254, 242, 242, 0.96) 0%, rgba(255, 255, 255, 0.98) 100%);
  border-color: rgba(239, 68, 68, 0.24);
}

.quality-score.is-info {
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.98) 0%, rgba(255, 255, 255, 0.98) 100%);
  border-color: rgba(59, 130, 246, 0.18);
}

.quality-score__label {
  color: var(--text-secondary);
  font-size: 12px;
}

.quality-score__value {
  color: var(--text-primary);
  font-size: 38px;
  font-weight: 700;
}

.quality-score__desc {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.quality-insight {
  display: grid;
  gap: 10px;
}

.quality-insight__row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  font-size: 13px;
}

.quality-insight__row span {
  color: var(--text-secondary);
}

.quality-insight__row strong {
  color: var(--text-primary);
  text-align: right;
}

.quality-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quality-item--wide {
  grid-column: 1 / -1;
}

.quality-item__label {
  font-size: 12px;
  color: var(--text-secondary);
}

.quality-item__value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.quality-item__value--success {
  color: #059669;
}

.quality-item__value--danger {
  color: #dc2626;
}

.quality-item__value--warning {
  color: #d97706;
}

.quality-highlights {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.quality-highlight {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quality-highlight__label {
  color: var(--text-secondary);
  font-size: 12px;
}

.quality-highlight__value {
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 700;
}

.quality-highlight__hint {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 1440px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .hero-glance,
  .resource-overview,
  .quality-highlights {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .hero,
  .dashboard-grid--primary {
    grid-template-columns: 1fr;
  }

  .quality-summary {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .hero-glance,
  .resource-overview,
  .quality-highlights,
  .resource-health__main {
    grid-template-columns: 1fr;
    display: grid;
  }

  .hero-side__stats {
    grid-template-columns: 1fr;
  }
}
</style>
