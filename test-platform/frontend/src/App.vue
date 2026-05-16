<template>
  <router-view />
</template>

<script setup>
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

:root {
  --primary-gradient: linear-gradient(135deg, #3b82f6 0%, #0ea5e9 100%);
  --primary-soft-gradient: linear-gradient(180deg, rgba(59, 130, 246, 0.14) 0%, rgba(14, 165, 233, 0.05) 100%);
  --primary-color: #3b82f6;
  --primary-color-light: #60a5fa;
  --secondary-color: #0f172a;
  --accent-color: #14b8a6;
  --page-bg: #f3f7fb;
  --page-bg-strong: #eef4f9;
  --surface-color: rgba(255, 255, 255, 0.92);
  --surface-strong: #ffffff;
  --surface-muted: #f8fbff;
  --sidebar-bg: linear-gradient(180deg, #0f172a 0%, #162033 100%);
  --sidebar-text: #d9e4f5;
  --sidebar-hover: rgba(96, 165, 250, 0.12);
  --sidebar-active: rgba(96, 165, 250, 0.18);
  --line-color: rgba(148, 163, 184, 0.22);
  --card-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
  --card-hover-shadow: 0 22px 50px rgba(15, 23, 42, 0.1);
  --text-primary: #0f172a;
  --text-secondary: #64748b;
  --text-tertiary: #94a3b8;
  --success-color: #10b981;
  --warning-color: #f59e0b;
  --danger-color: #ef4444;
  --border-radius: 18px;
  --border-radius-sm: 12px;
  --transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

#app {
  font-family: 'Inter', 'Segoe UI', system-ui, -apple-system, sans-serif;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.1) 0, rgba(59, 130, 246, 0) 32%),
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.08) 0, rgba(20, 184, 166, 0) 28%),
    linear-gradient(180deg, #f8fbff 0%, var(--page-bg) 36%, var(--page-bg-strong) 100%);
}

body {
  background: var(--page-bg);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--text-primary);
}

html,
body,
#app {
  min-height: 100%;
}

body,
button,
input,
textarea,
select {
  font-family: inherit;
}

.el-button--primary {
  --el-button-bg-color: var(--primary-color);
  --el-button-border-color: var(--primary-color);
  --el-button-hover-bg-color: #4c91f7;
  --el-button-hover-border-color: #4c91f7;
  --el-button-active-bg-color: #2f76ea;
  --el-button-active-border-color: #2f76ea;
}

.el-menu {
  --el-menu-bg-color: transparent !important;
  --el-menu-text-color: var(--sidebar-text);
  --el-menu-hover-bg-color: var(--sidebar-hover);
  --el-menu-active-color: #fff;
  --el-menu-item-height: 48px;
}

.el-menu-item.is-active {
  background: var(--sidebar-active) !important;
  border-right: 3px solid var(--primary-color-light);
}

.el-card {
  --el-card-border-radius: var(--border-radius);
  background: var(--surface-color) !important;
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.72) !important;
  box-shadow: var(--card-shadow) !important;
  transition: var(--transition) !important;
}

.el-card:hover {
  box-shadow: var(--card-hover-shadow) !important;
}

.el-input__wrapper {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.14) inset !important;
  transition: var(--transition);
}

.el-input__wrapper.is-focus,
.el-textarea__inner:focus,
.el-select__wrapper.is-focused {
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.3) inset,
    0 0 0 4px rgba(59, 130, 246, 0.08) !important;
}

.el-textarea__inner,
.el-select__wrapper,
.el-input-number {
  border-radius: 12px;
}

.el-button {
  border-radius: 12px;
  font-weight: 600;
  transition: var(--transition);
}

.el-button:not(.is-text):not(.is-link) {
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.05);
}

.el-button:hover {
  transform: translateY(-1px);
}

.el-tabs--top .el-tabs__item.is-active {
  color: var(--primary-color);
}

.el-tabs--top .el-tabs__active-bar {
  background-color: var(--primary-color);
}

::selection {
  background-color: rgba(59, 130, 246, 0.18);
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(59, 130, 246, 0.28);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(59, 130, 246, 0.45);
}

.el-page-header {
  padding: 0;
}

.el-page-header__back {
  color: var(--primary-color);
  transition: var(--transition);
}

.el-page-header__back:hover {
  color: #1d4ed8;
}

.el-page-header__content {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.el-dialog__footer {
  padding: 16px 20px;
  border-top: 1px solid #f1f5f9;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.el-image-viewer__wrapper {
  z-index: 3000 !important;
}

.el-table {
  --el-table-border-color: rgba(226, 232, 240, 0.85);
  --el-table-header-bg-color: #f8fbff;
  --el-table-row-hover-bg-color: rgba(59, 130, 246, 0.04);
}

.el-tag {
  border-radius: 999px;
}

.el-dialog,
.el-drawer {
  backdrop-filter: blur(14px);
}
</style>
