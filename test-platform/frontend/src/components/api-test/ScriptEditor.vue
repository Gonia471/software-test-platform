<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑脚本' : '新建脚本'"
    width="900px"
    :close-on-click-modal="false"
  >
    <div class="script-editor">
      <el-form :model="form" label-width="80px" size="small">
        <el-form-item label="函数名">
          <el-input v-model="detectedFunctionName" disabled placeholder="自动识别" />
          <div class="form-hint">函数名从代码中自动识别，无需填写</div>
        </el-form-item>

        <el-form-item label="脚本名称" required>
          <el-input v-model="form.scriptName" placeholder="如：获取Token" />
        </el-form-item>

        <el-form-item label="功能说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述脚本功能" />
        </el-form-item>

        <el-form-item label="Python代码" required>
          <div class="code-editor-wrapper">
            <div class="code-hint">定义一个函数，返回字典类型结果，如：{'{'}&quot;token&quot;: &quot;xxx&quot;{'}'}</div>
            <div ref="cmEditorRef" class="cm-editor-container"></div>
          </div>
        </el-form-item>
      </el-form>

      <div class="compile-section">
        <el-button type="primary" :loading="compiling" @click="handleCompile">
          <el-icon><VideoPlay /></el-icon>
          编译测试
        </el-button>
        <el-button @click="handleSave" :disabled="!canSave">
          <el-icon><Check /></el-icon>
          保存
        </el-button>
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
          检测到函数: {{ compileResult.functionName }}
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
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, VideoPlay, Check, CircleCheck, CircleClose } from '@element-plus/icons-vue'
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

onMounted(() => {
  if (cmEditorRef.value && window.CodeMirror) {
    editorInstance = window.CodeMirror(cmEditorRef.value, {
      value: form.value.content || '',
      mode: 'python',
      theme: 'material-darker',
      lineNumbers: true,
      indentUnit: 4,
      tabSize: 4,
      matchBrackets: true,
      lineWrapping: true,
      extraKeys: { "Ctrl-Space": "autocomplete" }
    })

    editorInstance.on('change', (cm) => {
      form.value.content = cm.getValue()
      onContentChange()
    })
  }
})

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
    nextTick(() => {
      if (editorInstance) {
        editorInstance.refresh()
      }
    })
  }
})

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
    const res = await compileScript(form.value)
    compileResult.value = res.data

    if (res.data.success) {
      detectedFunctionName.value = res.data.functionName || ''
      ElMessage.success('编译成功')

      testScript(form.value.content, []).then(testRes => {
        testResult.value = testRes.data
      }).catch(() => {
        // 测试可能失败，不影响编译结果
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
  gap: 16px;
}

.form-hint {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.code-editor-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  background: #212121;
}

.cm-editor-container {
  height: 350px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
}

:deep(.CodeMirror) {
  height: 100%;
}

:deep(.CodeMirror-gutters) {
  background-color: #212121;
  border-right: 1px solid #333;
}

.code-hint {
  padding: 8px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 12px;
}

.compile-section {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.compile-result,
.test-result {
  padding: 12px;
  border-radius: 6px;
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
</style>