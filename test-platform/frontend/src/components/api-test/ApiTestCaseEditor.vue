<template>
  <div class="api-case-editor">
    <div class="editor-header">
      <el-input
        v-model="localCase.name"
        placeholder="接口名称"
        size="large"
        class="case-name-input"
        @change="handleNameChange"
      />
      <div class="header-actions">
        <el-switch
          v-model="localCase.stopOnFail"
          active-text="失败终止"
          inactive-text="失败继续"
          size="small"
          @change="handleChange"
        />
        <el-button size="small" @click="openScriptLibrary">
          <el-icon><Code /></el-icon>
          脚本库
        </el-button>
        <el-button size="small" type="primary" :loading="sending" @click="executeCase">
          <el-icon><VideoPlay /></el-icon>
          执行
        </el-button>
        <el-button size="small" type="primary" @click="saveCase">
          <el-icon><Check /></el-icon>
          保存
        </el-button>
      </div>
    </div>

    <div class="editor-main">
      <div class="main-left">
        <div class="request-config">
          <div class="config-section http-config">
            <div class="section-header">
              <span>请求配置</span>
            </div>
            <div class="request-row">
              <el-select v-model="localCase.method" size="small" class="method-select">
                <el-option v-for="m in HTTP_METHODS" :key="m" :label="m" :value="m" />
              </el-select>
              <el-input
                v-model="localCase.url"
                size="small"
                placeholder="请求URL"
                class="url-input"
                @change="handleChange"
              />
            </div>
          </div>

          <el-tabs>
            <el-tab-pane label="参数" name="params">
              <KeyValueEditor
                v-model="localCase.params"
                @change="handleChange"
              />
            </el-tab-pane>
            <el-tab-pane label="请求头" name="headers">
              <KeyValueEditor
                v-model="localCase.headers"
                :key-placeholder="'Header名'"
                :value-placeholder="'值'"
                @change="handleChange"
              />
            </el-tab-pane>
            <el-tab-pane label="Body" name="body">
              <el-radio-group v-model="localCase.bodyType" size="small" @change="handleChange">
                <el-radio-button value="none">none</el-radio-button>
                <el-radio-button value="form-data">form-data</el-radio-button>
                <el-radio-button value="x-www-form-urlencoded">x-www-form-urlencoded</el-radio-button>
                <el-radio-button value="raw">raw</el-radio-button>
              </el-radio-group>
              <el-select v-if="localCase.bodyType === 'raw'" v-model="localCase.bodyRawType" size="small" @change="handleChange">
                <el-option label="JSON" value="json" />
                <el-option label="XML" value="xml" />
                <el-option label="Text" value="text" />
              </el-select>
              <el-input
                v-if="localCase.bodyType === 'raw'"
                v-model="localCase.bodyRaw"
                type="textarea"
                :rows="8"
                placeholder='{"key": "value"}'
                class="body-raw"
                @change="handleChange"
              />
              <KeyValueEditor
                v-else-if="localCase.bodyType !== 'none'"
                v-model="localCase.bodyForm"
                @change="handleChange"
              />
            </el-tab-pane>
            <el-tab-pane label="认证" name="auth">
              <el-select v-model="localCase.authType" @change="handleChange">
                <el-option label="无认证" value="none" />
                <el-option label="Bearer Token" value="bearer" />
                <el-option label="Basic Auth" value="basic" />
                <el-option label="API Key" value="apikey" />
              </el-select>
              <div v-if="localCase.authType === 'bearer'" class="auth-config">
                <el-input v-model="localCase.authConfig.token" placeholder="Token" @change="handleChange" />
              </div>
              <div v-else-if="localCase.authType === 'basic'" class="auth-config">
                <el-input v-model="localCase.authConfig.username" placeholder="用户名" @change="handleChange" />
                <el-input v-model="localCase.authConfig.password" type="password" placeholder="密码" @change="handleChange" />
              </div>
              <div v-else-if="localCase.authType === 'apikey'" class="auth-config">
                <el-input v-model="localCase.authConfig.key" placeholder="Key名" @change="handleChange" />
                <el-input v-model="localCase.authConfig.value" placeholder="Value" @change="handleChange" />
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <div class="main-right">
        <el-tabs>
          <el-tab-pane label="前置步骤" name="prescripts">
            <ApiPrescriptEditor
              v-model="localCase.prescripts"
              @change="handleChange"
            />
          </el-tab-pane>
          <el-tab-pane label="断言" name="assertions">
            <ApiAssertionEditor
              v-model="localCase.assertions"
              @change="handleChange"
            />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, Check } from '@element-plus/icons-vue'
import KeyValueEditor from '../components/api-test/KeyValueEditor.vue'
import ApiPrescriptEditor from '../components/api-test/ApiPrescriptEditor.vue'
import ApiAssertionEditor from '../components/api-test/ApiAssertionEditor.vue'

const HTTP_METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS']

const props = defineProps({
  caseData: Object,
  sending: Boolean
})

const emit = defineEmits(['update:caseData', 'execute', 'save'])

const localCase = reactive({
  name: '',
  stopOnFail: false,
  method: 'GET',
  url: '',
  params: [],
  headers: [],
  bodyType: 'none',
  bodyRaw: '',
  bodyRawType: 'json',
  bodyForm: [],
  authType: 'none',
  authConfig: {},
  prescripts: [],
  assertions: []
})

watch(() => props.caseData, (val) => {
  if (val) {
    Object.assign(localCase, val)
  }
}, { immediate: true, deep: true })

function handleChange() {
  emit('update:caseData', { ...localCase })
}

function handleNameChange() {
  handleChange()
}

function executeCase() {
  emit('execute')
}

function saveCase() {
  emit('save')
}

function openScriptLibrary() {
  emit('openScriptLibrary')
}
</script>

<style scoped>
.api-case-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
}

.case-name-input {
  max-width: 400px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.editor-main {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 400px;
  overflow: hidden;
}

.main-left {
  border-right: 1px solid #e2e8f0;
  overflow: auto;
}

.request-config {
  padding: 16px;
}

.http-config {
  margin-bottom: 16px;
}

.section-header {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 8px;
}

.request-row {
  display: flex;
  gap: 8px;
}

.method-select {
  width: 100px;
}

.url-input {
  flex: 1;
}

.body-raw :deep(textarea) {
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.auth-config {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 12px;
}

.main-right {
  overflow: auto;
  background: #fafafa;
}
</style>
