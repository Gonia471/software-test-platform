<template>
  <div class="tree-node" :class="{ 'is-folder': node.type === 'folder' }">
    <div
      class="tree-node-row"
      :class="{ 'is-selected': node.id === selectedId && node.type === 'case' }"
      @click="handleClick"
    >
      <span class="node-expand" @click.stop="toggleExpand">
        <el-icon v-if="node.type === 'folder' && hasChildren">
          <component :is="expanded ? ArrowDown : ArrowRight" />
        </el-icon>
        <span v-else class="node-expand-placeholder" />
      </span>
      <el-icon v-if="node.type === 'folder'" class="node-icon folder">
        <Folder />
      </el-icon>
      <el-icon v-else class="node-icon case" :style="{ color: methodColor }">
        <Document />
      </el-icon>
      <span class="node-name" :title="node.name">{{ node.name }}</span>
      <span v-if="node.type === 'case'" class="node-method">{{ node.method || 'GET' }}</span>
      <div v-if="node.id !== 'root'" class="node-actions" @click.stop>
        <el-dropdown trigger="click" @command="(c) => handleCommand(c, node)">
          <el-button link size="small" :icon="MoreFilled" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="node.type === 'folder'" command="add-folder">
                新建文件夹
              </el-dropdown-item>
              <el-dropdown-item v-if="node.type === 'folder'" command="add-case">
                新建接口
              </el-dropdown-item>
              <el-dropdown-item command="rename">重命名</el-dropdown-item>
              <el-dropdown-item v-if="node.id !== 'root'" command="delete" divided>删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <div v-if="expanded && hasChildren" class="tree-node-children">
      <TreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :search-text="searchText"
        :selected-id="selectedId"
        :parent-id="node.id"
        @select="$emit('select', $event)"
        @add-folder="$emit('add-folder', $event)"
        @add-case="$emit('add-case', $event)"
        @rename="(id, name) => $emit('rename', id, name)"
        @delete="$emit('delete', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Folder, Document, ArrowDown, ArrowRight, MoreFilled } from '@element-plus/icons-vue'

const props = defineProps({
  node: { type: Object, required: true },
  searchText: { type: String, default: '' },
  selectedId: { type: String, default: null },
  parentId: { type: String, default: null },
})

const emit = defineEmits(['select', 'add-folder', 'add-case', 'rename', 'delete'])

const expanded = ref(true)

const hasChildren = computed(() => {
  const c = props.node?.children
  return Array.isArray(c) && c.length > 0
})

const methodColor = computed(() => {
  const m = (props.node?.method || 'GET').toUpperCase()
  const map = {
    GET: '#2563eb',
    POST: '#16a34a',
    PUT: '#ca8a04',
    DELETE: '#dc2626',
    PATCH: '#9333ea',
  }
  return map[m] || '#6b7280'
})

watch(
  () => props.searchText,
  (t) => {
    if (t?.trim() && props.node.type === 'folder') {
      expanded.value = true
    }
  },
)

function toggleExpand() {
  if (props.node.type === 'folder' && hasChildren.value) {
    expanded.value = !expanded.value
  }
}

function handleClick() {
  if (props.node.type === 'case') {
    emit('select', props.node.id)
  } else if (props.node.type === 'folder' && hasChildren.value) {
    toggleExpand()
  }
}

async function handleCommand(cmd, node) {
  if (cmd === 'add-folder') emit('add-folder', node.id)
  else if (cmd === 'add-case') emit('add-case', node.id)
  else if (cmd === 'rename') {
    try {
      const { value } = await ElMessageBox.prompt('请输入新的节点名称', '重命名', {
        inputValue: node.name,
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '名称不能为空',
      })
      if (value && value.trim() && value.trim() !== node.name) {
        emit('rename', node.id, value.trim())
      }
    } catch {
      // ignore cancel
    }
  }
  else if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm(`确定删除「${node.name}」？删除后无法恢复。`, '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })
      emit('delete', node.id)
    } catch {
      // ignore cancel
    }
  }
}
</script>

<style scoped>
.tree-node {
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tree-node-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.5);
}

.tree-node-row:hover {
  background: #ffffff;
  border-color: rgba(226, 232, 240, 0.95);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.05);
}

.tree-node-row.is-selected {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: var(--primary-color);
  border-color: rgba(59, 130, 246, 0.25);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.08);
}

.node-expand {
  width: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-secondary);
}

.node-expand-placeholder {
  display: inline-block;
  width: 18px;
}

.node-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.node-icon.folder {
  color: #f59e0b;
}

.node-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
  font-weight: 500;
}

.node-method {
  font-size: 11px;
  color: var(--text-secondary);
  font-weight: 700;
  background: #f8fbff;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.node-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node-row:hover .node-actions {
  opacity: 1;
}

.tree-node-children {
  margin-left: 24px;
  border-left: 1px dashed rgba(148, 163, 184, 0.28);
  padding-left: 8px;
}
</style>
