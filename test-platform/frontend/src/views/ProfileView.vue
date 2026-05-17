<template>
  <div class="page">
    <section class="profile-hero">
      <div class="profile-hero__main">
        <div class="page-header">
          <h2>个人中心</h2>
        </div>
        <p class="hero-desc">
          管理个人信息、切换组织空间并维护当前账户安全设置，界面已统一为简洁的后台管理风格。
        </p>
      </div>
      <div class="profile-hero__side">
        <div class="hero-stat">
          <span class="hero-stat__label">当前用户</span>
          <strong class="hero-stat__value">{{ userInfo.username || userStore.username || '未设置' }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-stat__label">组织数量</span>
          <strong class="hero-stat__value">{{ organizations.length }}</strong>
        </div>
      </div>
    </section>

    <el-row :gutter="24">
      <el-col :span="16">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span>个人信息</span>
              <el-button v-if="!isEditing" type="primary" size="small" @click="startEdit">编辑</el-button>
            </div>
          </template>

          <div v-if="!isEditing" class="info-view">
            <div class="info-item">
              <span class="label">用户名</span>
              <span class="value">{{ userInfo.username || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="label">手机号</span>
              <span class="value">{{ formatPhone(userInfo.phone) || '未绑定' }}</span>
            </div>
          </div>

          <el-form v-else :model="editForm" label-width="80px" class="edit-form">
            <el-form-item label="用户名">
              <el-input v-model="editForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input
                v-model="editForm.phone"
                placeholder="请输入手机号"
                maxlength="11"
                inputmode="numeric"
                @input="editForm.phone = sanitizePhoneInput(editForm.phone)"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveEdit" :loading="saving">保存</el-button>
              <el-button @click="cancelEdit">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span>我的组织空间</span>
            </div>
          </template>

          <div v-if="organizations.length === 0" class="empty-tip">
            暂未加入任何组织
          </div>

          <div v-else class="org-list">
            <div
              v-for="org in organizations"
              :key="org.id"
              :class="['org-item', { active: currentOrgId == org.id }]"
              @click="switchOrg(org)"
            >
              <div class="org-icon" :style="{ backgroundColor: org.color }">
                {{ org.name.charAt(0).toUpperCase() }}
              </div>
              <div class="org-info">
                <div class="org-name">{{ org.name }}</div>
                <div class="org-meta">
                  {{ org.memberCount }} 成员 · {{ org.projectCount }} 项目
                </div>
              </div>
              <el-tag v-if="currentOrgId == org.id" type="success" size="small">当前</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span>安全设置</span>
            </div>
          </template>
          <div class="action-list">
            <el-button type="primary" plain @click="handleEditProfile" class="action-btn">
              修改个人信息
            </el-button>
            <el-button type="danger" plain @click="handleLeaveOrg" class="action-btn" v-if="currentOrgId">
              退出当前企业
            </el-button>
            <el-button type="danger" @click="handleLogout" class="action-btn">
              退出登录
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { useOrgStore } from '../stores/org'
import { getCurrentUser, updateCurrentUser } from '../api/user'
import { getUserOrganizations, removeMember } from '../api/organization'
import { isValidPhone, sanitizePhoneInput } from '../utils/phone'

const router = useRouter()
const userStore = useUserStore()
const orgStore = useOrgStore()

const userInfo = ref({})
const organizations = ref([])
const currentOrgId = ref(null)
const isEditing = ref(false)
const saving = ref(false)
const editForm = ref({
  username: '',
  phone: ''
})

onMounted(async () => {
  try {
    await Promise.all([
      loadUserInfo(),
      loadOrganizations()
    ])
  } catch (e) {
    console.error('Failed to load profile data', e)
  }
})

async function loadUserInfo() {
  try {
    const { data } = await getCurrentUser()
    userInfo.value = data
    editForm.value.username = data.username || ''
    editForm.value.phone = data.phone || ''
  } catch (e) {
    console.error('Failed to load user info', e)
  }
}

async function loadOrganizations() {
  try {
    const { data } = await getUserOrganizations()
    organizations.value = data || []
    currentOrgId.value = orgStore.currentOrganizationId
  } catch (e) {
    console.error('Failed to load organizations', e)
  }
}

function startEdit() {
  editForm.value.username = userInfo.value.username || ''
  editForm.value.phone = userInfo.value.phone || ''
  isEditing.value = true
}

function cancelEdit() {
  isEditing.value = false
}

async function saveEdit() {
  if (!editForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  editForm.value.phone = sanitizePhoneInput(editForm.value.phone)
  if (editForm.value.phone && !isValidPhone(editForm.value.phone)) {
    ElMessage.warning('请输入11位手机号')
    return
  }
  saving.value = true
  try {
    const { data } = await updateCurrentUser({
      username: editForm.value.username,
      phone: editForm.value.phone
    })
    userInfo.value = data
    userStore.setAuth(userStore.token, data.username, data.phone, data.id)
    isEditing.value = false
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleEditProfile() {
  startEdit()
}

async function handleLeaveOrg() {
  if (!currentOrgId.value) {
    ElMessage.warning('请先选择一个组织')
    return
  }

  const org = organizations.value.find(o => o.id === currentOrgId.value)
  if (!org) return

  try {
    await ElMessageBox.confirm(
      `确定要退出组织"${org.name}"吗？退出后您将无法访问该组织的资源。`,
      '退出组织确认',
      {
        confirmButtonText: '确定退出',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await removeMember(currentOrgId.value, userStore.userId)
    ElMessage.success('已退出组织')
    await loadOrganizations()
    if (organizations.value.length > 0) {
      orgStore.setCurrentOrganization(organizations.value[0].id)
      currentOrgId.value = organizations.value[0].id
    } else {
      orgStore.setCurrentOrganization(null)
      currentOrgId.value = null
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('退出失败')
    }
  }
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
}

function switchOrg(org) {
  orgStore.setCurrentOrganization(org.id)
  currentOrgId.value = org.id
  ElMessage.success(`已切换到 ${org.name}`)
}

function formatPhone(phone) {
  if (!phone) return ''
  if (phone.length === 11) {
    return phone.substring(0, 3) + '****' + phone.substring(7)
  }
  return phone
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
  border-radius: var(--border-radius);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, rgba(246, 250, 255, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: var(--card-shadow);
}

.profile-hero__main {
  min-width: 0;
}

.profile-hero__side {
  display: grid;
  gap: 12px;
  min-width: 250px;
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

.hero-desc {
  max-width: 760px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.page-header {
  margin-bottom: 8px;
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.info-card {
  margin-bottom: 24px;
  border-radius: 18px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fbff;
}

.info-item .label {
  width: 80px;
  color: var(--text-secondary);
  font-size: 14px;
}

.info-item .value {
  font-size: 14px;
  color: var(--text-primary);
}

.edit-form {
  max-width: 400px;
}

.empty-tip {
  color: var(--text-secondary);
  text-align: center;
  padding: 20px;
}

.org-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.org-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: rgba(255, 255, 255, 0.92);
}

.org-item:hover {
  background: #ffffff;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.06);
}

.org-item.active {
  background: #eef6ff;
  border-color: rgba(59, 130, 246, 0.38);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.org-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 16px;
}

.org-info {
  flex: 1;
}

.org-name {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 2px;
}

.org-meta {
  font-size: 12px;
  color: var(--text-secondary);
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 0;
}

.action-btn {
  width: 100%;
  margin: 0 !important;
  height: 44px;
}

@media (max-width: 1100px) {
  .profile-hero {
    flex-direction: column;
  }

  .profile-hero__side {
    min-width: 0;
    grid-template-columns: 1fr 1fr;
  }
}
</style>
