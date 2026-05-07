<template>
  <div class="key-value-editor">
    <div class="kv-header">
      <span class="kv-col-check"></span>
      <span class="kv-col">KEY</span>
      <span class="kv-col">VALUE</span>
      <span class="kv-col-actions">操作</span>
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
        <el-button link type="danger" size="small" @click="remove(idx)">
          删除
        </el-button>
      </div>
    </div>
    <el-button size="small" type="primary" plain @click="add" class="add-btn">
      {{ addLabel }}
    </el-button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  addLabel: { type: String, default: '添加' },
  keyPlaceholder: { type: String, default: '参数名' },
  valuePlaceholder: { type: String, default: '值' },
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
  padding: 12px 16px 16px;
}

.kv-header {
  display: grid;
  grid-template-columns: 24px 1fr 1fr 80px;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  padding: 0 16px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  letter-spacing: 0.02em;
  border-bottom: 1px solid #e5e7eb;
}

.kv-header .kv-col-check {
  width: 24px;
}

.kv-header .kv-col {
  text-align: center;
}

.kv-header .kv-col-actions {
  text-align: center;
}

.kv-row-box {
  display: grid;
  grid-template-columns: 24px 1fr 1fr 80px;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
  padding: 10px 16px;
  background: #fafafa;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: border-color 0.15s, background 0.15s;
}

.kv-row-box:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.kv-row-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
}

.kv-cell-check {
  justify-content: flex-start;
}

.kv-cell-key,
.kv-cell-value {
  justify-content: stretch;
}

.kv-cell-key .kv-input,
.kv-cell-value .kv-input {
  width: 100%;
}

.kv-cell-actions {
  justify-content: center;
}

.kv-input {
  width: 100%;
}

.kv-input :deep(.el-input__wrapper) {
  border-radius: 6px;
}

.add-btn {
  margin-top: 14px;
}
</style>
