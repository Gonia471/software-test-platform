<template>
  <el-drawer v-model="visible" title="公共脚本库" size="900px" direction="rtl">
    <div class="script-library">
      <div class="library-header">
        <el-input v-model="searchKeyword" placeholder="搜索函数名" size="small" clearable />
        <el-button type="primary" @click="openEditor(null)">
          <el-icon><Plus /></el-icon>
          新建脚本
        </el-button>
      </div>

      <div class="library-content">
        <el-table :data="filteredScripts" stripe size="small">
          <el-table-column prop="functionName" label="函数名" width="180" />
          <el-table-column prop="scriptName" label="脚本名称" min-width="150" />
          <el-table-column prop="description" label="功能说明" min-width="200" show-overflow-tooltip />
          <el-table-column prop="creator" label="创建者" width="100" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEditor(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="deleteScript(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <ScriptEditor
      v-model="editorVisible"
      :script="currentScript"
      @saved="handleSaved"
    />
  </el-drawer>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ScriptEditor from './ScriptEditor.vue'

defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const scripts = ref([])
const searchKeyword = ref('')
const editorVisible = ref(false)
const currentScript = ref(null)

const filteredScripts = computed(() => {
  if (!searchKeyword.value) return scripts.value
  return scripts.value.filter(s => 
    s.functionName.includes(searchKeyword.value) ||
    s.scriptName?.includes(searchKeyword.value)
  )
})

function openEditor(script) {
  currentScript.value = script
  editorVisible.value = true
}

function handleSaved() {
  editorVisible.value = false
  loadScripts()
  ElMessage.success('保存成功')
}

function deleteScript(script) {
  ElMessageBox.confirm('确定删除该脚本？删除后无法恢复', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    scripts.value = scripts.value.filter(s => s.id !== script.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

function loadScripts() {
  // TODO: 从后端加载脚本列表
}
</script>

<style scoped>
.script-library {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.library-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.library-content {
  flex: 1;
  overflow: auto;
}
</style>
