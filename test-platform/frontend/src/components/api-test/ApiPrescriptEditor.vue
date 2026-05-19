<template>
  <div class="prescript-editor">
    <div class="prescript-header">
      <div class="header-main">
        <span class="header-title">前置步骤</span>
        <span class="header-subtitle">支持添加 HTTP 请求、函数调用、变量设置，作为接口执行前的准备流程</span>
      </div>
      <div class="header-actions">
        <el-button size="small" class="action-btn action-btn--http" @click="addHttpPrescript">
          <el-icon><Plus /></el-icon>
          HTTP请求
        </el-button>
        <el-button size="small" class="action-btn action-btn--function" @click="addFunctionPrescript">
          <el-icon><Plus /></el-icon>
          函数调用
        </el-button>
        <el-button size="small" class="action-btn action-btn--variable" @click="addSetVariable">
          <el-icon><Plus /></el-icon>
          设置变量
        </el-button>
        <el-button size="small" class="action-btn action-btn--library" @click="openScriptLibrary">
          <el-icon><Code /></el-icon>
          脚本库
        </el-button>
      </div>
    </div>

    <div class="prescript-list">
      <div v-if="prescripts.length === 0" class="prescript-empty">
        暂无前置步骤，点击上方按钮添加
      </div>

      <div
        v-for="(prescript, index) in prescripts"
        :key="index"
        class="prescript-item"
        :class="prescriptClassName(prescript.stepType)"
      >
        <div class="prescript-item-header">
          <el-tag size="small" :type="getStepTypeTag(prescript.stepType)">
            {{ getStepTypeLabel(prescript.stepType) }}
          </el-tag>
          <span class="step-index">#{{ index + 1 }}</span>
          <div class="prescript-actions">
            <el-button size="small" text @click="moveUp(index)" :disabled="index === 0">
              <el-icon><Top /></el-icon>
            </el-button>
            <el-button size="small" text @click="moveDown(index)" :disabled="index === prescripts.length - 1">
              <el-icon><Bottom /></el-icon>
            </el-button>
            <el-switch v-model="prescript.stopOnFail" size="small" />
            <span class="stop-label">失败终止</span>
            <el-button type="danger" size="small" text @click="removePrescript(index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <div v-if="prescript.stepType === 'HTTP'" class="prescript-http">
          <div class="http-row">
            <el-select v-model="prescript.method" size="small" class="method-select">
              <el-option label="GET" value="GET" />
              <el-option label="POST" value="POST" />
              <el-option label="PUT" value="PUT" />
              <el-option label="DELETE" value="DELETE" />
              <el-option label="PATCH" value="PATCH" />
            </el-select>
            <el-input
              v-model="prescript.url"
              size="small"
              placeholder="请求URL，支持 {{变量名}} 语法"
              class="url-input"
            />
          </div>

          <div class="http-section">
            <div class="section-header">
              <span>请求头</span>
            </div>
            <KeyValueEditor
              v-model="prescript.headers"
              :add-label="'添加请求头'"
              :key-placeholder="'请求头名称'"
              :value-placeholder="'值，支持 {{变量名}}'"
              @change="emitChange"
            />
          </div>

          <div class="http-section">
            <div class="section-header">
              <span>请求体</span>
            </div>
            <div class="body-editor">
              <el-radio-group v-model="prescript.bodyType" size="small" @change="emitChange">
                <el-radio-button value="none">无</el-radio-button>
                <el-radio-button value="form-data">表单数据</el-radio-button>
                <el-radio-button value="x-www-form-urlencoded">URL编码</el-radio-button>
                <el-radio-button value="raw">原始数据</el-radio-button>
              </el-radio-group>
              <div v-if="prescript.bodyType === 'raw'" class="raw-body">
                <el-select v-model="prescript.bodyRawType" size="small" class="raw-type" @change="emitChange">
                  <el-option label="JSON" value="json" />
                  <el-option label="XML" value="xml" />
                  <el-option label="纯文本" value="text" />
                </el-select>
                <el-input
                  v-model="prescript.bodyRaw"
                  type="textarea"
                  :rows="6"
                  placeholder='{"key":"value"}'
                  @input="emitChange"
                />
              </div>
              <KeyValueEditor
                v-else-if="prescript.bodyType !== 'none'"
                v-model="prescript.bodyForm"
                :add-label="'添加字段'"
                :key-placeholder="'字段名'"
                :value-placeholder="'字段值，支持 {{变量名}}'"
                @change="emitChange"
              />
            </div>
          </div>

          <div class="extract-section">
            <div class="extract-header">
              <span>参数提取（可提取多个）</span>
              <el-button size="small" text @click="addExtract(index)">
                <el-icon><Plus /></el-icon>
                添加提取
              </el-button>
            </div>
            <div v-if="!prescript.extractParams || prescript.extractParams.length === 0" class="extract-empty">
              无参数提取
            </div>
            <div v-else class="extract-list">
              <div v-for="(param, pIdx) in prescript.extractParams" :key="pIdx" class="extract-item">
                <el-input v-model="param.name" size="small" placeholder="变量名" class="extract-name" />
                <span class="extract-arrow">←</span>
                <el-input v-model="param.path" size="small" placeholder="response.data.id" class="extract-path" />
                <el-button size="small" text type="danger" @click="removeExtract(index, pIdx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>

          <div class="step-assertions">
            <div class="step-assertions-header">
              <span>步骤断言</span>
              <el-button size="small" text @click="addStepAssertion(index)">
                <el-icon><Plus /></el-icon>
                添加断言
              </el-button>
            </div>
            <div v-if="!prescript.assertions || prescript.assertions.length === 0" class="step-assertions-empty">
              无断言
            </div>
            <div v-else class="step-assertions-list">
              <div v-for="(assertion, aIdx) in prescript.assertions" :key="aIdx" class="step-assertion-item">
                <el-select v-model="assertion.type" size="small" class="assertion-type">
                  <el-option label="状态码" value="STATUS" />
                  <el-option label="JSONPath" value="JSONPATH" />
                  <el-option label="包含" value="CONTAINS" />
                </el-select>
                <el-input v-model="assertion.expression" size="small" placeholder="表达式" class="assertion-expr" />
                <el-input v-if="assertion.type !== 'STATUS'" v-model="assertion.expected" size="small" placeholder="期望值" class="assertion-expected" />
                <el-button size="small" text type="danger" @click="removeStepAssertion(index, aIdx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="prescript.stepType === 'FUNCTION'" class="prescript-function">
          <div class="function-row">
            <el-input
              v-model="prescript.functionName"
              size="small"
              placeholder="函数名，如：get_token"
              class="function-name"
            />
            <span class="function-bracket">(</span>
            <el-input
              v-model="prescript.functionParams"
              size="small"
              placeholder="参数，多个用逗号分隔，可使用 {{变量名}}"
              class="function-params"
            />
            <span class="function-bracket">)</span>
          </div>

          <div class="function-output-section">
            <span class="section-label">输出变量（函数返回值会自动赋值）</span>
            <el-input
              v-model="prescript.outputVar"
              size="small"
              placeholder="变量名，如：token"
              class="output-var"
            />
          </div>
        </div>

        <div v-else-if="prescript.stepType === 'SET_VARIABLE'" class="prescript-set-variable">
          <div class="set-var-row">
            <el-input v-model="prescript.varName" size="small" placeholder="变量名" class="var-name" />
            <span class="set-arrow">=</span>
            <el-input v-model="prescript.varValue" size="small" placeholder="变量值，支持 {{变量名}}" class="var-value" />
          </div>
          <div class="set-var-hint">
            示例：varName=base_url, varValue=https://api.example.com
          </div>
        </div>
      </div>
    </div>

    <ScriptLibraryModal v-model="scriptLibraryVisible" @select="onSelectScript" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Plus, Delete, Top, Bottom } from '@element-plus/icons-vue'
import ScriptLibraryModal from './ScriptLibraryModal.vue'
import KeyValueEditor from './KeyValueEditor.vue'

const DEFAULT_HTTP_HEADERS = [
  { key: 'Accept', value: 'application/json', enabled: true },
  { key: 'Content-Type', value: 'application/json', enabled: true },
]

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'openScriptLibrary'])

const prescripts = ref(normalizePrescripts(props.modelValue))
const scriptLibraryVisible = ref(false)
const currentScriptIndex = ref(-1)

watch(prescripts, (val) => {
  const normalized = normalizePrescripts(val)
  if (serializePrescripts(normalized) === serializePrescripts(props.modelValue)) {
    return
  }
  emit('update:modelValue', normalized)
  emit('change', normalized)
}, { deep: true })

watch(
  () => props.modelValue,
  (val) => {
    const normalized = normalizePrescripts(val)
    if (serializePrescripts(normalized) !== serializePrescripts(prescripts.value)) {
      prescripts.value = normalized
    }
  },
  { deep: true },
)

function getStepTypeLabel(type) {
  const labels = { HTTP: 'HTTP请求', FUNCTION: '函数调用', SET_VARIABLE: '设置变量' }
  return labels[type] || type
}

function getStepTypeTag(type) {
  const tags = { HTTP: 'success', FUNCTION: 'warning', SET_VARIABLE: 'info' }
  return tags[type] || ''
}

function prescriptClassName(type) {
  const classMap = {
    HTTP: 'prescript-item--http',
    FUNCTION: 'prescript-item--function',
    SET_VARIABLE: 'prescript-item--variable',
  }
  return classMap[type] || ''
}

function addHttpPrescript() {
  prescripts.value.push({
    stepType: 'HTTP',
    method: 'GET',
    url: '',
    headers: createDefaultHttpHeaders(),
    bodyType: 'none',
    bodyRaw: '',
    bodyRawType: 'json',
    bodyForm: [],
    extractParams: [],
    assertions: [],
    stopOnFail: false
  })
}

function addFunctionPrescript() {
  prescripts.value.push({
    stepType: 'FUNCTION',
    functionName: '',
    functionParams: '',
    outputVar: '',
    stopOnFail: false
  })
}

function addSetVariable() {
  prescripts.value.push({
    stepType: 'SET_VARIABLE',
    varName: '',
    varValue: '',
    stopOnFail: false
  })
}

function removePrescript(index) {
  prescripts.value.splice(index, 1)
}

function moveUp(index) {
  if (index > 0) {
    const temp = prescripts.value[index]
    prescripts.value[index] = prescripts.value[index - 1]
    prescripts.value[index - 1] = temp
  }
}

function moveDown(index) {
  if (index < prescripts.value.length - 1) {
    const temp = prescripts.value[index]
    prescripts.value[index] = prescripts.value[index + 1]
    prescripts.value[index + 1] = temp
  }
}

function addExtract(prescriptIndex) {
  if (!prescripts.value[prescriptIndex].extractParams) {
    prescripts.value[prescriptIndex].extractParams = []
  }
  prescripts.value[prescriptIndex].extractParams.push({ name: '', path: '' })
}

function removeExtract(prescriptIndex, extractIndex) {
  prescripts.value[prescriptIndex].extractParams.splice(extractIndex, 1)
}

function addStepAssertion(prescriptIndex) {
  if (!prescripts.value[prescriptIndex].assertions) {
    prescripts.value[prescriptIndex].assertions = []
  }
  prescripts.value[prescriptIndex].assertions.push({
    type: 'STATUS',
    expression: '200',
    expected: ''
  })
}

function removeStepAssertion(prescriptIndex, assertionIndex) {
  prescripts.value[prescriptIndex].assertions.splice(assertionIndex, 1)
}

function openScriptLibrary() {
  scriptLibraryVisible.value = true
  emit('openScriptLibrary')
}

function onSelectScript(script) {
  // 用户从脚本库选择了脚本
}

function emitChange() {
  emit('update:modelValue', prescripts.value)
  emit('change', prescripts.value)
}

function normalizePrescripts(items) {
  if (!Array.isArray(items)) {
    return []
  }

  return items.map((item) => normalizePrescript(item))
}

function normalizePrescript(item) {
  const source = item && typeof item === 'object' ? item : {}
  const stepType = source.stepType || 'HTTP'

  if (stepType === 'HTTP') {
    return {
      ...source,
      stepType: 'HTTP',
      method: source.method || 'GET',
      url: source.url || '',
      headers: normalizeHttpHeaders(source.headers),
      bodyType: source.bodyType || 'none',
      bodyRaw: source.bodyRaw || '',
      bodyRawType: source.bodyRawType || 'json',
      bodyForm: Array.isArray(source.bodyForm) ? source.bodyForm : [],
      extractParams: Array.isArray(source.extractParams) ? source.extractParams : [],
      assertions: Array.isArray(source.assertions) ? source.assertions : [],
      stopOnFail: Boolean(source.stopOnFail),
    }
  }

  if (stepType === 'FUNCTION') {
    return {
      ...source,
      stepType: 'FUNCTION',
      functionName: source.functionName || '',
      functionParams: source.functionParams || '',
      outputVar: source.outputVar || '',
      stopOnFail: Boolean(source.stopOnFail),
    }
  }

  return {
    ...source,
    stepType: 'SET_VARIABLE',
    varName: source.varName || '',
    varValue: source.varValue || '',
    stopOnFail: Boolean(source.stopOnFail),
  }
}

function createDefaultHttpHeaders() {
  return DEFAULT_HTTP_HEADERS.map((header) => ({ ...header }))
}

function normalizeHttpHeaders(headers) {
  if (!Array.isArray(headers) || headers.length === 0) {
    return createDefaultHttpHeaders()
  }
  return headers.map((header) => ({
    key: header?.key || '',
    value: header?.value || '',
    enabled: header?.enabled !== false,
  }))
}

function serializePrescripts(items) {
  try {
    return JSON.stringify(normalizePrescripts(items))
  } catch {
    return '[]'
  }
}
</script>

<style scoped>
.prescript-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 16px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.88) 0%, rgba(255, 255, 255, 0.98) 100%);
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.prescript-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
}

.header-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.header-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.header-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.action-btn {
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-width: 1px;
}

:deep(.action-btn > span) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  line-height: 1;
}

:deep(.action-btn .el-icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.action-btn--http {
  color: #0f766e;
  background: rgba(20, 184, 166, 0.1);
  border-color: rgba(20, 184, 166, 0.2);
}

.action-btn--http:hover {
  color: #0f766e;
  background: rgba(20, 184, 166, 0.16);
  border-color: rgba(20, 184, 166, 0.26);
}

.action-btn--function {
  color: #7c3aed;
  background: rgba(124, 58, 237, 0.1);
  border-color: rgba(124, 58, 237, 0.18);
}

.action-btn--function:hover {
  color: #7c3aed;
  background: rgba(124, 58, 237, 0.16);
  border-color: rgba(124, 58, 237, 0.24);
}

.action-btn--variable {
  color: #2563eb;
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.18);
}

.action-btn--variable:hover {
  color: #2563eb;
  background: rgba(59, 130, 246, 0.16);
  border-color: rgba(59, 130, 246, 0.24);
}

.action-btn--library {
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.9);
  border-color: rgba(96, 165, 250, 0.5);
}

.action-btn--library:hover {
  color: #1d4ed8;
  background: #dbeafe;
  border-color: rgba(96, 165, 250, 0.68);
}

.prescript-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.prescript-empty {
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 14px;
  border: 1px dashed rgba(148, 163, 184, 0.28);
}

.prescript-item {
  background: rgba(255, 255, 255, 0.96);
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.prescript-item--http {
  border-color: rgba(20, 184, 166, 0.22);
  background: linear-gradient(135deg, rgba(240, 253, 250, 0.96) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.prescript-item--function {
  border-color: rgba(168, 85, 247, 0.2);
  background: linear-gradient(135deg, rgba(250, 245, 255, 0.96) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.prescript-item--variable {
  border-color: rgba(59, 130, 246, 0.2);
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.96) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.prescript-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.step-index {
  font-size: 12px;
  color: var(--text-secondary);
  margin-left: 8px;
}

.prescript-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stop-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-right: 4px;
}

.http-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.method-select {
  width: 100px;
}

.url-input {
  flex: 1;
}

.body-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.raw-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.raw-type {
  width: 120px;
}

.raw-body :deep(textarea) {
  font-family: 'Courier New', monospace;
}

.extract-section,
.step-assertions,
.http-section {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed rgba(226, 232, 240, 0.95);
}

.extract-header,
.step-assertions-header,
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.extract-empty,
.step-assertions-empty {
  font-size: 12px;
  color: #a3a3a3;
  text-align: center;
  padding: 8px;
}

.extract-list,
.step-assertions-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.extract-item,
.step-assertion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fbff;
}

.extract-name {
  width: 100px;
}

.extract-arrow {
  color: var(--text-secondary);
  font-weight: bold;
}

.extract-path {
  flex: 1;
}

.assertion-type {
  width: 100px;
}

.assertion-expr {
  flex: 1;
}

.assertion-expected {
  width: 120px;
}

.function-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.function-name {
  width: 150px;
}

.function-bracket {
  font-family: monospace;
  font-size: 14px;
  color: var(--text-secondary);
}

.function-params {
  flex: 1;
}

.function-output-section {
  margin-top: 8px;
}

.section-label {
  font-size: 12px;
  color: var(--text-secondary);
  display: block;
  margin-bottom: 4px;
}

.output-var {
  width: 200px;
}

.prescript-set-variable {
  padding: 8px 0;
}

.set-var-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.var-name {
  width: 150px;
}

.set-arrow {
  font-weight: bold;
  color: var(--text-secondary);
}

.var-value {
  flex: 1;
}

.set-var-hint {
  font-size: 11px;
  color: #a3a3a3;
  margin-top: 4px;
}

@media (max-width: 1100px) {
  .prescript-header {
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }
}
</style>
