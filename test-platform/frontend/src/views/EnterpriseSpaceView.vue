<template>
  <div class="space-page">
    <section class="space-hero">
      <div>
        <h2>企业空间</h2>
        <p class="space-desc">
          企业空间代表整个公司，可统一邀请成员，再按组织分配到不同业务部门。
        </p>
      </div>
      <div class="space-meta" v-if="space">
        <span>{{ space.name }}</span>
        <span>{{ space.memberCount || 0 }} 名成员</span>
        <span>{{ space.organizationCount || 0 }} 个组织</span>
      </div>
    </section>

    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>邀请新成员</template>
          <el-form label-width="90px">
            <el-form-item label="手机号">
              <el-input
                v-model="inviteForm.invitedPhone"
                placeholder="请输入被邀请人手机号"
                maxlength="11"
                inputmode="numeric"
                @input="inviteForm.invitedPhone = sanitizePhoneInput(inviteForm.invitedPhone)"
              />
            </el-form-item>
            <el-form-item label="加入组织">
              <el-select v-model="inviteForm.organizationId" clearable placeholder="可选，直接指定部门">
                <el-option
                  v-for="org in orgStore.organizations"
                  :key="org.id"
                  :label="org.name"
                  :value="org.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="有效期">
              <el-select v-model="inviteForm.validDays">
                <el-option :value="7" label="7天" />
                <el-option :value="14" label="14天" />
                <el-option :value="30" label="30天" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="inviteLoading" @click="submitInvite">发送邀请</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>企业成员</template>
          <el-table :data="members" size="small" stripe>
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="phone" label="手机号" />
            <el-table-column prop="role" label="角色" width="140" />
            <el-table-column prop="joinedAt" label="加入时间">
              <template #default="{ row }">{{ formatDate(row.joinedAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>邀请记录</template>
      <el-table :data="invitations" size="small" stripe>
        <el-table-column prop="invitedPhone" label="手机号" />
        <el-table-column prop="organizationName" label="目标组织">
          <template #default="{ row }">{{ row.organizationName || '未指定' }}</template>
        </el-table-column>
        <el-table-column prop="invitedByUsername" label="邀请人" />
        <el-table-column prop="used" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.used ? 'success' : 'warning'" size="small">
              {{ row.used ? '已加入' : '待加入' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="邀请时间">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentEnterpriseSpace, getEnterpriseSpaceMembers } from '../api/enterpriseSpace'
import { createInvitation, getInvitations } from '../api/invitation'
import { useOrgStore } from '../stores/org'
import { isValidPhone, sanitizePhoneInput } from '../utils/phone'

const orgStore = useOrgStore()
const space = ref(null)
const members = ref([])
const invitations = ref([])
const inviteLoading = ref(false)
const inviteForm = reactive({
  invitedPhone: '',
  organizationId: null,
  validDays: 7,
})

onMounted(async () => {
  await Promise.all([loadSpace(), orgStore.fetchOrganizations(), loadInvitations()])
})

async function loadSpace() {
  try {
    const [{ data: spaceData }, { data: memberData }] = await Promise.all([
      getCurrentEnterpriseSpace(),
      getEnterpriseSpaceMembers(),
    ])
    space.value = spaceData
    members.value = memberData || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载企业空间失败')
  }
}

async function loadInvitations() {
  try {
    const { data } = await getInvitations()
    invitations.value = data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载邀请记录失败')
  }
}

async function submitInvite() {
  inviteForm.invitedPhone = sanitizePhoneInput(inviteForm.invitedPhone)
  if (!inviteForm.invitedPhone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!isValidPhone(inviteForm.invitedPhone)) {
    ElMessage.warning('请输入11位手机号')
    return
  }
  inviteLoading.value = true
  try {
    await createInvitation({
      invitedPhone: inviteForm.invitedPhone,
      organizationId: inviteForm.organizationId,
      validDays: inviteForm.validDays,
    })
    ElMessage.success('邀请已发送')
    inviteForm.invitedPhone = ''
    inviteForm.organizationId = null
    inviteForm.validDays = 7
    await loadInvitations()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '发送邀请失败')
  } finally {
    inviteLoading.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}
</script>

<style scoped>
.space-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.space-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.space-hero h2 {
  margin: 0;
}

.space-desc {
  margin: 8px 0 0;
  color: var(--text-secondary);
}

.space-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
