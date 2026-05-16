<template>
  <div class="key-value-editor">
    <div class="kv-header">
      <span class="kv-col-check"></span>
      <span class="kv-col">参数名</span>
      <span class="kv-col">参数值</span>
      <span class="kv-col-actions"></span>
    </div>
    <div
      v-for="(item, idx) in innerList"
      :key="idx"
      class="kv-row-box"
    >
      <div class="kv-row-cell kv-cell-check">
        <el-checkbox v-model="item.enabled" />
      </div>
      <div class="kv-row-cell kv-cell-key">
        <el-input
          v-model="item.key"
          :placeholder="keyPlaceholder"
          size="small"
          class="kv-input"
          @input="emitChange"
        />
      </div>
      <div class="kv-row-cell kv-cell-value">
        <el-input
          v-model="item.value"
          :placeholder="valuePlaceholder"
          size="small"
          class="kv-input"
          @input="emitChange"
        />
      </div>
      <div class="kv-row-cell kv-cell-actions">
        <el-icon class="kv-delete-btn" @click="remove(idx)"><Delete /></el-icon>
      </div>
    </div>
    <el-button size="small" @click="add" class="add-btn">
      <el-icon><Plus /></el-icon>
      {{ addLabel }}
    </el-button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  addLabel: { type: String, default: '添加' },
  keyPlaceholder: { type: String, default: '参数名' },
  valuePlaceholder: { type: String, default: '参数值' },
})

const emit = defineEmits(['update:modelValue', 'change'])

const innerList = ref(normalizeList(props.modelValue))

function normalizeList(arr) {
  if (!Array.isArray(arr)) return [{ key: '', value: '', enabled: true }]
  if (arr.length === 0) return [{ key: '', value: '', enabled: true }]
  return arr.map((it) => ({
    key: typeof it === 'object' ? it.key ?? '' : '',
    value: typeof it === 'object' ? it.value ?? '' : '',
    enabled: typeof it === 'object' && 'enabled' in it ? it.enabled : true,
  }))
}

watch(
  () => props.modelValue,
  (val) => {
    innerList.value = normalizeList(val)
  },
  { deep: true },
)

function add() {
  innerList.value.push({ key: '', value: '', enabled: true })
  emitChange()
}

function remove(idx) {
  innerList.value.splice(idx, 1)
  emitChange()
}

function emitChange() {
  const out = innerList.value.map(({ key, value, enabled }) => ({ key, value, enabled }))
  emit('update:modelValue', out)
  emit('change', out)
}
</script>

<style scoped>
.key-value-editor {
  padding: 4px 0 0;
}

.kv-header {
  display: grid;
  grid-template-columns: 28px 1fr 1fr 36px;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  padding: 0 12px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.kv-header .kv-col-check {
  width: 28px;
}

.kv-header .kv-col {
  text-align: left;
}

.kv-header .kv-col-actions {
  width: 36px;
}

.kv-row-box {
  display: grid;
  grid-template-columns: 28px 1fr 1fr 36px;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
  padding: 12px 12px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 16px;
  transition: var(--transition);
}

.kv-row-box:hover {
  background: #ffffff;
  border-color: rgba(59, 130, 246, 0.18);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.06);
}

.kv-row-box:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.08);
}

.kv-row-cell {
  display: flex;
  align-items: center;
  min-width: 0;
}

.kv-cell-check {
  justify-content: flex-start;
}

.kv-cell-key,
.kv-cell-value {
  flex: 1;
}

.kv-cell-actions {
  justify-content: center;
}

.kv-delete-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 8px;
  transition: var(--transition);
}

.kv-delete-btn:hover {
  color: #ef4444;
  background: #fef2f2;
}

.kv-input {
  width: 100%;
}

.kv-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #f8fbff;
  padding: 8px 10px;
}

.kv-input :deep(.el-input__inner) {
  font-size: 13px;
}

.add-btn {
  margin-top: 10px;
  border-radius: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  background: #f8fbff;
  border: 1px dashed rgba(148, 163, 184, 0.32);
  width: 100%;
  transition: var(--transition);
  min-height: 42px;
}

.add-btn:hover {
  background: #fff;
  border-color: var(--primary-color);
  color: var(--primary-color);
}
</style>
