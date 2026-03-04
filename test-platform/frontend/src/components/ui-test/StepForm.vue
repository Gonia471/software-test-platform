<template>
  <el-card class="panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <span class="panel-title">参数配置</span>
        <span class="panel-subtitle">
          {{ currentTitle }}
        </span>
      </div>
    </template>

    <div v-if="step" class="form-body">
      <el-form
        label-width="96px"
        label-position="left"
        size="small"
      >
        <el-form-item label="动作类型">
          <div class="action-type-row">
            <el-select
              v-model="selectedGroupType"
              placeholder="选择操作类别"
              style="width: 130px"
              @change="onGroupChange"
            >
              <el-option
                v-for="group in actionGroups"
                :key="group.type"
                :label="group.title"
                :value="group.type"
              />
            </el-select>
            <el-select
              v-model="selectedActionKey"
              placeholder="选择具体动作"
              style="width: 160px"
              @change="onActionChange"
            >
              <el-option
                v-for="act in currentActions"
                :key="act.key"
                :label="act.label"
                :value="act.key"
              />
            </el-select>
          </div>
        </el-form-item>

        <el-form-item label="步骤序号">
          <el-input-number
            v-model="displayOrder"
            :min="1"
            :max="Math.max(total, 1)"
            size="small"
            @change="onOrderChange"
          />
        </el-form-item>

        <el-form-item label="步骤描述">
          <el-input
            v-model="localStep.description"
            placeholder="用于说明此步骤的业务含义"
            @change="emitChange"
          />
        </el-form-item>

        <template v-if="step.action === 'openPage'">
          <el-form-item label="URL">
            <el-input
              v-model="localStep.parameters.url"
              placeholder="例如：https://example.com/login"
              @change="emitChange"
            />
          </el-form-item>
        </template>

        <template
          v-else-if="[
            'clickElement',
            'inputText',
            'clearText',
            'getText',
            'selectOption',
            'toggleCheck',
            'waitVisible',
            'waitClickable',
            'waitDisappear',
            'assertElementExist',
            'assertElementVisible',
          ].includes(step.action)"
        >
          <el-form-item label="定位方式">
            <el-select
              v-model="localStep.parameters.locatorType"
              placeholder="选择定位方式"
              @change="emitChange"
            >
              <el-option
                v-for="item in locatorOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="定位表达式">
            <el-input
              v-model="localStep.parameters.locatorValue"
              placeholder="如：#login-button 或 //button[text()='登录']"
              @change="emitChange"
            />
          </el-form-item>

          <el-form-item
            v-if="step.action === 'inputText'"
            label="输入内容"
          >
            <el-input
              v-model="localStep.parameters.text"
              placeholder="要输入的文本内容"
              @change="emitChange"
            />
          </el-form-item>

          <template v-if="step.action === 'selectOption'">
            <el-form-item label="选项类型">
              <el-select
                v-model="localStep.parameters.optionType"
                placeholder="按值或按文本选择"
                @change="emitChange"
              >
                <el-option label="按值" value="value" />
                <el-option label="按可见文本" value="text" />
              </el-select>
            </el-form-item>
            <el-form-item label="选项值/文本">
              <el-input
                v-model="localStep.parameters.optionValue"
                placeholder="例如：admin 或 管理员"
                @change="emitChange"
              />
            </el-form-item>
          </template>

          <el-form-item
            v-if="['waitVisible', 'waitClickable', 'waitDisappear'].includes(step.action)"
            label="超时时间（秒）"
          >
            <el-input-number
              v-model="localStep.parameters.timeout"
              :min="1"
              :max="120"
              @change="emitChange"
            />
          </el-form-item>

          <el-form-item
            v-if="['assertElementExist', 'assertElementVisible'].includes(step.action)"
            label="预期结果"
          >
            <el-radio-group
              v-model="localStep.parameters.expected"
              @change="emitChange"
            >
              <el-radio-button :label="true">是</el-radio-button>
              <el-radio-button :label="false">否</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </template>

        <template v-else-if="step.action === 'sleep'">
          <el-form-item label="等待时间（秒）">
            <el-input-number
              v-model="localStep.parameters.seconds"
              :min="1"
              :max="300"
              @change="emitChange"
            />
          </el-form-item>
        </template>

        <template v-else-if="step.action === 'assertTitle'">
          <el-form-item label="预期标题">
            <el-input
              v-model="localStep.parameters.expected"
              placeholder="期望的页面标题"
              @change="emitChange"
            />
          </el-form-item>
        </template>

        <template v-else-if="step.action === 'assertUrl'">
          <el-form-item label="预期 URL">
            <el-input
              v-model="localStep.parameters.expected"
              placeholder="期望的 URL，支持前缀匹配"
              @change="emitChange"
            />
          </el-form-item>
        </template>

        <template v-else-if="step.action === 'assertTextContains'">
          <el-form-item label="预期文本">
            <el-input
              v-model="localStep.parameters.expectedText"
              type="textarea"
              :rows="3"
              placeholder="期望页面中包含的文本片段"
              @change="emitChange"
            />
          </el-form-item>
        </template>

        <template v-else-if="step.action === 'aiNaturalLanguage'">
          <el-form-item label="自然语言指令">
            <el-input
              v-model="localStep.parameters.instruction"
              type="textarea"
              :rows="4"
              placeholder="例如：点击“登录”按钮，然后等待首页标题包含“仪表盘”"
              @change="emitChange"
            />
          </el-form-item>
        </template>

        <template v-else-if="step.action === 'aiImageClick'">
          <el-form-item label="上传截图">
            <el-upload
              class="upload"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="onImageChange"
            >
              <el-button type="primary" plain>选择截图文件</el-button>
            </el-upload>
            <p v-if="localStep.parameters.imagePath" class="upload-tip">
              已选择：{{ localStep.parameters.imagePath }}
            </p>
            <p class="upload-tip">
              仅前端演示：实际识别与点击逻辑将在后端/执行引擎中实现
            </p>
          </el-form-item>
        </template>

        <el-form-item v-if="!knownAction">
          <el-alert
            title="当前动作暂未配置专门的参数表单，请在后续实现中补充。"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form-item>
      </el-form>

      <div class="footer">
        <el-button type="primary" size="small" @click="emitChange">
          保存参数
        </el-button>
        <span class="footer-tip">参数会直接保存到当前用例的步骤数据中（前端 Mock）。</span>
      </div>
    </div>

    <div v-else class="empty-tip">
      请在中间区域选择一个步骤后，再进行参数配置
    </div>
  </el-card>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { locatorOptions, actionGroups, createStepFromAction } from '../../views/uiTestActions'

const props = defineProps({
  step: {
    type: Object,
    default: null,
  },
  index: {
    type: Number,
    default: -1,
  },
  total: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits(['update-step', 'change-order'])

const localStep = reactive({
  id: '',
  type: '',
  action: '',
  description: '',
  parameters: {},
})

const displayOrder = ref(1)
const selectedGroupType = ref('')
const selectedActionKey = ref('')

const currentActions = computed(() => {
  const group = actionGroups.find((g) => g.type === selectedGroupType.value)
  return group ? group.actions : []
})

const currentTitle = computed(() => {
  if (!props.step) return '当前未选择步骤'
  return `步骤配置 - ${mapActionLabel(props.step.action)}`
})

const knownAction = computed(() => {
  if (!props.step) return false
  return actionGroups.some((group) =>
    group.actions.some((a) => a.key === props.step.action),
  )
})

watch(
  () => props.step,
  (val) => {
    if (!val) return
    localStep.id = val.id
    localStep.type = val.type
    localStep.action = val.action
    localStep.description = val.description || ''
    localStep.parameters = {
      ...(val.parameters || {}),
    }
    let groupType = val.type
    if (!groupType) {
      const foundGroup = actionGroups.find((g) =>
        g.actions.some((a) => a.key === val.action),
      )
      groupType = foundGroup?.type || ''
    }
    selectedGroupType.value = groupType || actionGroups[0]?.type || ''
    selectedActionKey.value = val.action || ''
    if (props.index != null && props.index >= 0) {
      displayOrder.value = props.index + 1
    }
  },
  { immediate: true, deep: true },
)

watch(
  () => props.index,
  (val) => {
    if (val != null && val >= 0) {
      displayOrder.value = val + 1
    }
  },
)

function mapActionLabel(action) {
  switch (action) {
    case 'openPage':
      return '打开网页'
    case 'refreshPage':
      return '刷新页面'
    case 'goBack':
      return '后退'
    case 'goForward':
      return '前进'
    case 'closeWindow':
      return '关闭窗口'
    case 'clickElement':
      return '点击元素'
    case 'inputText':
      return '输入文本'
    case 'clearText':
      return '清空文本'
    case 'getText':
      return '获取文本/属性'
    case 'selectOption':
      return '下拉框选择'
    case 'toggleCheck':
      return '勾选框/单选框'
    case 'sleep':
      return '强制等待'
    case 'waitVisible':
      return '等待元素可见'
    case 'waitClickable':
      return '等待元素可点击'
    case 'waitDisappear':
      return '等待元素消失'
    case 'assertTitle':
      return '断言标题'
    case 'assertUrl':
      return '断言 URL'
    case 'assertTextContains':
      return '断言文本包含'
    case 'assertElementExist':
      return '断言元素存在/不存在'
    case 'assertElementVisible':
      return '断言元素可见/不可见'
    case 'aiNaturalLanguage':
      return '自然语言指令'
    case 'aiImageClick':
      return '图像识别点击'
    default:
      return action || '未知动作'
  }
}

function emitChange() {
  if (!props.step) return
  emit('update-step', {
    id: localStep.id,
    type: localStep.type,
    action: localStep.action,
    description: localStep.description,
    parameters: { ...localStep.parameters },
  })
}

function onImageChange(file) {
  localStep.parameters.imagePath = file.name
  emitChange()
}

function onOrderChange(val) {
  if (!props.step) return
  const target = Number(val) - 1
  if (Number.isNaN(target)) return
  emit('change-order', target)
}

function onGroupChange(type) {
  selectedGroupType.value = type
  selectedActionKey.value = ''
}

function onActionChange(actionKey) {
  const group = actionGroups.find((g) => g.type === selectedGroupType.value)
  if (!group) return
  const act = group.actions.find((a) => a.key === actionKey)
  if (!act) return
  const base = createStepFromAction(
    { key: act.key, label: act.label },
    group.type,
  )
  localStep.type = group.type
  localStep.action = act.key
  if (!localStep.description) {
    localStep.description = act.label
  }
  localStep.parameters = { ...(base.parameters || {}) }
  emitChange()
}
</script>

<style scoped>
.panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  flex-direction: column;
}

.panel-title {
  font-weight: 600;
}

.panel-subtitle {
  font-size: 12px;
  color: #9ca3af;
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  overflow-y: auto;
}

.action-type-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload {
  margin-bottom: 4px;
}

.upload-tip {
  font-size: 12px;
  color: #9ca3af;
  margin: 2px 0 0;
}

.footer {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-tip {
  font-size: 12px;
  color: #9ca3af;
}

.empty-tip {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  padding: 24px 0;
}
</style>

