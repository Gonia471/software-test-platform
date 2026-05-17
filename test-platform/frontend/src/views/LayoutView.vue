<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo-wrapper">
        <span v-if="!isCollapse" class="logo-badge">EasyTest</span>
        <span class="logo-text">
          {{ isCollapse ? 'ET' : '软件测试平台' }}
        </span>
      </div>
      <el-menu
        router
        :default-active="activeMenu"
        :collapse="isCollapse"
        class="menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/organizations">
          <el-icon><OfficeBuilding /></el-icon>
          <span>组织管理</span>
        </el-menu-item>
        <el-menu-item index="/projects">
          <el-icon><FolderOpened /></el-icon>
          <span>项目管理</span>
        </el-menu-item>
        <el-menu-item index="/api-test">
          <el-icon><ChromeFilled /></el-icon>
          <span>API 测试</span>
        </el-menu-item>
        <el-menu-item index="/ui-test">
          <el-icon><Monitor /></el-icon>
          <span>UI 测试</span>
        </el-menu-item>
        <el-menu-item index="/reports">
          <el-icon><Document /></el-icon>
          <span>测试报告</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <span>用户手册</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="main-header">
        <div class="header-left">
          <el-button
            link
            class="collapse-btn"
            @click="isCollapse = !isCollapse"
          >
            <el-icon>
              <component :is="isCollapse ? Expand : Fold" />
            </el-icon>
          </el-button>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleOrgChange" v-if="orgStore.organizations.length > 0">
            <span class="workspace-selector">
              <el-icon><OfficeBuilding /></el-icon>
              <span>{{ orgStore.currentOrganization?.name || '选择组织' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="org in orgStore.organizations"
                  :key="org.id"
                  :command="org.id"
                  :selected="org.id === orgStore.currentOrganizationId"
                >
                  <el-icon><Check v-if="org.id === orgStore.currentOrganizationId" /></el-icon>
                  {{ org.name }}
                </el-dropdown-item>
                <el-dropdown-item divided command="manage">管理组织</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-else size="small" @click="goOrganizations">创建组织</el-button>

          <span class="system-name">EasyTest 软件测试系统</span>
          <el-dropdown @command="handleUserCommand">
            <span class="user-dropdown">
              <el-avatar
                size="small"
                class="avatar"
              >
                {{ avatarText }}
              </el-avatar>
              <span class="username">{{ userStore.username || '未登录用户' }}</span>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useOrgStore } from '../stores/org'
import { ElMessage } from 'element-plus'
import {
  Odometer,
  FolderOpened,
  ChromeFilled,
  Monitor,
  Document,
  Setting,
  User,
  ArrowDown,
  Fold,
  Expand,
  OfficeBuilding,
  Check,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const orgStore = useOrgStore()

const isCollapse = ref(false)

onMounted(async () => {
  const organizations = await orgStore.fetchOrganizations()
  if (!organizations.length) {
    ElMessage.warning('当前无法获取组织列表，请确认登录状态或组织权限')
  }
})

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/api-test')) return '/api-test'
  if (path.startsWith('/ui-test')) return '/ui-test'
  if (path.startsWith('/projects')) return '/projects'
  if (path.startsWith('/reports')) return '/reports'
  if (path.startsWith('/settings')) return '/settings'
  if (path.startsWith('/profile')) return '/profile'
  return '/dashboard'
})

const avatarText = computed(() => {
  const name = userStore.username || 'User'
  return name.slice(0, 1).toUpperCase()
})

function logout() {
  userStore.logout()
  router.push('/login')
}

function goProfile() {
  router.push('/profile')
}

function goOrganizations() {
  router.push('/organizations')
}

function handleUserCommand(command) {
  if (command === 'profile') {
    goProfile()
  } else if (command === 'logout') {
    logout()
  }
}

function handleOrgChange(command) {
  if (command === 'manage') {
    router.push('/organizations')
  } else {
    orgStore.setCurrentOrganization(command)
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
  background: transparent;
}

.sidebar {
  background: var(--sidebar-bg);
  color: var(--sidebar-text);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 18px 0 48px rgba(15, 23, 42, 0.14);
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 200px;
  background: linear-gradient(180deg, rgba(99, 102, 241, 0.15) 0%, transparent 100%);
  pointer-events: none;
}

.logo-wrapper {
  padding: 18px 16px 20px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
  z-index: 1;
}

.logo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(96, 165, 250, 0.14);
  color: #bfdbfe;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff 0%, #dbeafe 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.03em;
}

.menu {
  border: none;
  flex: 1;
  overflow-y: auto;
  position: relative;
  z-index: 1;
}

.menu:not(.el-menu--collapse) {
  width: 220px;
}

.el-menu--collapse .logo-text {
  font-size: 16px;
}

.main-header {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 68px;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 20px;
  color: var(--text-secondary);
  width: 40px;
  height: 40px;
  border-radius: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.workspace-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border-radius: 14px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-primary);
  background: rgba(248, 250, 252, 0.78);
  border: 1px solid rgba(226, 232, 240, 0.9);
  transition: var(--transition);
}

.workspace-selector:hover {
  background: #ffffff;
  border-color: rgba(59, 130, 246, 0.22);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.system-name {
  font-size: 14px;
  color: var(--text-secondary);
  padding-left: 14px;
  border-left: 1px solid rgba(226, 232, 240, 0.9);
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 14px;
  border: 1px solid transparent;
  transition: var(--transition);
}

.user-dropdown:hover {
  background: rgba(255, 255, 255, 0.88);
  border-color: rgba(226, 232, 240, 0.95);
}

.avatar {
  background: var(--primary-gradient);
  color: #fff;
  box-shadow: 0 10px 18px rgba(59, 130, 246, 0.24);
}

.username {
  font-size: 14px;
  color: var(--text-primary);
}

.dropdown-icon {
  color: var(--text-secondary);
  font-size: 12px;
}

.main-content {
  padding: 22px;
  background: transparent;
  overflow-y: auto;
}
</style>
