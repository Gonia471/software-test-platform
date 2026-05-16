<template>
  <el-dialog
    v-model="visible"
    title="环境管理"
    width="680px"
    destroy-on-close
    append-to-body
    @close="handleClose"
  >
    <div class="env-manager">
      <div class="env-toolbar">
        <div class="toolbar-meta">
          <span class="meta-chip">{{ environments.length }} 个环境</span>
          <span class="meta-tip">支持设置变量，并在 URL、请求头、认证信息中通过 <code v-pre>{{变量名}}</code> 方式引用</span>
        </div>
        <el-button type="primary" plain :icon="Plus" @click="addEnv">
          添加环境
        </el-button>
      </div>

      <div class="env-layout">
        <div class="env-list">
          <div
            v-for="env in environments"
            :key="env.id"
            class="env-item"
            :class="{ 'is-active': env.id === currentEnvId }"
            @click="selectEnv(env.id)"
          >
            <div class="env-item__main">
              <span class="env-name">{{ env.name }}</span>
              <span class="env-count">{{ env.variables?.length || 0 }} 个变量</span>
            </div>
            <el-button
              link
              type="danger"
              size="small"
              :icon="Delete"
              @click.stop="removeEnv(env.id)"
            />
          </div>
        </div>

        <div v-if="editingEnv" class="env-editor">
          <div class="env-editor__header">
            <div>
              <h4>{{ editingEnv.name }}</h4>
              <p>当前环境下的变量可在请求配置中直接引用</p>
            </div>
            <el-button size="small" @click="addVar">添加变量</el-button>
          </div>
          <div class="env-name-edit">
            <span class="field-label">环境名称</span>
            <el-input
              v-model="editingEnv.name"
              placeholder="请输入环境名称"
              @change="updateEnvName"
            />
          </div>
          <div class="var-list">
            <div
              v-for="(v, idx) in editingEnv.variables"
              :key="idx"
              class="var-row"
            >
              <el-input v-model="v.key" placeholder="变量名" size="small" class="var-key" @change="syncVariables" />
              <el-input v-model="v.value" placeholder="值" size="small" class="var-value" @change="syncVariables" />
              <el-button link type="danger" size="small" @click="removeVar(idx)">
                删除
              </el-button>
            </div>
          </div>
          <div v-if="!editingEnv.variables?.length" class="env-empty">
            暂无环境变量，点击上方“添加变量”开始配置。
          </div>
        </div>
        <div v-else class="env-empty-panel">
          <span class="env-empty-panel__title">请选择一个环境</span>
          <p class="env-empty-panel__desc">左侧选择环境后，可在这里维护变量和环境名称。</p>
        </div>
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
  [() => props.modelValue, () => props.currentEnvId, () => props.environments],
  ([visibleValue]) => {
    if (visibleValue) {
      syncEditingEnv()
    }
  },
  { deep: true },
)

function selectEnv(id) {
  emit('set-current', id)
  syncEditingEnv(id)
}

function addEnv() {
  emit('add-env')
}

function removeEnv(id) {
  emit('remove-env', id)
}

function addVar() {
  if (!editingEnv.value) return
  editingEnv.value.variables = editingEnv.value.variables || []
  editingEnv.value.variables.push({ key: '', value: '' })
  syncVariables()
}

function removeVar(idx) {
  if (!editingEnv.value?.variables) return
  editingEnv.value.variables.splice(idx, 1)
  syncVariables()
}

function updateEnvName() {
  if (!editingEnv.value) return
  emit('update-env', editingEnv.value.id, { name: editingEnv.value.name })
}

function syncVariables() {
  if (!editingEnv.value) return
  emit('update-env', editingEnv.value.id, {
    variables: editingEnv.value.variables || [],
  })
}

function syncEditingEnv(targetId = props.currentEnvId) {
  const target = props.environments.find((env) => String(env.id) === String(targetId))
  editingEnv.value = target
    ? {
        ...target,
        variables: Array.isArray(target.variables)
          ? target.variables.map((variable) => ({
              key: variable.key || '',
              value: variable.value || '',
            }))
          : [],
      }
    : null
}

function handleClose() {
  editingEnv.value = null
}
</script>

<style scoped>
.env-manager {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.env-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.toolbar-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.meta-tip {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.env-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 16px;
  min-height: 320px;
}

.env-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.env-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: rgba(255, 255, 255, 0.94);
  cursor: pointer;
  transition: var(--transition);
}

.env-item:hover {
  background: #ffffff;
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.06);
}

.env-item.is-active {
  border-color: rgba(59, 130, 246, 0.35);
  background: #eef6ff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.env-item__main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.env-name {
  font-weight: 600;
  color: var(--text-primary);
}

.env-count {
  font-size: 12px;
  color: var(--text-tertiary);
}

.env-editor {
  padding: 18px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.92) 0%, rgba(255, 255, 255, 0.98) 100%);
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.env-editor__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.env-editor h4 {
  margin: 0 0 6px;
  font-size: 18px;
  color: var(--text-primary);
}

.env-editor p {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.env-name-edit {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.field-label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 600;
}

.var-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.var-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.var-key {
  width: 160px;
}

.var-value {
  flex: 1;
}

.env-empty {
  padding: 24px;
  text-align: center;
  border-radius: 14px;
  border: 1px dashed rgba(148, 163, 184, 0.3);
  color: var(--text-tertiary);
  background: rgba(255, 255, 255, 0.76);
}

.env-empty-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 30px 20px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.92) 0%, rgba(255, 255, 255, 0.98) 100%);
  border: 1px dashed rgba(148, 163, 184, 0.28);
}

.env-empty-panel__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.env-empty-panel__desc {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

@media (max-width: 900px) {
  .env-layout {
    grid-template-columns: 1fr;
  }
}
</style>
