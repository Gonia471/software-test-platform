<template>
  <div class="api-collection-tree">
    <div class="tree-header">
      <div class="tree-header__main">
        <span class="tree-title">接口集合</span>
        <span class="tree-subtitle">按目录组织接口，支持快速搜索与环境管理</span>
      </div>
      <div class="tree-actions">
        <el-dropdown trigger="click" @command="handleAdd">
          <el-button type="primary" size="small" :icon="Plus">
            新建
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="folder">新建文件夹</el-dropdown-item>
              <el-dropdown-item command="case">新建接口</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button
          link
          size="small"
          :icon="Setting"
          title="环境管理"
          @click="$emit('open-env')"
        />
      </div>
    </div>
    <div class="tree-search">
      <el-input
        v-model="searchText"
        placeholder="搜索接口或集合..."
        size="small"
        clearable
        :prefix-icon="Search"
      />
    </div>
    <div class="tree-body">
      <div
        v-for="node in filteredRoots"
        :key="node.id"
        class="tree-node-wrapper"
      >
        <TreeNode
          :node="node"
          :search-text="searchText"
          :selected-id="selectedCaseId"
          :parent-id="node.id === 'root' ? null : node.parentId"
          @select="onSelect"
          @add-folder="onAddFolder"
          @add-case="onAddCase"
          @rename="onRename"
          @delete="onDelete"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Plus, Search, Setting } from '@element-plus/icons-vue'
import TreeNode from './TreeNode.vue'

const props = defineProps({
  collections: { type: Array, default: () => [] },
  selectedCaseId: { type: String, default: null },
})

const emit = defineEmits(['select', 'add-folder', 'add-case', 'rename', 'delete', 'open-env'])

const searchText = ref('')

function filterNodes(nodes, text) {
  if (!nodes || !text?.trim()) return nodes
  const t = text.trim().toLowerCase()
  return nodes.filter((n) => {
    const match = (n.name || '').toLowerCase().includes(t)
    const childMatch = n.children && filterNodes(n.children, text).length > 0
    return match || childMatch
  })
}

const filteredRoots = computed(() => {
  const roots = props.collections || []
  if (!searchText.value?.trim()) return roots
  return filterNodes(roots, searchText.value)
})

function handleAdd(cmd) {
  if (cmd === 'folder') {
    emit('add-folder', null)
  } else {
    emit('add-case', null)
  }
}

function onSelect(id) {
  emit('select', id)
}

function onAddFolder(parentId) {
  emit('add-folder', parentId)
}

function onAddCase(parentId) {
  emit('add-case', parentId)
}

function onRename(id, name) {
  emit('rename', id, name)
}

function onDelete(id) {
  emit('delete', id)
}
</script>

<style scoped>
.api-collection-tree {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.92) 0%, rgba(255, 255, 255, 0.98) 100%);
  border-radius: var(--border-radius);
  overflow: hidden;
}

.tree-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 18px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(135deg, #fbfdff 0%, #f3f8ff 100%);
}

.tree-header__main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tree-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.tree-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.tree-actions {
  display: flex;
  gap: 8px;
}

.tree-search {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
}

.tree-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
}
</style>
