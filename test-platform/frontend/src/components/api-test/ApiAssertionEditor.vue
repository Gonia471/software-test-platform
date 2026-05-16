<template>
  <div class="assertion-editor">
    <div class="assertion-header">
      <span class="header-title">断言配置</span>
      <el-button type="primary" size="small" @click="addAssertion">
        <el-icon><Plus /></el-icon>
        添加断言
      </el-button>
    </div>

    <div class="assertion-list">
      <div v-if="assertions.length === 0" class="assertion-empty">
        暂无断言配置，点击上方按钮添加断言
      </div>

      <div
        v-for="(assertion, index) in assertions"
        :key="index"
        class="assertion-item"
      >
        <div class="assertion-row">
          <el-select v-model="assertion.assertionType" size="small" @change="handleChange">
            <el-option label="状态码" value="STATUS" />
            <el-option label="JSONPath" value="JSONPATH" />
            <el-option label="包含" value="CONTAINS" />
            <el-option label="响应时间" value="DURATION" />
            <el-option label="响应头" value="HEADERS" />
            <el-option label="函数断言" value="FUNCTION" />
          </el-select>

          <el-input
            v-model="assertion.expression"
            size="small"
            placeholder="表达式"
            class="assertion-expression"
            @change="handleChange"
          />

          <el-switch
            v-model="assertion.enabled"
            size="small"
            @change="handleChange"
          />

          <el-button
            type="danger"
            size="small"
            text
            @click="removeAssertion(index)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>

        <div v-if="assertion.assertionType === 'JSONPATH'" class="assertion-extras">
          <span class="extra-label">期望值：</span>
          <el-input
            v-model="assertion.expected"
            size="small"
            placeholder="期望值，支持比较符 == != > < >= <= 或 {{函数名(参数)}}"
            class="assertion-expected"
            @change="handleChange"
          />
        </div>

        <div v-if="assertion.assertionType === 'CONTAINS'" class="assertion-extras">
          <span class="extra-label">期望值：</span>
          <el-input
            v-model="assertion.expected"
            size="small"
            placeholder="期望包含的字符串"
            class="assertion-expected"
            @change="handleChange"
          />
        </div>

        <div v-if="assertion.assertionType === 'DURATION'" class="assertion-extras">
          <span class="extra-label">最大值：</span>
          <el-input
            v-model="assertion.expected"
            size="small"
            placeholder="最大响应时间（毫秒），如：1000"
            class="assertion-expected"
            @change="handleChange"
          />
        </div>

        <div v-if="assertion.assertionType === 'HEADERS'" class="assertion-extras">
          <span class="extra-label">期望值：</span>
          <el-input
            v-model="assertion.expected"
            size="small"
            placeholder="Header名:期望值，如：Content-Type:application/json"
            class="assertion-expected"
            @change="handleChange"
          />
        </div>

        <div v-if="assertion.assertionType === 'FUNCTION'" class="assertion-extras">
          <span class="extra-label">函数调用：</span>
          <el-input
            v-model="assertion.functionCall"
            size="small"
            placeholder="函数调用，如：validate_response(response_data)"
            class="assertion-expected"
            @change="handleChange"
          />
        </div>

        <div class="assertion-hint">
          <span v-if="assertion.assertionType === 'STATUS'">期望状态码，如：200</span>
          <span v-else-if="assertion.assertionType === 'JSONPATH'">JSON路径，如：$.data.id，支持比较符和函数调用</span>
          <span v-else-if="assertion.assertionType === 'CONTAINS'">期望包含的字符串</span>
          <span v-else-if="assertion.assertionType === 'DURATION'">最大响应时间（毫秒）</span>
          <span v-else-if="assertion.assertionType === 'HEADERS'">Header名:期望值</span>
          <span v-else-if="assertion.assertionType === 'FUNCTION'">调用Python函数进行断言，返回True/False</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const assertions = ref([...props.modelValue])

watch(assertions, (val) => {
  emit('update:modelValue', val)
}, { deep: true })

function handleChange() {
  emit('update:modelValue', assertions.value)
}

function addAssertion() {
  assertions.value.push({
    assertionType: 'STATUS',
    expression: '200',
    expected: '',
    functionCall: '',
    enabled: true
  })
  handleChange()
}

function removeAssertion(index) {
  assertions.value.splice(index, 1)
  handleChange()
}
</script>

<style scoped>
.assertion-editor {
  padding: 16px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.9) 0%, rgba(255, 255, 255, 0.98) 100%);
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.assertion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
}

.header-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.assertion-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.assertion-empty {
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 14px;
  border: 1px dashed rgba(148, 163, 184, 0.3);
}

.assertion-item {
  background: rgba(255, 255, 255, 0.96);
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.assertion-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.assertion-expression {
  flex: 1;
  min-width: 150px;
}

.assertion-extras {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fbff;
}

.extra-label {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.assertion-expected {
  flex: 1;
}

.assertion-hint {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 10px;
  padding-left: 2px;
  line-height: 1.6;
}
</style>
