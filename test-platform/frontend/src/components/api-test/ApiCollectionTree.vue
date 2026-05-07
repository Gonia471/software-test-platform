<template>
  <div class="api-collection-tree">
    <div class="tree-header">
      <span class="tree-title">接口集合</span>
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
    emit('add-folder', 'root')
  } else {
    emit('add-case', 'root')
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
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.tree-title {
  font-weight: 600;
  font-size: 15px;
  color: #111827;
}

.tree-actions {
  display: flex;
  gap: 6px;
}

.tree-search {
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
}

.tree-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}
</style>
