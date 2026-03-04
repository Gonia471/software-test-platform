<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo-wrapper">
        <span class="logo-text">
          {{ isCollapse ? 'ET' : 'EasyTest 测试平台' }}
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
          <span>系统设置</span>
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
          <span class="system-name">EasyTest 软件测试系统</span>
          <el-dropdown>
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
                <el-dropdown-item @click="goProfile">个人中心（占位）</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import {
  Odometer,
  FolderOpened,
  ChromeFilled,
  Monitor,
  Document,
  Setting,
  ArrowDown,
  Fold,
  Expand,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/api-test')) return '/api-test'
  if (path.startsWith('/ui-test')) return '/ui-test'
  if (path.startsWith('/projects')) return '/projects'
  if (path.startsWith('/reports')) return '/reports'
  if (path.startsWith('/settings')) return '/settings'
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
  // 预留：未来可跳转到用户信息页
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.sidebar {
  background-color: #0f172a;
  color: #e5e7eb;
  display: flex;
  flex-direction: column;
}

.logo-wrapper {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.35);
  box-sizing: border-box;
}

.logo-text {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu {
  border-right: none;
  flex: 1;
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 16px;
  box-sizing: border-box;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  color: #4b5563;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.system-name {
  font-size: 14px;
  color: #6b7280;
}

.user-dropdown {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}

.avatar {
  background: #4f46e5;
  font-size: 12px;
}

.username {
  margin: 0 8px;
  font-size: 14px;
  color: #374151;
}

.dropdown-icon {
  font-size: 12px;
  color: #9ca3af;
}

.main-content {
  background-color: #f3f4f6;
  padding: 16px;
  box-sizing: border-box;
}
</style>
