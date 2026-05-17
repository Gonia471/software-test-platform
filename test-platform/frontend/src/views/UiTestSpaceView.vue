<template>
  <div class="ui-space-page">
    <section class="space-hero">
      <div class="space-hero__main">
        <el-page-header
          content="UI 测试用例空间"
          class="page-header"
          @back="goBack"
        />
        <p class="space-hero__desc">
          统一管理当前组织下的 UI 自动化用例，按分类快速筛选，进入编排页继续配置步骤。
        </p>
      </div>
      <div class="space-hero__stats">
        <div class="hero-stat">
          <span class="hero-stat__label">用例总数</span>
          <strong class="hero-stat__value">{{ totalCases }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-stat__label">当前筛选</span>
          <strong class="hero-stat__value">{{ filteredCasesCount }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-stat__label">已选项目</span>
          <strong class="hero-stat__value">{{ selectedRows.length }}</strong>
        </div>
      </div>
    </section>

    <el-card shadow="never" class="space-card">
      <div class="space-header">
        <div class="space-header-left">
          <div class="field">
            <span class="label">用例分类</span>
            <el-select
              v-model="selectedModuleKey"
              placeholder="全部"
              size="small"
              style="width: 200px"
              clearable
            >
              <el-option label="全部" value="all" />
              <el-option
                v-for="m in uiStore.modules"
                :key="m.key"
                :label="m.name"
                :value="m.key"
              />
            </el-select>
          </div>
          <el-button
            plain
            class="module-create-trigger"
            @click="openCreateModuleDialog"
            :disabled="!orgStore.currentOrganizationId"
          >
            新建分类
          </el-button>
          <el-button
            plain
            class="module-create-trigger"
            @click="openManageModuleDialog"
            :disabled="!orgStore.currentOrganizationId"
          >
            管理分类
          </el-button>
        </div>
        <div class="space-header-right">
          <div class="filter-summary">
            <span class="filter-chip">{{ currentOrganizationLabel }}</span>
            <span class="filter-chip">{{ activeModuleLabel }}</span>
          </div>
          <el-button
            v-if="selectedRows.length > 0"
            type="danger"
            plain
            @click="onBatchDelete"
          >
            批量删除 ({{ selectedRows.length }})
          </el-button>
          <el-button
            type="primary"
            @click="onCreateCase"
          >
            新建 UI 用例
          </el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        :data="pagedData"
        size="small"
        stripe
        class="case-table"
        @sort-change="onSortChange"
        @selection-change="onSelectionChange"
      >
        <el-table-column
          type="selection"
          width="48"
        />
        <el-table-column
          prop="seq"
          label="序号"
          width="84"
          sortable
          align="center"
        />
        <el-table-column
          prop="name"
          label="用例名称"
          min-width="240"
          show-overflow-tooltip
          sortable
        >
          <template #default="{ row }">
            <div class="case-name-cell">
              <span class="case-name">{{ row.name }}</span>
              <span class="case-updated">更新于 {{ row.updatedAtDisplay }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="moduleName"
          label="分类"
          width="120"
          :filters="moduleFilters"
          :filter-method="onModuleFilter"
        >
          <template #default="{ row }">
            <span class="table-tag table-tag--module">{{ row.moduleName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="creator"
          label="创建人"
          width="120"
          :filters="creatorFilters"
          :filter-method="onCreatorFilter"
        >
          <template #default="{ row }">
            <span class="creator-pill">{{ row.creator || '未填写' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="updatedAtDisplay"
          label="更新时间"
          width="178"
          sortable
        />
        <el-table-column
          label="操作"
          width="208"
          fixed="right"
        >
          <template #default="{ row }">
            <div class="op-row">
              <el-button
                type="primary"
                plain
                @click="enterCase(row)"
              >
                编排
              </el-button>
              <el-button
                type="info"
                plain
                @click="duplicateCase(row)"
              >
                复制
              </el-button>
              <el-popconfirm
                title="确定删除？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                confirm-button-type="danger"
                @confirm="deleteCase(row)"
              >
                <template #reference>
                  <el-button
                    type="danger"
                    plain
                  >
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="table-footer__hint">支持排序、筛选与批量管理，页面风格已统一为简洁卡片式布局。</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20]"
          :total="tableData.length"
          layout="total, sizes, prev, pager, next"
          small
        />
      </div>
    </el-card>

    <el-dialog
      v-model="createDialogVisible"
      title="新建 UI 用例"
      width="500px"
      class="create-dialog"
    >
      <el-form label-width="90px" size="small" class="create-form">
        <el-form-item label="用例序号">
          <el-input
            :value="uiStore.nextSeq"
            style="width: 260px"
            disabled
          />
        </el-form-item>
        <el-form-item label="所属组织">
          <el-input
            :value="currentOrganizationLabel"
            style="width: 260px"
            disabled
          />
        </el-form-item>
        <el-form-item label="用例分类" required>
          <el-select
            v-model="newCaseModuleKey"
            placeholder="请选择用例分类"
            style="width: 260px"
          >
            <el-option
              v-for="m in uiStore.modules"
              :key="m.key"
              :label="m.name"
              :value="m.key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="创建人">
          <el-input
            v-model="newCaseCreator"
            style="width: 260px"
            disabled
          />
        </el-form-item>
        <el-form-item label="用例标题" required>
          <el-input
            v-model="newCaseTitle"
            style="width: 260px"
            placeholder="请输入用例标题"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmCreateCase">
            确认创建
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="createModuleDialogVisible"
      title="新建用例分类"
      width="420px"
      class="create-dialog"
    >
      <el-form label-width="90px" size="small" class="create-form create-form--module">
        <el-form-item label="所属组织">
          <el-input
            :value="currentOrganizationLabel"
            class="form-control"
            disabled
          />
        </el-form-item>
        <el-form-item label="分类名称" required>
          <el-input
            v-model="newModuleName"
            class="form-control"
            maxlength="50"
            placeholder="例如：登录流程、订单中心、核心回归"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createModuleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmCreateModule">
            确认创建
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="manageModuleDialogVisible"
      title="分类管理"
      width="560px"
      class="create-dialog"
    >
      <div class="module-manage-panel">
        <div
          v-for="module in uiStore.modules"
          :key="module.id || module.key"
          class="module-manage-item"
        >
          <div class="module-manage-item__meta">
            <span class="module-manage-item__name">{{ module.name }}</span>
            <span class="module-manage-item__key">{{ module.key }}</span>
            <span class="module-manage-item__count">关联用例 {{ module.caseCount || 0 }}</span>
          </div>
          <div class="module-manage-item__actions">
            <el-button plain size="small" @click="renameModule(module)">
              重命名
            </el-button>
            <el-button
              plain
              size="small"
              type="danger"
              :disabled="!module.deletable"
              @click="removeModule(module)"
            >
              删除
            </el-button>
          </div>
        </div>
        <el-empty
          v-if="!uiStore.modules.length"
          description="当前组织下暂无用例分类"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, nextTick, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useUiTestStore } from '../stores/uiTest'
import { useOrgStore } from '../stores/org'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const uiStore = useUiTestStore()
const orgStore = useOrgStore()

const tableRef = ref(null)
const selectedRows = ref([])
const createDialogVisible = ref(false)
const createModuleDialogVisible = ref(false)
const manageModuleDialogVisible = ref(false)
const newCaseModuleKey = ref('')
const newCaseCreator = ref('')
const newCaseTitle = ref('')
const newModuleName = ref('')

const currentPage = ref(1)
const pageSize = ref(10)

// 在组件挂载时获取用例数据和组织数据
onMounted(async () => {
  try {
    await orgStore.fetchOrganizations()
    await uiStore.fetchCases()
  } catch (err) {
    console.error('初始化 UI 用例空间失败:', err)
    ElMessage.error(err.message || '加载 UI 用例页面失败')
  }
})

watch(
  () => orgStore.currentOrganizationId,
  async (orgId, previousOrgId) => {
    if (!orgId || orgId === previousOrgId) {
      return
    }
    selectedRows.value = []
    tableRef.value?.clearSelection()
    currentPage.value = 1
    await uiStore.fetchCases(orgId)
  },
)

const selectedModuleKey = computed({
  get: () => uiStore.selectedModuleKey,
  set: (val) => uiStore.setModule(val),
})

const moduleNameMap = computed(() =>
  Object.fromEntries(uiStore.modules.map((module) => [module.key, module.name])),
)

const tableData = computed(() => {
  return (uiStore.filteredCases || []).map((c) => ({
    ...c,
    seq: c.id,
    moduleName: moduleNameMap.value[c.moduleKey] || c.moduleKey || uiStore.modules[0]?.name || '',
    updatedAtDisplay: c.updatedAt
      ? new Date(c.updatedAt).toLocaleString()
      : '-',
  }))
})

const totalCases = computed(() => uiStore.cases.length)
const filteredCasesCount = computed(() => tableData.value.length)
const currentOrganizationLabel = computed(() => orgStore.currentOrganization?.name || '未选择组织')
const activeModuleLabel = computed(() => {
  if (!selectedModuleKey.value || selectedModuleKey.value === 'all') return '全部分类'
  return moduleNameMap.value[selectedModuleKey.value] || selectedModuleKey.value
})

const sortState = ref({
  prop: 'seq',
  order: 'ascending',
})

const sortedData = computed(() => {
  const data = [...tableData.value]
  const { prop, order } = sortState.value
  if (!order || !prop) return data

  const factor = order === 'ascending' ? 1 : -1

  data.sort((a, b) => {
    const va = a[prop]
    const vb = b[prop]

    if (va == null && vb == null) return 0
    if (va == null) return -1 * factor
    if (vb == null) return 1 * factor

    if (typeof va === 'number' && typeof vb === 'number') {
      return (va - vb) * factor
    }

    const sa = String(va)
    const sb = String(vb)
    if (sa === sb) return 0
    return sa > sb ? factor : -factor
  })

  return data
})

const creatorFilters = computed(() => {
  const set = new Set(uiStore.filteredCases.map((c) => c.creator || ''))
  return Array.from(set)
    .filter(Boolean)
    .map((name) => ({ text: name, value: name }))
})

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return sortedData.value.slice(start, end)
})

function ensureTeamAndModule() {
  const moduleKey = selectedModuleKey.value === 'all'
    ? uiStore.modules[0]?.key
    : selectedModuleKey.value
  return { moduleKey }
}

function onCreateCase() {
  if (!uiStore.modules.length) {
    ElMessage.warning('请先创建用例分类')
    openCreateModuleDialog()
    return
  }
  const { moduleKey } = ensureTeamAndModule()
  newCaseModuleKey.value = selectedModuleKey.value === 'all' ? '' : selectedModuleKey.value
  if (!newCaseModuleKey.value) {
    newCaseModuleKey.value = moduleKey || ''
  }
  newCaseCreator.value = userStore.username || '未命名用户'
  newCaseTitle.value = ''
  createDialogVisible.value = true
}

function openCreateModuleDialog() {
  if (!orgStore.currentOrganizationId) {
    ElMessage.warning('请先选择所属组织')
    return
  }
  newModuleName.value = ''
  createModuleDialogVisible.value = true
}

function openManageModuleDialog() {
  if (!orgStore.currentOrganizationId) {
    ElMessage.warning('请先选择所属组织')
    return
  }
  manageModuleDialogVisible.value = true
}

async function confirmCreateModule() {
  const moduleName = String(newModuleName.value || '').trim()
  if (!moduleName) {
    ElMessage.error('请输入分类名称')
    return
  }
  if (uiStore.modules.some(item => item.key === moduleName)) {
    ElMessage.warning('该分类已存在')
    return
  }

  try {
    await uiStore.addModule(moduleName, orgStore.currentOrganizationId)
    selectedModuleKey.value = moduleName
    newCaseModuleKey.value = moduleName
    createModuleDialogVisible.value = false
    ElMessage.success('分类创建成功')
  } catch (err) {
    ElMessage.error(err.message || '创建分类失败')
  }
}

async function renameModule(module) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的分类名称', '重命名分类', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputValue: module.name,
      inputPlaceholder: '请输入分类名称',
      inputValidator: (value) => {
        if (!String(value || '').trim()) {
          return '分类名称不能为空'
        }
        return true
      },
    })

    const nextName = String(value || '').trim()
    if (!nextName || nextName === module.name) {
      return
    }

    await uiStore.renameModule(module.id, nextName, orgStore.currentOrganizationId)
    ElMessage.success('分类重命名成功')
  } catch (err) {
    if (err === 'cancel') return
    ElMessage.error(err.message || '分类重命名失败')
  }
}

async function removeModule(module) {
  if (!module.deletable) {
    ElMessage.warning('该分类下仍有关联用例，无法删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定删除分类“${module.name}”吗？`,
      '删除分类确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await uiStore.removeModule(module.id, orgStore.currentOrganizationId)
    if (selectedModuleKey.value === module.key) {
      selectedModuleKey.value = 'all'
    }
    ElMessage.success('分类删除成功')
  } catch (err) {
    if (err === 'cancel') return
    ElMessage.error(err.message || '删除分类失败')
  }
}

async function confirmCreateCase() {
  if (!orgStore.currentOrganizationId) {
    ElMessage.error('请先选择所属组织')
    return
  }
  if (!newCaseTitle.value || !newCaseTitle.value.trim()) {
    ElMessage.error('请输入用例标题')
    return
  }
  if (!newCaseModuleKey.value) {
    ElMessage.error('请选择用例分类')
    return
  }

  try {
    const newCase = await uiStore.createCase({
      name: newCaseTitle.value.trim(),
      description: '',
      moduleKey: newCaseModuleKey.value,
      organizationId: Number(orgStore.currentOrganizationId),
      steps: []
    })

    createDialogVisible.value = false
    ElMessage.success('用例创建成功')
    
    // 等待数据刷新后聚焦到新用例
    setTimeout(() => {
      focusCaseRow(newCase)
    }, 500)
  } catch (err) {
    ElMessage.error('创建用例失败: ' + (err.message || '未知错误'))
  }
}

function enterCase(row) {
  router.push({
    path: `/ui-test/cases/${row.id}`,
  })
}

async function duplicateCase(row) {
  try {
    // 首先获取要复制的用例详情
    const caseDetail = await uiStore.getCaseById(row.id)
    
    // 创建复制后的用例
    const copy = await uiStore.createCase({
      name: `${caseDetail.name}（复制）`,
      description: caseDetail.description,
      moduleKey: caseDetail.moduleKey,
      organizationId: caseDetail.organizationId,
      steps: caseDetail.steps || []
    })
    
    ElMessage.success(`已复制用例：${copy.name}`)
    
    // 等待数据刷新后聚焦到新用例
    setTimeout(() => {
      focusCaseRow(copy)
    }, 500)
  } catch (err) {
    ElMessage.error('复制用例失败: ' + (err.message || '未知错误'))
  }
}

async function deleteCase(row) {
  try {
    await uiStore.removeCase(row.id)
    ElMessage.success('用例删除成功')
  } catch (err) {
    ElMessage.error('删除用例失败: ' + (err.message || '未知错误'))
  }
}

function onSelectionChange(selection) {
  selectedRows.value = selection
}

async function onBatchDelete() {
  if (!selectedRows.value.length) return
  const count = selectedRows.value.length
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${count} 个用例吗？此操作不可恢复。`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    // 批量删除
    const deletePromises = selectedRows.value.map(row => uiStore.removeCase(row.id))
    await Promise.all(deletePromises)
    
    selectedRows.value = []
    tableRef.value?.clearSelection()
    ElMessage.success(`已删除 ${count} 个用例`)
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('批量删除失败: ' + (err.message || '未知错误'))
    }
  }
}

function onCreatorFilter(value, row) {
  if (!value) return true
  return row.creator === value
}

const moduleFilters = computed(() => {
  const set = new Set(tableData.value.map((c) => c.moduleName || ''))
  return Array.from(set)
    .filter(Boolean)
    .map((name) => ({ text: name, value: name }))
})

function onModuleFilter(value, row) {
  if (!value) return true
  return row.moduleName === value
}

function onSortChange({ prop, order }) {
  sortState.value = { prop, order }
  currentPage.value = 1
}

function goBack() {
  router.push('/dashboard')
}

function focusCaseRow(testCase) {
  if (selectedModuleKey.value && selectedModuleKey.value !== 'all' && selectedModuleKey.value !== testCase.moduleKey) {
    uiStore.setModule('all')
  }

  nextTick(() => {
    const idx = sortedData.value.findIndex((c) => c.id === testCase.id)
    if (idx === -1) return
    currentPage.value = Math.floor(idx / pageSize.value) + 1
  })
}
</script>

<style scoped>
.ui-space-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: 100%;
}

.space-hero {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
  border-radius: var(--border-radius);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94) 0%, rgba(248, 251, 255, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: var(--card-shadow);
}

.space-hero__main {
  min-width: 0;
}

.page-header {
  padding: 0;
  margin-bottom: 10px;
}

.space-hero__desc {
  max-width: 640px;
  color: var(--text-secondary);
  line-height: 1.7;
  font-size: 14px;
}

.space-hero__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(110px, 1fr));
  gap: 12px;
  min-width: 360px;
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
  font-size: 28px;
  line-height: 1;
  color: var(--text-primary);
}

.space-card {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  border-radius: var(--border-radius);
  overflow: hidden;
}

.space-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
  gap: 16px;
  flex-wrap: wrap;
  padding: 6px 4px 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.space-header-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px 18px;
}

.space-header-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.field {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  background: var(--surface-muted);
  border: 1px solid rgba(226, 232, 240, 0.78);
}

.label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  white-space: nowrap;
}

.module-create-trigger {
  height: 40px;
  padding: 0 16px;
  border-radius: 14px;
}

.filter-summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.case-table {
  width: 100%;
  flex: 1;
  border-radius: var(--border-radius);
}

:deep(.el-table) {
  border-radius: var(--border-radius);
  overflow: hidden;
}

:deep(.el-table th.el-table__cell) {
  background: #f8fbff;
  font-weight: 600;
  color: var(--text-primary);
  font-size: 13px;
  padding: 14px 0;
  white-space: nowrap;
}

:deep(.el-table th.el-table__cell > .cell) {
  white-space: nowrap;
  word-break: keep-all;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
}

:deep(.el-table th.el-table__cell.is-center > .cell) {
  justify-content: center;
}

:deep(.el-table td.el-table__cell) {
  padding: 14px 0;
  font-size: 13px;
  height: 60px;
}

:deep(.el-table td.el-table__cell > .cell) {
  white-space: nowrap;
  word-break: keep-all;
}

:deep(.el-table .caret-wrapper) {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 12px;
  width: 12px;
  margin-left: 4px;
  vertical-align: middle;
  position: relative;
}

:deep(.el-table .sort-caret) {
  position: static !important;
  width: 0;
  height: 0;
  border-left: 4px solid transparent !important;
  border-right: 4px solid transparent !important;
  margin: 0 !important;
}

:deep(.el-table .sort-caret.ascending) {
  border-bottom: 5px solid #c0c4cc !important;
  border-top: none !important;
}

:deep(.el-table .sort-caret.descending) {
  border-top: 5px solid #c0c4cc !important;
  border-bottom: none !important;
}

:deep(.el-table .ascending .sort-caret.ascending) {
  border-bottom-color: var(--primary-color) !important;
}

:deep(.el-table .descending .sort-caret.descending) {
  border-top-color: var(--primary-color) !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: #fbfdff;
}

:deep(.el-table__body tr:hover > td.el-table__cell) {
  background: rgba(59, 130, 246, 0.04) !important;
}

:deep(.el-table-row-selected) {
  background: rgba(59, 130, 246, 0.08) !important;
}

.case-name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.case-name {
  color: var(--text-primary);
  font-weight: 600;
}

.case-updated {
  font-size: 12px;
  color: var(--text-tertiary);
}

.table-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.table-tag--module {
  color: #0f766e;
  background: rgba(20, 184, 166, 0.1);
}

.table-tag--team {
  color: #1d4ed8;
  background: rgba(59, 130, 246, 0.1);
}

.creator-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #f8fafc;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.op-row {
  display: flex;
  gap: 8px;
  justify-content: flex-start;
  align-items: center;
  flex-wrap: nowrap;
}

:deep(.el-button) {
  border-radius: 6px;
  font-weight: 500;
}

.op-row :deep(.el-button) {
  padding: 7px 12px;
  margin: 0 !important;
}

.table-footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
}

.table-footer__hint {
  color: var(--text-tertiary);
  font-size: 12px;
}

:deep(.create-dialog) {
  border-radius: var(--border-radius);
}

:deep(.create-dialog .el-dialog__header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f1f5f9;
  margin-right: 0;
}

:deep(.create-dialog .el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

:deep(.create-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.create-form .el-form-item__label) {
  font-weight: 500;
  color: var(--text-primary);
}

:deep(.create-form .el-form-item) {
  margin-bottom: 20px;
}

:deep(.create-form .el-form-item:last-child) {
  margin-bottom: 0;
}

.create-form--module {
  padding-top: 4px;
}

.form-control {
  width: 100%;
}

:deep(.create-form--module .el-form-item__content) {
  max-width: 260px;
}

:deep(.create-form .el-form-item.is-required .el-form-item__label::before) {
  color: #f56c6c;
  margin-right: 4px;
}

:deep(.create-dialog .el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.module-manage-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.module-manage-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.module-manage-item__meta {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.module-manage-item__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.module-manage-item__key,
.module-manage-item__count {
  font-size: 12px;
  color: var(--text-secondary);
}

.module-manage-item__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

:deep(.el-pagination) {
  --el-pagination-hover-color: var(--primary-color);
}

:deep(.el-dialog) {
  border-radius: var(--border-radius);
}

:deep(.el-dialog__header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-primary);
}

@media (max-width: 1200px) {
  .space-hero {
    flex-direction: column;
  }

  .space-hero__stats {
    min-width: 0;
  }
}

@media (max-width: 900px) {
  .space-hero__stats {
    grid-template-columns: 1fr;
  }

  .table-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
