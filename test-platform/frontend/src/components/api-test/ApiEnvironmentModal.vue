<template>
  <el-dialog
    v-model="visible"
    title="环境管理"
    width="560px"
    destroy-on-close
    @close="handleClose"
  >
    <div class="env-manager">
      <div class="env-list">
        <div
          v-for="env in environments"
          :key="env.id"
          class="env-item"
          :class="{ 'is-active': env.id === currentEnvId }"
          @click="selectEnv(env.id)"
        >
          <span class="env-name">{{ env.name }}</span>
          <el-button
            link
            type="danger"
            size="small"
            :icon="Delete"
            @click.stop="removeEnv(env.id)"
          />
        </div>
      </div>
      <el-button type="primary" plain size="small" :icon="Plus" @click="addEnv">
        添加环境
      </el-button>

      <div v-if="editingEnv" class="env-editor">
        <h4>{{ editingEnv.name }}</h4>
        <div class="var-list">
          <div
            v-for="(v, idx) in editingEnv.variables"
            :key="idx"
            class="var-row"
          >
            <el-input v-model="v.key" placeholder="变量名" size="small" style="width: 140px" />
            <el-input v-model="v.value" placeholder="值" size="small" style="flex: 1" />
            <el-button link type="danger" size="small" @click="removeVar(idx)">
              删除
            </el-button>
          </div>
        </div>
        <el-button size="small" @click="addVar">添加变量</el-button>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  environments: { type: Array, default: () => [] },
  currentEnvId: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue', 'update-env', 'remove-env', 'add-env', 'set-current'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const editingEnv = ref(null)

watch(
  () => props.modelValue,
  (v) => {
    if (v && props.currentEnvId) {
      editingEnv.value = props.environments.find((e) => e.id === props.currentEnvId) || null
    }
  },
)

function selectEnv(id) {
  emit('set-current', id)
  editingEnv.value = props.environments.find((e) => e.id === id) || null
}

function addEnv() {
  emit('add-env')
}

function removeEnv(id) {
  emit('remove-env', id)
  if (editingEnv.value?.id === id) {
    editingEnv.value = props.environments.find((e) => e.id !== id) || null
  }
}

function addVar() {
  if (!editingEnv.value) return
  editingEnv.value.variables = editingEnv.value.variables || []
  editingEnv.value.variables.push({ key: '', value: '' })
  emit('update-env', editingEnv.value.id, { variables: editingEnv.value.variables })
}

function removeVar(idx) {
  if (!editingEnv.value?.variables) return
  editingEnv.value.variables.splice(idx, 1)
  emit('update-env', editingEnv.value.id, { variables: editingEnv.value.variables })
}

function handleClose() {
  editingEnv.value = null
}
</script>

<style scoped>
.env-manager {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.env-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.env-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.15s;
}

.env-item:hover {
  background: #f9fafb;
}

.env-item.is-active {
  border-color: #2563eb;
  background: #eff6ff;
}

.env-name {
  font-weight: 500;
}

.env-editor {
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.env-editor h4 {
  margin: 0 0 12px;
  font-size: 14px;
}

.var-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.var-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
