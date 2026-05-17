<template>
  <div class="org-page">
    <section class="org-hero">
      <div class="org-hero__main">
        <el-page-header content="组织管理" class="hero-header" @back="goBack" />
        <h2 class="hero-title">统一管理你的组织空间与成员协作</h2>
        <p class="hero-desc">
          在这里创建组织、维护组织信息、查看成员和邀请记录，页面样式已统一为清爽的卡片式管理后台风格。
        </p>
      </div>
      <div class="org-hero__side">
        <div class="hero-stat">
          <span class="hero-stat__label">组织数量</span>
          <strong class="hero-stat__value">{{ orgStore.organizations.length }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-stat__label">已选择</span>
          <strong class="hero-stat__value">{{ selectedOrgIds.length }}</strong>
        </div>
      </div>
    </section>

    <div class="org-page-header">
      <div class="header-actions">
        <el-button
          :type="batchSelectMode ? 'primary' : 'default'"
          plain
          @click="toggleBatchSelectMode"
        >
          {{ batchSelectMode ? '退出多选' : '批量选择' }}
        </el-button>
        <el-button
          plain
          :disabled="orgStore.organizations.length === 0"
          @click="toggleSelectAll"
        >
          {{ allSelected ? '取消全选' : '全选' }}
        </el-button>
        <el-dropdown @command="handleBatchCommand">
          <el-button plain :disabled="selectedOrgIds.length === 0">
            批量操作
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="clear" :disabled="selectedOrgIds.length === 0">
                清空选择
              </el-dropdown-item>
              <el-dropdown-item command="delete" :disabled="selectedOrgIds.length === 0">
                批量删除
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <span class="selection-summary">已选 {{ selectedOrgIds.length }} 个组织</span>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          创建组织
        </el-button>
      </div>
    </div>

    <el-empty v-if="orgStore.organizations.length === 0" description="暂无可访问的组织" />

    <div v-else class="org-list">
      <div
        v-for="org in orgStore.organizations"
        :key="org.id"
        class="org-item"
        :class="{ 'is-selected': selectedOrgIds.includes(org.id), 'is-batch-mode': batchSelectMode }"
        @click="handleOrgCardClick(org)"
      >
        <div v-if="batchSelectMode" class="org-select-check" @click.stop="toggleOrgSelection(org.id)">
          <el-checkbox
            :model-value="selectedOrgIds.includes(org.id)"
            @change="() => toggleOrgSelection(org.id)"
          />
        </div>
        <div class="org-item-main">
          <div class="org-avatar" :style="{ backgroundColor: org.color }">
            {{ org.name.charAt(0).toUpperCase() }}
          </div>
          <div class="org-content">
            <div class="org-title-row">
              <span class="org-title">{{ org.name }}</span>
              <el-tag v-if="org.ownerId === userStore.userId" size="small" type="warning">所有者</el-tag>
            </div>
            <div class="org-desc">
              {{ org.description || '暂无描述' }}
            </div>
            <div class="org-stats-row">
              <span class="org-stat">
                <el-icon><UserIcon /></el-icon>
                {{ org.memberCount || 0 }} 成员
              </span>
              <span class="org-stat">
                <el-icon><FolderIcon /></el-icon>
                {{ org.projectCount || 0 }} 项目
              </span>
              <span class="org-owner">
                <el-avatar :size="16" class="mini-avatar">
                  {{ (org.ownerUsername || '?').charAt(0).toUpperCase() }}
                </el-avatar>
                {{ org.ownerUsername || '未知' }}
              </span>
            </div>
          </div>
        </div>
        <div class="org-item-actions">
          <el-button
            class="select-btn"
            :type="selectedOrgIds.includes(org.id) ? 'primary' : 'default'"
            plain
            @click.stop="toggleOrgSelection(org.id)"
          >
            {{ selectedOrgIds.includes(org.id) ? '已选' : '选择' }}
          </el-button>
          <el-button type="primary" plain @click.stop="goToMembers(org)" :disabled="batchSelectMode">
            <el-icon><UserFilled /></el-icon>
            人员
          </el-button>
          <el-button plain @click.stop="editOrg(org)" :disabled="batchSelectMode">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button type="danger" plain @click.stop="deleteOrg(org)" :disabled="batchSelectMode">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 创建组织 -->
    <el-dialog v-model="showCreateDialog" title="创建组织" width="500px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="输入组织名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="输入组织描述" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑组织 -->
    <el-dialog v-model="showEditDialog" title="编辑组织" width="500px">
      <el-form :model="editForm" label-width="70px">
        <el-form-item label="名称" required>
          <el-input v-model="editForm.name" placeholder="输入组织名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="2" placeholder="输入组织描述" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="editForm.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 人员管理 -->
    <el-dialog
      v-model="showMembersDialog"
      :title="`人员管理 - ${currentOrg?.name || ''}`"
      width="700px"
    >
      <div v-loading="membersLoading">
        <el-table :data="members" stripe size="small">
          <el-table-column prop="username" label="用户名" min-width="100" />
          <el-table-column prop="role" label="角色" width="110">
            <template #default="{ row }">
              <el-tag :type="roleTagType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="canRead" label="读" width="60" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.canRead" color="#67C23A"><Check /></el-icon>
              <el-icon v-else color="#F56C6C"><Close /></el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="canWrite" label="写" width="60" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.canWrite" color="#67C23A"><Check /></el-icon>
              <el-icon v-else color="#F56C6C"><Close /></el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="joinedAt" label="加入时间" width="150">
            <template #default="{ row }">{{ formatDate(row.joinedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.userId !== userStore.userId"
                link
                type="danger"
                size="small"
                @click="handleRemoveMember(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="showMembersDialog = false">关闭</el-button>
        <el-button type="primary" @click="openInviteDialog(currentOrg)">邀请成员</el-button>
      </template>
    </el-dialog>

    <!-- 邀请成员 -->
    <el-dialog v-model="showInviteDialogFlag" title="邀请成员" width="580px">
      <el-tabs v-model="inviteTab">
        <el-tab-pane label="生成邀请码" name="create">
          <el-form :model="inviteForm" label-width="100px">
            <el-form-item label="被邀请手机">
              <el-input v-model="inviteForm.invitedPhone" placeholder="选填" />
            </el-form-item>
            <el-form-item label="有效期">
              <el-select v-model="inviteForm.validDays">
                <el-option :value="7" label="7天" />
                <el-option :value="14" label="14天" />
                <el-option :value="30" label="30天" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleCreateInvite" :loading="inviteLoading">生成邀请码</el-button>
            </el-form-item>
          </el-form>
          <div v-if="generatedCode" class="invite-code-box">
            <p>邀请码</p>
            <h2>{{ generatedCode }}</h2>
            <p class="tip">请发送给需要加入的成员</p>
          </div>
        </el-tab-pane>
        <el-tab-pane label="邀请记录" name="history">
          <el-table :data="invitations" size="small" stripe>
            <el-table-column prop="invitationCode" label="邀请码" width="110" />
            <el-table-column prop="invitedPhone" label="手机" width="100">
              <template #default="{ row }">{{ row.invitedPhone || '-' }}</template>
            </el-table-column>
            <el-table-column prop="used" label="状态" width="70">
              <template #default="{ row }">
                <el-tag :type="row.used ? 'danger' : 'success'" size="small">{{ row.used ? '已用' : '未用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="usedByUsername" label="使用者" width="90">
              <template #default="{ row }">{{ row.usedByUsername || '-' }}</template>
            </el-table-column>
            <el-table-column prop="expiredAt" label="过期时间">
              <template #default="{ row }">{{ formatDate(row.expiredAt) }}</template>
            </el-table-column>
          </el-table>
          <el-button type="primary" style="margin-top: 12px" size="small" @click="loadInvitations">刷新</el-button>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User as UserIcon,
  Folder as FolderIcon,
  Plus,
  Edit,
  Delete,
  UserFilled,
  Check,
  Close,
  ArrowDown,
} from '@element-plus/icons-vue'
import { useOrgStore } from '../stores/org'
import { useUserStore } from '../stores/user'
import { updateOrganization, deleteOrganization, getOrgMembers, removeMember as removeMemberApi } from '../api/organization'
import { getOrgInvitations, createInvitation } from '../api/invitation'

const router = useRouter()
const orgStore = useOrgStore()
const userStore = useUserStore()

const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const showInviteDialogFlag = ref(false)
const showMembersDialog = ref(false)
const inviteTab = ref('create')
const inviteLoading = ref(false)
const membersLoading = ref(false)
const generatedCode = ref('')
const invitations = ref([])
const currentOrgId = ref(null)
const members = ref([])
const selectedOrgIds = ref([])
const batchSelectMode = ref(false)

const currentOrg = computed(() =>
  orgStore.organizations.find((o) => o.id === currentOrgId.value)
)

const allSelected = computed(() =>
  orgStore.organizations.length > 0
  && selectedOrgIds.value.length === orgStore.organizations.length
)

const form = reactive({ name: '', description: '', color: '#409EFF' })
const editForm = reactive({ id: null, name: '', description: '', color: '' })
const inviteForm = reactive({ invitedPhone: '', validDays: 7 })

onMounted(() => {
  orgStore.fetchOrganizations()
})

watch(
  () => orgStore.organizations.map((org) => org.id),
  (ids) => {
    selectedOrgIds.value = selectedOrgIds.value.filter((id) => ids.includes(id))
    if (selectedOrgIds.value.length === 0) {
      batchSelectMode.value = false
    }
  },
  { immediate: true },
)

function goBack() {
  router.push('/dashboard')
}

async function goToMembers(org) {
  if (batchSelectMode.value) {
    toggleOrgSelection(org.id)
    return
  }
  currentOrgId.value = org.id
  orgStore.setCurrentOrganization(org.id)
  showMembersDialog.value = true
  membersLoading.value = true
  try {
    const res = await getOrgMembers(org.id)
    members.value = res.data || []
  } catch (e) {
    ElMessage.error('加载成员失败')
    members.value = []
  } finally {
    membersLoading.value = false
  }
}

function editOrg(org) {
  editForm.id = org.id
  editForm.name = org.name
  editForm.description = org.description || ''
  editForm.color = org.color || '#409EFF'
  showEditDialog.value = true
}

async function handleCreate() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入组织名称')
    return
  }
  try {
    await orgStore.createOrg({ ...form })
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    Object.assign(form, { name: '', description: '', color: '#409EFF' })
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '创建失败')
  }
}

async function handleUpdate() {
  if (!editForm.name.trim()) {
    ElMessage.warning('请输入组织名称')
    return
  }
  try {
    await updateOrganization(editForm.id, { ...editForm })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    orgStore.fetchOrganizations()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '更新失败')
  }
}

async function deleteOrg(org) {
  try {
    await ElMessageBox.confirm(
      `确定删除组织 "${org.name}" 吗？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteOrganization(org.id)
    ElMessage.success('删除成功')
    selectedOrgIds.value = selectedOrgIds.value.filter((id) => id !== org.id)
    orgStore.fetchOrganizations()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

async function handleBatchDelete() {
  if (selectedOrgIds.value.length === 0) return
  const names = orgStore.organizations
    .filter((o) => selectedOrgIds.value.includes(o.id))
    .map((o) => o.name)
    .join('、')
  try {
    await ElMessageBox.confirm(
      `确定删除以下 ${selectedOrgIds.value.length} 个组织吗？\n\n${names}`,
      '批量删除',
      { type: 'warning', confirmButtonText: '删除' }
    )
    for (const id of selectedOrgIds.value) {
      await deleteOrganization(id)
    }
    ElMessage.success(`已删除 ${selectedOrgIds.value.length} 个组织`)
    selectedOrgIds.value = []
    orgStore.fetchOrganizations()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('批量删除失败')
  }
}

function toggleOrgSelection(id) {
  if (selectedOrgIds.value.includes(id)) {
    selectedOrgIds.value = selectedOrgIds.value.filter((item) => item !== id)
  } else {
    selectedOrgIds.value = [...selectedOrgIds.value, id]
  }
  batchSelectMode.value = selectedOrgIds.value.length > 0
}

function clearSelectedOrgs() {
  selectedOrgIds.value = []
  batchSelectMode.value = false
}

function toggleBatchSelectMode() {
  batchSelectMode.value = !batchSelectMode.value
  if (!batchSelectMode.value) {
    selectedOrgIds.value = []
  }
}

function toggleSelectAll() {
  if (allSelected.value) {
    clearSelectedOrgs()
    return
  }
  selectedOrgIds.value = orgStore.organizations.map((org) => org.id)
  batchSelectMode.value = true
}

function handleOrgCardClick(org) {
  if (batchSelectMode.value) {
    toggleOrgSelection(org.id)
    return
  }
  goToMembers(org)
}

function handleBatchCommand(command) {
  if (command === 'clear') {
    clearSelectedOrgs()
    return
  }
  if (command === 'delete') {
    handleBatchDelete()
  }
}

async function openInviteDialog(org) {
  if (!org) return
  currentOrgId.value = org.id
  showInviteDialogFlag.value = true
  inviteTab.value = 'create'
  generatedCode.value = ''
  inviteForm.invitedPhone = ''
  inviteForm.validDays = 7
  await loadInvitations()
}

async function loadInvitations() {
  if (!currentOrgId.value) return
  inviteLoading.value = true
  try {
    const res = await getOrgInvitations(currentOrgId.value)
    invitations.value = res.data || []
  } catch (e) {
    ElMessage.error('加载邀请记录失败')
  } finally {
    inviteLoading.value = false
  }
}

async function handleCreateInvite() {
  inviteLoading.value = true
  try {
    const res = await createInvitation(currentOrgId.value, {
      invitedPhone: inviteForm.invitedPhone || null,
      validDays: inviteForm.validDays,
    })
    generatedCode.value = res.data.invitationCode
    ElMessage.success('邀请码生成成功')
    await loadInvitations()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '生成失败')
  } finally {
    inviteLoading.value = false
  }
}

async function handleRemoveMember(member) {
  try {
    await ElMessageBox.confirm(`移除 "${member.username}"？`, '确认', { type: 'warning' })
    await removeMemberApi(currentOrgId.value, member.userId)
    ElMessage.success('移除成功')
    const res = await getOrgMembers(currentOrgId.value)
    members.value = res.data || []
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '移除失败')
  }
}

function roleLabel(role) {
  const map = { SPACE_CREATOR: '创建者', SPACE_ADMIN: '管理员', ORG_ADMIN: '组织管理', MEMBER: '成员' }
  return map[role] || role
}

function roleTagType(role) {
  if (role === 'SPACE_CREATOR') return 'danger'
  if (role === 'SPACE_ADMIN') return 'warning'
  if (role === 'ORG_ADMIN') return 'success'
  return 'info'
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}
</script>

<style scoped>
.org-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.org-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
  border-radius: var(--border-radius);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, rgba(246, 250, 255, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: var(--card-shadow);
}

.org-hero__main {
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

.org-hero__side {
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

.org-page-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.selection-summary {
  font-size: 13px;
  color: var(--text-secondary);
  padding: 0 2px;
}

.org-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.org-item {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: var(--transition);
  position: relative;
}

.org-item:hover {
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.org-item.is-selected {
  border-color: rgba(59, 130, 246, 0.38);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.08);
}

.org-item.is-batch-mode {
  cursor: pointer;
}

.org-select-check {
  position: absolute;
  top: 14px;
  right: 16px;
  z-index: 1;
  padding: 4px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
}

.org-item-main {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.org-avatar {
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

.org-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.org-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.org-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.org-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.org-stats-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--text-secondary);
}

.org-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f8fbff;
}

.org-stat .el-icon {
  font-size: 13px;
}

.org-owner {
  display: flex;
  align-items: center;
  gap: 4px;
}

.mini-avatar {
  background: var(--primary-gradient);
  color: white;
  font-size: 10px;
}

.org-item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  width: 100%;
  justify-content: flex-end;
  padding-top: 4px;
  border-top: 1px solid rgba(226, 232, 240, 0.85);
}

.org-item-actions .el-button {
  display: flex;
  align-items: center;
  gap: 3px;
}

.select-btn {
  min-width: 74px;
}

.invite-code-box {
  text-align: center;
  padding: 20px;
  background: var(--primary-gradient);
  border-radius: 16px;
  color: white;
  margin-top: 16px;
  box-shadow: 0 18px 34px rgba(59, 130, 246, 0.22);
}

.invite-code-box p {
  margin: 0 0 6px 0;
  font-size: 13px;
  opacity: 0.9;
}

.invite-code-box h2 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 3px;
  font-family: monospace;
}

.invite-code-box .tip {
  margin-top: 8px;
  font-size: 11px;
  opacity: 0.7;
}

@media (max-width: 1200px) {
  .org-hero {
    flex-direction: column;
  }

  .org-list {
    grid-template-columns: 1fr;
  }
}
</style>
