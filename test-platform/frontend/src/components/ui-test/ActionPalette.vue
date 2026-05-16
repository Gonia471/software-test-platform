<template>
  <el-card class="panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <div class="panel-header-left">
          <span class="panel-title">动作组件库</span>
          <span class="panel-subtitle">拖拽动作到中间步骤区</span>
        </div>
        <button
          type="button"
          class="toggle-all-btn"
          @click="toggleAll"
        >
          {{ allCollapsed ? '全部展开' : '全部折叠' }}
        </button>
      </div>
    </template>

    <div class="groups">
      <div
        v-for="group in groups"
        :key="group.type"
        class="group"
      >
        <div class="group-title" @click="toggleGroup(group.type)">
          <el-icon
            class="group-arrow"
            :class="{ 'is-collapsed': isCollapsed(group.type) }"
          >
            <ArrowDown />
          </el-icon>
          <span>{{ group.title }}</span>
        </div>
        <draggable
          v-if="!isCollapsed(group.type)"
          :list="group.actions"
          :group="dragGroup"
          item-key="key"
          :sort="false"
          :clone="(action) => createStepFromAction(action, group.type)"
          class="action-list"
        >
          <template #item="{ element }">
            <div class="action-item">
              <div class="action-main">
                <div class="action-name">{{ element.label }}</div>
                <div class="action-desc">{{ element.description }}</div>
              </div>
              <el-tag size="small" type="info" effect="plain">
                拖拽
              </el-tag>
            </div>
          </template>
        </draggable>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import draggable from 'vuedraggable'

const props = defineProps({
  groups: {
    type: Array,
    required: true,
  },
  dragGroup: {
    type: Object,
    required: true,
  },
  createStepFromAction: {
    type: Function,
    required: true,
  },
})

const collapsedTypes = ref([])

watch(
  () => props.groups,
  (groups) => {
    const types = (groups || []).map((g) => g.type)
    collapsedTypes.value = collapsedTypes.value.filter((t) => types.includes(t))
  },
  { immediate: true },
)

const allCollapsed = computed(() => {
  if (!props.groups || !props.groups.length) return false
  return collapsedTypes.value.length === props.groups.length
})

function isCollapsed(type) {
  return collapsedTypes.value.includes(type)
}

function toggleGroup(type) {
  if (isCollapsed(type)) {
    collapsedTypes.value = collapsedTypes.value.filter((t) => t !== type)
  } else {
    collapsedTypes.value = [...collapsedTypes.value, type]
  }
}

function toggleAll() {
  if (allCollapsed.value) {
    collapsedTypes.value = []
  } else {
    collapsedTypes.value = (props.groups || []).map((g) => g.type)
  }
}
</script>

<style scoped>
.panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: var(--border-radius);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  background: linear-gradient(135deg, #fbfdff 0%, #f3f8ff 100%);
  min-height: 86px;
  box-sizing: border-box;
}

.panel-header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.panel-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.panel-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
}

.groups {
  flex: 1;
  overflow: auto;
  padding: 14px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.92) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.group + .group {
  margin-top: 10px;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  padding: 12px 14px;
  cursor: pointer;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  transition: var(--transition);
}

.group-title:hover {
  background: #f8fbff;
  border-color: rgba(59, 130, 246, 0.2);
}

.group-arrow {
  font-size: 12px;
  transition: transform 0.2s ease;
  color: var(--text-secondary);
}

.group-arrow.is-collapsed {
  transform: rotate(-90deg);
}

.toggle-all-btn {
  font-size: 11px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: rgba(255, 255, 255, 0.94);
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition);
  font-weight: 600;
}

.toggle-all-btn:hover {
  background: var(--primary-color);
  color: #fff;
  border-color: var(--primary-color);
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.action-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: rgba(255, 255, 255, 0.94);
  cursor: grab;
  transition: var(--transition);
}

.action-item:hover {
  border-color: var(--primary-color-light);
  background: #ffffff;
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.action-item:active {
  cursor: grabbing;
  transform: translateY(0);
}

.action-main {
  min-width: 0;
  flex: 1;
}

.action-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.action-desc {
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.el-tag) {
  border-radius: 999px;
  font-size: 10px;
  padding: 0 8px;
  border-color: rgba(59, 130, 246, 0.16);
  background: rgba(59, 130, 246, 0.08);
  color: #2563eb;
}
</style>
