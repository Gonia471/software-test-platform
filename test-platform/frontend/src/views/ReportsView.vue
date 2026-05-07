<template>
  <div class="page">
    <el-page-header content="测试报告" class="page-header" />

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
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="uiStatusType(row.status)" size="small">{{ row.status }}</el-tag>
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
        <el-empty description="接口测试报告将后续接入" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import UiExecutionReportPanel from '../components/ui-test/UiExecutionReportPanel.vue'
import { listExecutions, getExecutionDetail } from '../api/uiTest'

const route = useRoute()
const router = useRouter()

const activeTab = ref('ui')
const uiListLoading = ref(false)
const uiDetailLoading = ref(false)
const uiExecutions = ref([])
const selectedUiId = ref(null)
const uiDetail = ref(null)

function uiStatusType(status) {
  if (status === 'PASSED') return 'success'
  if (status === 'FAILED' || status === 'STOPPED') return 'danger'
  return 'info'
}

function formatTime(iso) {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return String(iso)
  return d.toLocaleString('zh-CN', { hour12: false })
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
)

onMounted(() => {
  loadUiList().then(() => {
    const q = route.query.uiExecution
    const id = q ? Number(q) : null
    if (id && !Number.isNaN(id)) {
      selectedUiId.value = id
      loadUiDetail(id)
    }
  })
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-header {
  padding: 0;
}

.report-tabs :deep(.el-tabs__content) {
  padding-top: 8px;
}

.ui-report-layout {
  display: grid;
  grid-template-columns: minmax(320px, 38%) 1fr;
  gap: 16px;
  align-items: start;
}

@media (max-width: 960px) {
  .ui-report-layout {
    grid-template-columns: 1fr;
  }
}

.list-card,
.detail-card {
  border-radius: 10px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-body {
  min-height: 200px;
}

.detail-body {
  min-height: 320px;
}

:deep(.el-table__body tr.ui-exec-row-selected > td.el-table__cell) {
  background-color: #eef2ff !important;
}
</style>
