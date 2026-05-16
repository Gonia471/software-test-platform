<template>
  <el-drawer v-model="visible" title="公共脚本库" size="800px" direction="rtl" append-to-body class="script-library-drawer">
    <div class="script-library">
      <div class="library-header">
        <div class="library-header__main">
          <span class="library-title">脚本列表</span>
          <span class="library-subtitle">维护可复用的公共函数脚本，供前置步骤与断言逻辑调用</span>
        </div>
        <el-input v-model="searchKeyword" placeholder="搜索函数名或脚本名称" size="default" clearable prefix-icon="Search" class="library-search" />
        <el-button type="primary" class="create-script-btn" @click="openEditor(null)">
          <el-icon><Plus /></el-icon>
          新建脚本
        </el-button>
      </div>

      <div v-loading="loading" class="library-content">
        <el-empty v-if="!filteredScripts.length" description="暂无脚本，点击上方按钮新建" />

        <div v-else class="script-list">
          <div
            v-for="script in filteredScripts"
            :key="script.id"
            class="script-item"
          >
            <div class="script-main">
              <div class="script-info">
                <div class="script-name-row">
                  <span class="function-tag">def</span>
                  <span class="function-name">{{ script.functionName }}</span>
                  <span class="function-bracket">()</span>
                </div>
                <div class="script-alias">{{ script.scriptName || '未命名' }}</div>
                <div class="script-desc" v-if="script.description">
                  {{ script.description }}
                </div>
              </div>
              <div class="script-meta">
                <span class="creator">
                  <el-icon><User /></el-icon>
                  {{ script.creator?.username || '未知' }}
                </span>
              </div>
            </div>

            <div class="script-actions">
              <el-tooltip content="测试运行此函数" placement="top">
                <el-button size="small" type="info" plain circle @click="testScript(script)">
                  <el-icon><VideoPlay /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="编辑脚本内容" placement="top">
                <el-button size="small" type="primary" plain circle @click="openEditor(script)">
                  <el-icon><Edit /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="删除此脚本" placement="top">
                <el-button size="small" type="danger" plain circle @click="deleteScript(script)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ScriptEditorDialog
      v-model="editorVisible"
      :script="currentScript"
      @saved="handleSaved"
    />

    <el-dialog v-model="testVisible" title="测试运行" width="600px" append-to-body>
      <div v-if="testResult" class="test-result" :class="{ success: testResult.success, error: !testResult.success }">
        <div class="result-header">
          <el-icon :color="testResult.success ? '#22c55e' : '#ef4444'" size="20">
            <CircleCheck v-if="testResult.success" />
            <CircleClose v-else />
          </el-icon>
          <span>{{ testResult.success ? '运行成功' : '运行失败' }}</span>
        </div>
        <pre v-if="testResult.output" class="result-output">{{ testResult.output }}</pre>
        <pre v-if="testResult.errorMessage" class="result-error">{{ testResult.errorMessage }}</pre>
      </div>
      <div v-else class="test-running">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在执行...</span>
      </div>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, User, VideoPlay, Edit, Delete, CircleCheck, CircleClose, Loading } from '@element-plus/icons-vue'
import ScriptEditorDialog from './ScriptEditorDialog.vue'
import { listScripts, deleteScript as apiDeleteScript, testScript as apiTestScript } from '../../api/apiTestAdvanced'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'select'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const scripts = ref([])
const searchKeyword = ref('')
const editorVisible = ref(false)
const currentScript = ref(null)
const testVisible = ref(false)
const testResult = ref(null)

const filteredScripts = computed(() => {
  if (!searchKeyword.value) return scripts.value
  const keyword = searchKeyword.value.toLowerCase()
  return scripts.value.filter(s =>
    s.functionName?.toLowerCase().includes(keyword) ||
    s.scriptName?.toLowerCase().includes(keyword)
  )
})

watch(visible, (val) => {
  if (val) {
    loadScripts()
  }
})

async function loadScripts() {
  loading.value = true
  try {
    const res = await listScripts()
    const data = res.data
    if (Array.isArray(data)) {
      scripts.value = data
    } else {
      scripts.value = []
    }
  } catch (e) {
    ElMessage.error('加载脚本库失败')
    scripts.value = []
  } finally {
    loading.value = false
  }
}

function openEditor(script) {
  currentScript.value = script
  editorVisible.value = true
}

async function testScript(script) {
  testVisible.value = true
  testResult.value = null
  try {
    const res = await apiTestScript(script.content, [])
    testResult.value = res.data
  } catch (e) {
    testResult.value = { success: false, errorMessage: '调用失败：' + (e.message || '未知错误') }
  }
}

function handleSaved() {
  editorVisible.value = false
  nextTick(() => {
    loadScripts()
  })
  ElMessage.success('保存成功')
}

async function deleteScript(script) {
  try {
    await ElMessageBox.confirm(
      `确定删除脚本 "${script.functionName}"？删除后无法恢复。`,
      '删除确认',
      { type: 'warning' }
    )
    await apiDeleteScript(script.id)
    ElMessage.success('删除成功')
    loadScripts()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped>
.script-library {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.92) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.library-header {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 4px 2px 0;
  flex-wrap: wrap;
}

.library-header__main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 180px;
}

.library-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.library-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.library-search {
  flex: 1;
  min-width: 220px;
}

.create-script-btn {
  min-height: 40px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  white-space: nowrap;
}

:deep(.create-script-btn .el-icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

:deep(.create-script-btn > span) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  line-height: 1;
}

.library-content {
  flex: 1;
  overflow-y: auto;
}

.script-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.script-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 18px;
  transition: var(--transition);
}

.script-item:hover {
  background: #fff;
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.script-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.script-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 0;
  flex-wrap: wrap;
}

.function-tag {
  font-size: 11px;
  font-weight: 600;
  color: #2563eb;
  background: rgba(59, 130, 246, 0.1);
  padding: 4px 8px;
  border-radius: 999px;
  font-family: 'JetBrains Mono', monospace;
}

.function-name {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
}

.function-bracket {
  font-size: 15px;
  color: #94a3b8;
  font-family: 'JetBrains Mono', 'Consolas', monospace;
}

.script-alias {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.script-desc {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.script-meta {
  margin-top: 8px;
}

.script-meta .creator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #94a3b8;
}

.script-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  align-items: center;
}

.test-result {
  padding: 16px;
  border-radius: 8px;
  background: #f8fafc;
}

.test-result.success {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.test-result.error {
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  margin-bottom: 12px;
}

.result-output,
.result-error {
  margin: 0;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.result-error {
  color: #dc2626;
}

.test-running {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
  color: #64748b;
}

:deep(.script-library-drawer .el-drawer__header) {
  margin-bottom: 0;
  padding: 22px 24px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(135deg, #fbfdff 0%, #f3f8ff 100%);
}

:deep(.script-library-drawer .el-drawer__body) {
  padding: 20px 24px 24px;
}

:deep(.script-actions .el-button .el-icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>
