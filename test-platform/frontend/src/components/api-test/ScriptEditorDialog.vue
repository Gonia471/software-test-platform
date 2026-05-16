<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑脚本' : '新建脚本'"
    width="900px"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
    top="6vh"
    class="script-editor-dialog"
  >
    <div class="script-editor">
      <div class="dialog-intro">
        <div class="dialog-intro__main">
          <span class="dialog-intro__badge">{{ isEdit ? '编辑模式' : '新建模式' }}</span>
          <h3 class="dialog-intro__title">{{ isEdit ? '维护公共脚本' : '创建新的公共脚本' }}</h3>
          <p class="dialog-intro__desc">
            脚本将作为可复用函数供前置步骤和断言使用，建议统一函数名与说明描述，便于后续维护。
          </p>
        </div>
        <div class="dialog-intro__status">
          <div class="status-card">
            <span class="status-card__label">函数识别</span>
            <strong class="status-card__value">{{ detectedFunctionName || '待编译检测' }}</strong>
          </div>
        </div>
      </div>

      <el-form :model="form" label-width="86px" size="small" class="script-form">
        <div class="form-section">
          <div class="form-section__title">基础信息</div>
          <el-form-item label="函数名">
            <el-input v-model="detectedFunctionName" disabled placeholder="自动识别" />
            <div class="form-hint">函数名从代码中自动识别，无需手动填写</div>
          </el-form-item>

          <el-form-item label="脚本名称" required>
            <el-input v-model="form.scriptName" placeholder="如：获取Token" />
          </el-form-item>

          <el-form-item label="功能说明">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="简要描述脚本用途、输入输出或适用场景" />
          </el-form-item>
        </div>

        <div class="form-section form-section--editor">
          <div class="form-section__title">Python 代码</div>
          <el-form-item label="代码内容" required>
            <div class="code-editor-wrapper">
              <div class="code-hint">
                定义一个函数，返回字典类型结果：
                <br />def get_token(username, password):
                <br />&nbsp;&nbsp;&nbsp;&nbsp;# 你的代码
                <br />&nbsp;&nbsp;&nbsp;&nbsp;return {'{'}&quot;token&quot;: &quot;xxx&quot;{'}'}
              </div>
              <div ref="cmEditorRef" class="cm-editor-container"></div>
            </div>
          </el-form-item>
        </div>
      </el-form>

      <div class="compile-section">
        <div class="compile-section__tip">建议先编译测试，通过后再保存到公共脚本库。</div>
        <div class="compile-section__actions">
        <el-button type="primary" :loading="compiling" @click="handleCompile">
          <el-icon><VideoPlay /></el-icon>
          编译测试
        </el-button>
        <el-button class="save-btn" @click="handleSave" :disabled="!canSave">
          <el-icon><Check /></el-icon>
          保存
        </el-button>
        </div>
      </div>

      <div v-if="compileResult" class="compile-result" :class="{ success: compileResult.success, error: !compileResult.success }">
        <div class="result-header">
          <el-icon :color="compileResult.success ? '#22c55e' : '#ef4444'">
            <CircleCheck v-if="compileResult.success" />
            <CircleClose v-else />
          </el-icon>
          <span>{{ compileResult.success ? '编译成功' : '编译失败' }}</span>
        </div>
        <pre v-if="compileResult.errorMessage" class="result-message">{{ compileResult.errorMessage }}</pre>
        <div v-if="compileResult.success && compileResult.functionName" class="function-detected">
          检测到函数：{{ compileResult.functionName }}
        </div>
      </div>

      <div v-if="testResult" class="test-result" :class="{ success: testResult.success, error: !testResult.success }">
        <div class="result-header">
          <el-icon :color="testResult.success ? '#22c55e' : '#ef4444'">
            <CircleCheck v-if="testResult.success" />
            <CircleClose v-else />
          </el-icon>
          <span>{{ testResult.success ? '测试成功' : '测试失败' }}</span>
        </div>
        <pre v-if="testResult.output" class="result-output">{{ testResult.output }}</pre>
        <pre v-if="testResult.errorMessage" class="result-message">{{ testResult.errorMessage }}</pre>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, Check, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { compileScript, testScript, createScript, updateScript } from '../../api/apiTestAdvanced'

const props = defineProps({
  modelValue: Boolean,
  script: Object
})

const emit = defineEmits(['update:modelValue', 'saved'])

const cmEditorRef = ref(null)
let editorInstance = null

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const isEdit = computed(() => !!props.script?.id)

const form = ref({
  scriptName: '',
  description: '',
  content: ''
})

const detectedFunctionName = ref('')
const compiling = ref(false)
const compileResult = ref(null)
const testResult = ref(null)

const canSave = computed(() => form.value.scriptName && form.value.content && compileResult.value?.success)

function initEditor() {
  if (editorInstance) {
    // 已初始化，仅刷新
    nextTick(() => editorInstance.refresh())
    return
  }

  if (cmEditorRef.value) {
    if (window.CodeMirror) {
      editorInstance = window.CodeMirror(cmEditorRef.value, {
        value: form.value.content || '',
        mode: 'python',
        theme: 'material-darker',
        lineNumbers: true,
        indentUnit: 4,
        tabSize: 4,
        matchBrackets: true,
        lineWrapping: true
      })

      editorInstance.on('change', (cm) => {
        form.value.content = cm.getValue()
        onContentChange()
      })

      nextTick(() => editorInstance.refresh())
    } else {
      // 等待 CDN 加载完成后再重试
      setTimeout(initEditor, 400)
    }
  }
}

watch(() => props.script, (val) => {
  if (val) {
    form.value = {
      scriptName: val.scriptName || '',
      description: val.description || '',
      content: val.content || ''
    }
    detectedFunctionName.value = val.functionName || ''
  } else {
    form.value = { scriptName: '', description: '', content: '' }
    detectedFunctionName.value = ''
  }

  if (editorInstance) {
    editorInstance.setValue(form.value.content)
  }

  compileResult.value = null
  testResult.value = null
}, { immediate: true })

watch(visible, (val) => {
  if (val) {
    // 弹窗打开时初始化或刷新编辑器
    nextTick(() => initEditor())
  }
}, { immediate: true })

function onContentChange() {
  compileResult.value = null
  detectedFunctionName.value = ''
}

async function handleCompile() {
  if (!form.value.content) {
    ElMessage.warning('请输入Python代码')
    return
  }

  compiling.value = true
  compileResult.value = null
  testResult.value = null

  try {
    const res = await compileScript(form.value.content)
    compileResult.value = res.data

    if (res.data.success) {
      detectedFunctionName.value = res.data.functionName || ''
      ElMessage.success('编译成功')

      testScript(form.value.content, []).then(testRes => {
        testResult.value = testRes.data
      }).catch(() => {
      })
    } else {
      ElMessage.error('编译失败：' + res.data.errorMessage)
    }
  } catch (e) {
    ElMessage.error('编译失败：' + (e.response?.data?.message || e.message))
  } finally {
    compiling.value = false
  }
}

async function handleSave() {
  if (!canSave.value) {
    ElMessage.warning('请先编译通过')
    return
  }

  try {
    if (isEdit.value) {
      await updateScript(props.script.id, form.value)
    } else {
      await createScript(form.value)
    }
    ElMessage.success('保存成功')
    emit('saved')
    visible.value = false
  } catch (e) {
    ElMessage.error('保存失败：' + (e.response?.data?.message || e.message))
  }
}
</script>

<style scoped>
.script-editor {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dialog-intro {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, rgba(243, 248, 255, 0.98) 100%);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.dialog-intro__main {
  min-width: 0;
}

.dialog-intro__badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 10px;
}

.dialog-intro__title {
  margin: 0 0 8px;
  font-size: 22px;
  line-height: 1.2;
  color: var(--text-primary);
}

.dialog-intro__desc {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.dialog-intro__status {
  min-width: 220px;
}

.status-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 16px;
  background: var(--primary-soft-gradient);
  border: 1px solid rgba(59, 130, 246, 0.12);
}

.status-card__label {
  font-size: 12px;
  color: var(--text-secondary);
}

.status-card__value {
  font-size: 16px;
  color: var(--text-primary);
  word-break: break-word;
}

.script-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-section {
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.form-section__title {
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.form-section--editor {
  padding-bottom: 20px;
}

.script-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.script-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.script-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--text-primary);
}

.form-hint {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 6px;
}

.code-editor-wrapper {
  width: 100%;
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 16px;
  overflow: hidden;
  background: #0f172a;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
}

.cm-editor-container {
  width: 100%;
  height: 350px;
  min-width: 0;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
}

:deep(.CodeMirror) {
  height: 100% !important;
  width: 100% !important;
}

:deep(.CodeMirror-gutters) {
  background-color: #212121;
  border-right: 1px solid #333;
}

.code-hint {
  padding: 12px 14px;
  background: #f8fbff;
  border-bottom: 1px solid rgba(226, 232, 240, 0.95);
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.compile-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.95);
}

.compile-section__tip {
  font-size: 12px;
  color: var(--text-secondary);
}

.compile-section__actions {
  display: flex;
  gap: 10px;
}

.save-btn {
  min-width: 90px;
}

.compile-result,
.test-result {
  padding: 14px 16px;
  border-radius: 14px;
}

.compile-result.success,
.test-result.success {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.compile-result.error,
.test-result.error {
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.result-message {
  margin: 8px 0 0;
  font-family: monospace;
  font-size: 12px;
  color: #dc2626;
  white-space: pre-wrap;
}

.result-output {
  margin: 8px 0 0;
  font-family: monospace;
  font-size: 12px;
  color: #16a34a;
  white-space: pre-wrap;
  max-height: 200px;
  overflow: auto;
}

.function-detected {
  margin-top: 8px;
  font-size: 12px;
  color: #16a34a;
  font-weight: 500;
}

:deep(.script-editor-dialog .el-dialog) {
  border-radius: 22px;
  overflow: hidden;
}

:deep(.script-editor-dialog .el-dialog__header) {
  padding: 22px 24px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(135deg, #fbfdff 0%, #f3f8ff 100%);
}

:deep(.script-editor-dialog .el-dialog__body) {
  padding: 20px 24px 24px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.92) 0%, rgba(255, 255, 255, 0.98) 100%);
}

@media (max-width: 980px) {
  .dialog-intro {
    flex-direction: column;
  }

  .dialog-intro__status {
    min-width: 0;
  }

  .compile-section {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
