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
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.panel-header-left {
  display: flex;
  flex-direction: column;
}

.panel-title {
  font-weight: 600;
}

.panel-subtitle {
  font-size: 12px;
  color: #9ca3af;
}

.groups {
  flex: 1;
  overflow: auto;
  padding-right: 4px;
}

.group + .group {
  margin-top: 12px;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
  cursor: pointer;
}

.group-arrow {
  font-size: 12px;
  transition: transform 0.15s ease-out;
}

.group-arrow.is-collapsed {
  transform: rotate(-90deg);
}

.toggle-all-btn {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background-color: #f9fafb;
  color: #4b5563;
  cursor: pointer;
}

.toggle-all-btn:hover {
  background-color: #e5e7eb;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 6px;
  border: 1px dashed #e5e7eb;
  background-color: #f9fafb;
  cursor: grab;
}

.action-main {
  min-width: 0;
}

.action-name {
  font-size: 13px;
  font-weight: 500;
}

.action-desc {
  font-size: 12px;
  color: #9ca3af;
}
</style>

