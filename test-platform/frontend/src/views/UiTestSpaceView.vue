<template>
  <div class="ui-space-page">
    <el-page-header content="UI 测试用例空间" class="page-header" />

    <el-card shadow="never" class="space-card">
      <div class="space-header">
        <div class="space-header-left">
          <div class="field">
            <span class="label">所属组织</span>
            <el-select
              v-model="selectedTeamId"
              placeholder="全部"
              size="small"
              style="width: 200px"
              clearable
            >
              <el-option label="全部" :value="''" />
              <el-option
                v-for="team in teams"
                :key="team.id"
                :label="team.name"
                :value="team.id"
              />
            </el-select>
          </div>
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
                v-for="m in modules"
                :key="m.key"
                :label="m.name"
                :value="m.key"
              />
            </el-select>
          </div>
        </div>
        <div class="space-header-right">
          <el-button
            type="primary"
            size="small"
            @click="onCreateCase"
          >
            新建 UI 用例
          </el-button>
        </div>
      </div>

      <el-table
        :data="pagedData"
        size="small"
        border
        class="case-table"
        @sort-change="onSortChange"
      >
        <el-table-column
          prop="seq"
          label="用例序号"
          width="90"
          sortable
        />
        <el-table-column
          prop="name"
          label="用例名称"
          min-width="180"
          show-overflow-tooltip
          sortable
        />
        <el-table-column
          prop="moduleName"
          label="用例分类"
          width="120"
          :filters="moduleFilters"
          :filter-method="onModuleFilter"
        />
        <el-table-column
          prop="teamName"
          label="所属组织"
          width="140"
          :filters="teamFilters"
          :filter-method="onTeamFilter"
        />
        <el-table-column
          prop="creator"
          label="创建人"
          width="100"
          :filters="creatorFilters"
          :filter-method="onCreatorFilter"
        />
        <el-table-column
          prop="updatedAtDisplay"
          label="最近更新时间"
          width="160"
          sortable
        />
        <el-table-column
          label="操作"
          width="220"
        >
          <template #default="{ row }">
            <div class="op-row">
              <el-button
                size="small"
                type="primary"
                plain
                @click="enterCase(row)"
              >
                进入编排
              </el-button>
              <el-button
                size="small"
                type="success"
                plain
                @click="duplicateCase(row)"
              >
                复制
              </el-button>
              <el-popconfirm
                title="确定删除该用例？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                confirm-button-type="danger"
                @confirm="deleteCase(row)"
              >
                <template #reference>
                  <el-button
                    size="small"
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
      width="480px"
    >
      <el-form label-width="80px" size="small">
        <el-form-item label="用例序号">
          <el-input
            :value="uiStore.nextSeq"
            style="width: 260px"
            disabled
          />
        </el-form-item>
        <el-form-item label="所属组织">
          <el-select
            v-model="newCaseTeamId"
            placeholder="请选择所属组织"
            style="width: 260px"
          >
            <el-option
              v-for="team in teams"
              :key="team.id"
              :label="team.name"
              :value="team.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用例分类">
          <el-select
            v-model="newCaseModuleKey"
            placeholder="请选择用例分类"
            style="width: 260px"
          >
            <el-option
              v-for="m in modules"
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
        <el-form-item label="用例标题">
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
  </div>
</template>

<script setup>
import { computed, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useUiTestStore } from '../stores/uiTest'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const uiStore = useUiTestStore()

const teams = uiStore.teams
const modules = uiStore.modules

const createDialogVisible = ref(false)
const newCaseTeamId = ref('')
const newCaseModuleKey = ref('')
const newCaseCreator = ref('')
const newCaseTitle = ref('')

const currentPage = ref(1)
const pageSize = ref(10)

const selectedTeamId = computed({
  get: () => uiStore.selectedTeamId,
  set: (val) => uiStore.setTeam(val),
})

const selectedModuleKey = computed({
  get: () => uiStore.selectedModuleKey,
  set: (val) => uiStore.setModule(val),
})

const tableData = computed(() => {
  const teamMap = new Map(teams.map((t) => [t.id, t.name]))
  const moduleMap = new Map(modules.map((m) => [m.key, m.name]))
  return uiStore.filteredCases.map((c) => ({
    ...c,
    seq: c.seq,
    teamName: teamMap.get(c.teamId) || c.teamId,
    moduleName: moduleMap.get(c.moduleKey) || c.moduleKey,
    updatedAtDisplay: c.updatedAt
      ? new Date(c.updatedAt).toLocaleString()
      : '-',
  }))
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
  const teamId = selectedTeamId.value || teams[0]?.id
  const moduleKey = selectedModuleKey.value === 'all'
    ? modules[0]?.key
    : selectedModuleKey.value
  return { teamId, moduleKey }
}

function onCreateCase() {
  const { teamId } = ensureTeamAndModule()
  newCaseTeamId.value = teamId || ''
  newCaseModuleKey.value = ''
  newCaseCreator.value = userStore.username || '未命名用户'
   newCaseTitle.value = ''
  createDialogVisible.value = true
}

function confirmCreateCase() {
  if (!newCaseTeamId.value) {
    ElMessage.error('请选择所属组织')
    return
  }
  if (!newCaseModuleKey.value) {
    ElMessage.error('请选择用例分类')
    return
  }

  const newCase = uiStore.createCase({
    teamId: newCaseTeamId.value,
    moduleKey: newCaseModuleKey.value,
    name: newCaseTitle.value || '新建 UI 用例',
    creator: newCaseCreator.value,
  })

  createDialogVisible.value = false
  focusCaseRow(newCase)
}

function enterCase(row) {
  router.push({
    path: `/ui-test/cases/${row.id}`,
  })
}

function duplicateCase(row) {
  const copy = uiStore.createCase({
    teamId: row.teamId,
    moduleKey: row.moduleKey,
    name: `${row.name}（复制）`,
    creator: row.creator,
  })
  focusCaseRow(copy)
  ElMessage.success(`已复制用例：${copy.name}`)
}

function deleteCase(row) {
  uiStore.removeCase(row.id)
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

const teamFilters = computed(() => {
  const set = new Set(tableData.value.map((c) => c.teamName || ''))
  return Array.from(set)
    .filter(Boolean)
    .map((name) => ({ text: name, value: name }))
})

function onModuleFilter(value, row) {
  if (!value) return true
  return row.moduleName === value
}

function onTeamFilter(value, row) {
  if (!value) return true
  return row.teamName === value
}

function onSortChange({ prop, order }) {
  sortState.value = { prop, order }
  currentPage.value = 1
}

function focusCaseRow(testCase) {
  // 组织空间：如果当前是“全部”（空值），则不动；如果是具体组织但与新用例不符，则切回全部
  if (selectedTeamId.value && selectedTeamId.value !== testCase.teamId) {
    uiStore.setTeam('')
  }

  // 用例分类：如果当前是“全部”（all），则不动；如果是具体分类但与新用例不符，则切回全部
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
  gap: 12px;
  height: 100%;
}

.page-header {
  padding: 0;
}

.space-card {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.space-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 12px;
  flex-wrap: wrap;
}

.space-header-left {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 24px;
}

.space-header-right {
  flex-shrink: 0;
}

.field {
  display: flex;
  align-items: center;
  gap: 6px;
}

.label {
  font-size: 13px;
  color: #6b7280;
}

.case-table {
  width: 100%;
  flex: 1;
}

.op-row {
  display: flex;
  gap: 6px;
  justify-content: flex-start;
}

.table-footer {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}
</style>

