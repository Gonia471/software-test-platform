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

        <template v-if="activeActionKey === 'openPage'">
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
          ].includes(activeActionKey)"
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
              @change="onLocatorChange"
              @input="onLocatorInput"
            />
          </el-form-item>
          <div
            v-if="showXpathPreview"
            class="xpath-preview"
          >
            <div class="preview-header">
              <span class="preview-title">XPath 优化预览</span>
              <el-tag :type="xpathPreview.isAbsolute === 'true' ? 'warning' : 'success'" size="small">
                {{ xpathPreview.isAbsolute === 'true' ? '绝对路径' : '相对路径' }}
              </el-tag>
            </div>
            <div class="preview-compare">
              <div class="preview-item">
                <div class="preview-label">原始路径</div>
                <div class="preview-value original">{{ xpathPreview.original }}</div>
              </div>
              <div class="preview-arrow">→</div>
              <div class="preview-item">
                <div class="preview-label">优化后</div>
                <div class="preview-value optimized">{{ xpathPreview.optimized }}</div>
              </div>
            </div>
            <div class="preview-info">
              提取元素：{{ xpathPreview.elementName }}
            </div>
            <div v-if="xpathPreview.message" class="preview-info preview-info--hint">
              {{ xpathPreview.message }}
            </div>
          </div>
          <el-form-item
            v-if="localStep.parameters.locatorType === 'xpath'"
            label="优化 XPath"
          >
            <el-switch
              v-model="localStep.parameters.optimizeXpath"
              active-text="启用"
              inactive-text="禁用"
              @change="onOptimizeXpathChange"
            />
            <div class="xpath-tip">
              启用后，系统会自动将绝对路径优化为更简洁的相对定位
            </div>
          </el-form-item>

          <el-form-item
            v-if="activeActionKey === 'inputText'"
            label="输入内容"
          >
            <el-input
              v-model="localStep.parameters.text"
              placeholder="要输入的文本内容"
              @change="emitChange"
            />
          </el-form-item>

          <template v-if="activeActionKey === 'selectOption'">
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
            v-if="['waitVisible', 'waitClickable', 'waitDisappear'].includes(activeActionKey)"
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
            v-if="['assertElementExist', 'assertElementVisible'].includes(activeActionKey)"
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

        <template v-else-if="activeActionKey === 'sleep'">
          <el-form-item label="等待时间（秒）">
            <el-input-number
              v-model="localStep.parameters.seconds"
              :min="1"
              :max="300"
              @change="emitChange"
            />
          </el-form-item>
        </template>

        <template v-else-if="activeActionKey === 'assertTitle'">
          <el-form-item label="预期标题">
            <el-input
              v-model="localStep.parameters.expected"
              placeholder="期望的页面标题"
              @change="emitChange"
            />
          </el-form-item>
        </template>

        <template v-else-if="activeActionKey === 'assertUrl'">
          <el-form-item label="预期 URL">
            <el-input
              v-model="localStep.parameters.expected"
              placeholder="期望的 URL，支持前缀匹配"
              @change="emitChange"
            />
          </el-form-item>
        </template>

        <template v-else-if="activeActionKey === 'assertTextContains'">
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

        <template v-else-if="activeActionKey === 'aiNaturalLanguage'">
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

        <template v-else-if="activeActionKey === 'aiImageClick'">
          <el-form-item label="文字提示">
            <el-input
              v-model="localStep.parameters.instruction"
              type="textarea"
              :rows="2"
              placeholder="例如：点击截图中蓝色的搜索按钮"
              @change="emitChange"
            />
          </el-form-item>

          <el-form-item label="识别模式">
            <el-radio-group
              v-model="localStep.parameters.mode"
              @change="onImageModeChange"
            >
              <el-radio-button label="crop">框选区域点击</el-radio-button>
              <el-radio-button label="template">目标小图点击</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="图片输入">
            <div
              class="image-upload-panel"
              :class="{ 'is-dragover': isImageDragOver, 'is-uploading': uploadPending }"
              tabindex="0"
              @paste="onImagePaste"
              @dragover.prevent="onImageDragOver"
              @dragleave.prevent="onImageDragLeave"
              @drop.prevent="onImageDrop"
            >
              <el-upload
                class="upload"
                action="#"
                accept="image/*"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="onImageChange"
              >
                <el-button type="primary" plain :loading="uploadPending">
                  选择截图文件
                </el-button>
              </el-upload>
              <el-button
                v-if="localStep.parameters.previewUrl"
                size="small"
                text
                type="danger"
                @click="clearImageSelection"
              >
                清空图片
              </el-button>
              <p class="upload-tip">
                支持选择文件、拖拽图片、Ctrl+V 粘贴剪贴板截图
              </p>
              <p class="upload-tip">
                {{
                  localStep.parameters.mode === 'crop'
                    ? '上传整张截图后，在预览图上框出要点击的目标区域'
                    : '上传目标控件的小图，执行时将按模板匹配点击'
                }}
              </p>
              <p v-if="localStep.parameters.assetId" class="upload-tip">
                资源已保存：{{ localStep.parameters.assetName || localStep.parameters.imagePath }}
              </p>
            </div>
          </el-form-item>

          <el-form-item v-if="currentImagePreviewUrl || localStep.parameters.assetId" label="图片预览">
            <div class="image-preview-panel">
              <div class="image-preview-toolbar">
                <div class="image-preview-meta">
                  <span class="image-preview-name">
                    {{ localStep.parameters.assetName || localStep.parameters.imagePath || '未命名图片' }}
                  </span>
                  <el-tag size="small" effect="plain">
                    {{ localStep.parameters.sourceType || 'upload' }}
                  </el-tag>
                </div>
                <span v-if="hasSelectedBox" class="image-selection-summary">
                  已框选目标区域
                </span>
              </div>

              <div
                ref="imageBoardRef"
                class="image-board"
                :class="{ 'is-crop-mode': localStep.parameters.mode === 'crop' }"
                @mousedown="onSelectionStart"
              >
                <img
                  ref="imagePreviewRef"
                  class="image-preview"
                  :src="currentImagePreviewUrl"
                  alt="上传预览"
                  @load="onPreviewImageLoad"
                >
                <div
                  v-if="selectionBoxStyle"
                  class="selection-box"
                  :style="selectionBoxStyle"
                />
              </div>

              <p class="upload-tip">
                {{
                  localStep.parameters.mode === 'crop'
                    ? '鼠标拖拽框选目标区域，系统将保存相对坐标用于后端裁剪'
                    : '当前模式无需框选，后端会直接把整张图片作为模板图'
                }}
              </p>
            </div>
          </el-form-item>

          <el-form-item label="匹配阈值">
            <el-input-number
              v-model="localStep.parameters.threshold"
              :min="0.1"
              :max="1"
              :step="0.01"
              :precision="2"
              @change="emitChange"
            />
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
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { locatorOptions, actionGroups, createStepFromAction } from '../../views/uiTestActions'
import {
  fetchAiVisionAssetBlob,
  previewXpathFast,
  previewXpathWithContext,
  uploadAiVisionAsset,
} from '../../api/uiTest'

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
  pageUrl: {
    type: String,
    default: '',
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
const xpathPreview = ref(null)
const imageBoardRef = ref(null)
const imagePreviewRef = ref(null)
const isImageDragOver = ref(false)
const uploadPending = ref(false)
const boardMetrics = ref({ width: 0, height: 0 })
const objectPreviewUrl = ref('')
let xpathPreviewTimer = null
let xpathPreviewRequestId = 0

const selectionDraft = reactive({
  active: false,
  startX: 0,
  startY: 0,
  currentX: 0,
  currentY: 0,
})

const currentActions = computed(() => {
  const group = actionGroups.find((g) => g.type === selectedGroupType.value)
  return group ? group.actions : []
})

const activeActionKey = computed(() =>
  localStep.action || selectedActionKey.value || props.step?.action || '',
)

const showXpathPreview = computed(() => (
  localStep.parameters?.locatorType === 'xpath'
  && Boolean(String(localStep.parameters?.locatorValue || '').trim())
  && Boolean(xpathPreview.value)
))

const hasSelectedBox = computed(() => {
  const box = localStep.parameters?.box
  return Boolean(
    box
      && box.widthRatio > 0
      && box.heightRatio > 0,
  )
})

const currentImagePreviewUrl = computed(() => objectPreviewUrl.value || '')

const selectionBoxStyle = computed(() => {
  if (activeActionKey.value !== 'aiImageClick' || localStep.parameters?.mode !== 'crop') {
    return null
  }

  const width = boardMetrics.value.width
  const height = boardMetrics.value.height
  if (!width || !height) {
    return null
  }

  if (selectionDraft.active) {
    const left = Math.min(selectionDraft.startX, selectionDraft.currentX)
    const top = Math.min(selectionDraft.startY, selectionDraft.currentY)
    const rectWidth = Math.abs(selectionDraft.currentX - selectionDraft.startX)
    const rectHeight = Math.abs(selectionDraft.currentY - selectionDraft.startY)
    return {
      left: `${left}px`,
      top: `${top}px`,
      width: `${rectWidth}px`,
      height: `${rectHeight}px`,
    }
  }

  const box = localStep.parameters?.box
  if (!box || box.widthRatio <= 0 || box.heightRatio <= 0) {
    return null
  }
  return {
    left: `${box.xRatio * width}px`,
    top: `${box.yRatio * height}px`,
    width: `${box.widthRatio * width}px`,
    height: `${box.heightRatio * height}px`,
  }
})

function defaultImageClickParameters(parameters = {}) {
  const box = parameters.box || {}
  return {
    mode: parameters.mode || 'crop',
    instruction: parameters.instruction || '',
    assetId: parameters.assetId || '',
    assetName: parameters.assetName || '',
    imagePath: parameters.imagePath || '',
    previewUrl: parameters.previewUrl || '',
    sourceType: parameters.sourceType || 'upload',
    threshold: typeof parameters.threshold === 'number' ? parameters.threshold : 0.82,
    box: {
      xRatio: Number(box.xRatio || 0),
      yRatio: Number(box.yRatio || 0),
      widthRatio: Number(box.widthRatio || 0),
      heightRatio: Number(box.heightRatio || 0),
    },
  }
}

function normalizeParameters(action, parameters = {}) {
  if (action === 'aiImageClick') {
    return defaultImageClickParameters(parameters)
  }
  return { ...parameters }
}

function setPreviewIfCurrent(requestId, preview) {
  if (requestId !== xpathPreviewRequestId) {
    return false
  }
  xpathPreview.value = preview
  return true
}

async function fetchFastXpathPreview(xpath, requestId = ++xpathPreviewRequestId) {
  if (!xpath || xpath.length < 5) {
    xpathPreview.value = null
    return null
  }
  try {
    const response = await previewXpathFast(xpath)
    const result = response.data || null
    setPreviewIfCurrent(requestId, result)
    return result
  } catch (e) {
    console.warn('Fast XPath preview fetch failed:', e)
  }
  return null
}

async function fetchContextXpathPreview(xpath, originalXpath, requestId = xpathPreviewRequestId) {
  if (!props.pageUrl) {
    return null
  }
  try {
    const response = await previewXpathWithContext({
      xpath,
      pageUrl: props.pageUrl || '',
    })
    const result = response.data || null
    if (!result || requestId !== xpathPreviewRequestId) {
      return result
    }

    const normalized = {
      ...result,
      original: originalXpath || result.original || xpath,
      optimized: result.optimized || xpath,
    }
    xpathPreview.value = normalized
    return normalized
  } catch (e) {
    console.warn('Context XPath preview fetch failed:', e)
  }
  return null
}

async function applyOptimizedXpath(xpath, forceApply = false) {
  const requestId = ++xpathPreviewRequestId
  const originalXpath = xpath
  const preview = await fetchFastXpathPreview(xpath, requestId)
  if (!preview) {
    emitChange()
    return
  }

  const optimized = preview.optimized || xpath
  if (requestId !== xpathPreviewRequestId) {
    return
  }

  xpathPreview.value = {
    ...preview,
    original: originalXpath,
    optimized,
  }

  if ((forceApply || localStep.parameters.optimizeXpath) && optimized && optimized !== localStep.parameters.locatorValue) {
    localStep.parameters.locatorValue = optimized
  }
  emitChange()

  const refined = await fetchContextXpathPreview(originalXpath, originalXpath, requestId)
  if (!refined || requestId !== xpathPreviewRequestId) {
    return
  }

  if ((forceApply || localStep.parameters.optimizeXpath) && refined.optimized && refined.optimized !== localStep.parameters.locatorValue) {
    localStep.parameters.locatorValue = refined.optimized
    emitChange()
  }
}

function onLocatorInput() {
  if (localStep.parameters.locatorType === 'xpath') {
    if (xpathPreviewTimer) clearTimeout(xpathPreviewTimer)
    xpathPreviewTimer = setTimeout(async () => {
      if (localStep.parameters.optimizeXpath) {
        await applyOptimizedXpath(localStep.parameters.locatorValue)
        return
      }
      const requestId = ++xpathPreviewRequestId
      await fetchFastXpathPreview(localStep.parameters.locatorValue, requestId)
      await fetchContextXpathPreview(localStep.parameters.locatorValue, localStep.parameters.locatorValue, requestId)
    }, 300)
  }
}

async function onLocatorChange() {
  if (localStep.parameters.locatorType === 'xpath') {
    if (localStep.parameters.optimizeXpath) {
      await applyOptimizedXpath(localStep.parameters.locatorValue)
      return
    }
    const requestId = ++xpathPreviewRequestId
    await fetchFastXpathPreview(localStep.parameters.locatorValue, requestId)
    await fetchContextXpathPreview(localStep.parameters.locatorValue, localStep.parameters.locatorValue, requestId)
  } else {
    xpathPreviewRequestId += 1
    xpathPreview.value = null
  }
  emitChange()
}

async function onOptimizeXpathChange(enabled) {
  if (!enabled) {
    emitChange()
    return
  }
  if (localStep.parameters.locatorType !== 'xpath') {
    emitChange()
    return
  }
  await applyOptimizedXpath(localStep.parameters.locatorValue, true)
}

const currentTitle = computed(() => {
  if (!props.step) return '当前未选择步骤'
  return `步骤配置 - ${mapActionLabel(activeActionKey.value)}`
})

const knownAction = computed(() => {
  if (!props.step) return false
  return actionGroups.some((group) =>
    group.actions.some((a) => a.key === activeActionKey.value),
  )
})

watch(
  () => props.step,
  async (val, oldVal) => {
    if (!val) return
    const nextLocatorType = val.parameters?.locatorType || ''
    const nextLocatorValue = String(val.parameters?.locatorValue || '').trim()
    const shouldResetPreview =
      !xpathPreview.value
      || oldVal?.id !== val.id
      || oldVal?.action !== val.action
      || nextLocatorType !== 'xpath'
      || !nextLocatorValue

    if (shouldResetPreview) {
      xpathPreviewRequestId += 1
      xpathPreview.value = null
    }
    const previousAssetId = oldVal?.parameters?.assetId || ''
    const nextAssetId = val.parameters?.assetId || ''
    if (previousAssetId !== nextAssetId) {
      revokeObjectPreviewUrl()
    }

    localStep.id = val.id
    localStep.type = val.type
    localStep.action = val.action
    localStep.description = val.description || ''
    localStep.parameters = normalizeParameters(val.action, val.parameters || {})
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
    await ensureImagePreviewLoaded()
  },
  { immediate: true, deep: true },
)

onBeforeUnmount(() => {
  if (xpathPreviewTimer) {
    clearTimeout(xpathPreviewTimer)
  }
  revokeObjectPreviewUrl()
  detachSelectionListeners()
})

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

async function onImageChange(file) {
  const rawFile = file?.raw || file
  await handleImageFile(rawFile, 'upload')
}

async function handleImageFile(file, sourceType = 'upload') {
  if (!file) return
  if (!String(file.type || '').startsWith('image/')) {
    ElMessage.error('仅支持上传图片文件')
    return
  }

  const localPreview = URL.createObjectURL(file)
  revokeObjectPreviewUrl()
  objectPreviewUrl.value = localPreview
  localStep.parameters = defaultImageClickParameters({
    ...localStep.parameters,
    assetId: '',
    assetName: file.name || 'clipboard-image.png',
    imagePath: file.name || 'clipboard-image.png',
    previewUrl: localPreview,
    sourceType,
    box: {
      xRatio: 0,
      yRatio: 0,
      widthRatio: 0,
      heightRatio: 0,
    },
  })
  emitChange()
  uploadPending.value = true

  try {
    const response = await uploadAiVisionAsset(file)
    const data = response?.data || {}
    localStep.parameters = defaultImageClickParameters({
      ...localStep.parameters,
      assetId: data.assetId || '',
      assetName: data.fileName || localStep.parameters.assetName,
      imagePath: data.fileName || localStep.parameters.imagePath,
      previewUrl: data.previewUrl || localStep.parameters.previewUrl,
      sourceType,
    })
    emitChange()
    ElMessage.success('图片上传成功')
    await nextTick()
    updateImageBoardMetrics()
  } catch (error) {
    ElMessage.error(error?.message || '图片上传失败')
  } finally {
    uploadPending.value = false
  }
}

function clearImageSelection() {
  revokeObjectPreviewUrl()
  localStep.parameters = defaultImageClickParameters()
  emitChange()
}

function revokeObjectPreviewUrl() {
  if (!objectPreviewUrl.value) return
  URL.revokeObjectURL(objectPreviewUrl.value)
  objectPreviewUrl.value = ''
}

async function ensureImagePreviewLoaded() {
  if (localStep.action !== 'aiImageClick') {
    revokeObjectPreviewUrl()
    return
  }

  if (objectPreviewUrl.value || !localStep.parameters?.assetId) {
    return
  }

  try {
    const response = await fetchAiVisionAssetBlob(localStep.parameters.assetId)
    const blobUrl = URL.createObjectURL(response.data)
    objectPreviewUrl.value = blobUrl
    await nextTick()
    updateImageBoardMetrics()
  } catch (error) {
    ElMessage.error(error?.message || '图片预览加载失败')
  }
}

function onImagePaste(event) {
  const items = event.clipboardData?.items || []
  for (const item of items) {
    if (item.type && item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (file) {
        handleImageFile(file, 'paste')
        event.preventDefault()
      }
      return
    }
  }
}

function onImageDragOver() {
  isImageDragOver.value = true
}

function onImageDragLeave() {
  isImageDragOver.value = false
}

function onImageDrop(event) {
  isImageDragOver.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    handleImageFile(file, 'drag')
  }
}

function onImageModeChange() {
  if (localStep.parameters.mode !== 'crop') {
    localStep.parameters.box = {
      xRatio: 0,
      yRatio: 0,
      widthRatio: 0,
      heightRatio: 0,
    }
  }
  emitChange()
}

function onPreviewImageLoad() {
  updateImageBoardMetrics()
}

function updateImageBoardMetrics() {
  const element = imageBoardRef.value
  if (!element) return
  boardMetrics.value = {
    width: element.clientWidth,
    height: element.clientHeight,
  }
}

function onSelectionStart(event) {
  if (activeActionKey.value !== 'aiImageClick' || localStep.parameters?.mode !== 'crop') {
    return
  }
  if (!currentImagePreviewUrl.value || event.button !== 0) {
    return
  }
  updateImageBoardMetrics()
  const rect = imageBoardRef.value?.getBoundingClientRect()
  if (!rect) return
  const point = getRelativePoint(event, rect)
  selectionDraft.active = true
  selectionDraft.startX = point.x
  selectionDraft.startY = point.y
  selectionDraft.currentX = point.x
  selectionDraft.currentY = point.y
  document.addEventListener('mousemove', onSelectionMove)
  document.addEventListener('mouseup', onSelectionEnd)
  event.preventDefault()
}

function onSelectionMove(event) {
  const rect = imageBoardRef.value?.getBoundingClientRect()
  if (!selectionDraft.active || !rect) return
  const point = getRelativePoint(event, rect)
  selectionDraft.currentX = point.x
  selectionDraft.currentY = point.y
}

function onSelectionEnd() {
  if (!selectionDraft.active) {
    detachSelectionListeners()
    return
  }

  const width = boardMetrics.value.width
  const height = boardMetrics.value.height
  const left = Math.min(selectionDraft.startX, selectionDraft.currentX)
  const top = Math.min(selectionDraft.startY, selectionDraft.currentY)
  const rectWidth = Math.abs(selectionDraft.currentX - selectionDraft.startX)
  const rectHeight = Math.abs(selectionDraft.currentY - selectionDraft.startY)

  selectionDraft.active = false
  detachSelectionListeners()

  if (!width || !height || rectWidth < 4 || rectHeight < 4) {
    return
  }

  localStep.parameters.box = {
    xRatio: left / width,
    yRatio: top / height,
    widthRatio: rectWidth / width,
    heightRatio: rectHeight / height,
  }
  emitChange()
}

function detachSelectionListeners() {
  document.removeEventListener('mousemove', onSelectionMove)
  document.removeEventListener('mouseup', onSelectionEnd)
}

function getRelativePoint(event, rect) {
  const x = Math.min(Math.max(event.clientX - rect.left, 0), rect.width)
  const y = Math.min(Math.max(event.clientY - rect.top, 0), rect.height)
  return { x, y }
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
  xpathPreviewRequestId += 1
  xpathPreview.value = null
}

function onActionChange(actionKey) {
  const group = actionGroups.find((g) => g.type === selectedGroupType.value)
  if (!group) return
  const act = group.actions.find((a) => a.key === actionKey)
  if (!act) return
  const previousActionLabel = mapActionLabel(activeActionKey.value)
  const previousDescription = String(localStep.description || '').trim()
  const base = createStepFromAction(
    { key: act.key, label: act.label },
    group.type,
  )
  localStep.type = group.type
  localStep.action = act.key
  if (!previousDescription || previousDescription === previousActionLabel) {
    localStep.description = act.label
  }
  localStep.parameters = normalizeParameters(act.key, base.parameters || {})
  xpathPreviewRequestId += 1
  xpathPreview.value = null
  emitChange()
}
</script>

<style scoped>
.panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: var(--border-radius);
  overflow: hidden;
}

.panel-header {
  display: flex;
  flex-direction: column;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  background: linear-gradient(135deg, #fbfdff 0%, #f3f8ff 100%);
  min-height: 86px;
  box-sizing: border-box;
}

.panel-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 8px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 999px;
  display: inline-block;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.92) 0%, rgba(255, 255, 255, 0.98) 100%);
}

:deep(.el-form-item) {
  margin-bottom: 0;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.88);
}

:deep(.el-form-item:last-child) {
  border-bottom: none;
  padding-bottom: 0;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 32px;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-input-number) {
  width: 100%;
}

.action-type-row {
  display: flex;
  gap: 12px;
}

.action-type-row .el-select {
  flex: 1;
}

.upload {
  margin-bottom: 8px;
}

.image-upload-panel {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px dashed rgba(148, 163, 184, 0.5);
  border-radius: 12px;
  background: rgba(248, 251, 255, 0.9);
  outline: none;
}

.image-upload-panel.is-dragover {
  border-color: var(--primary-color);
  background: rgba(239, 246, 255, 0.95);
}

.image-upload-panel.is-uploading {
  opacity: 0.75;
}

.image-preview-panel {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.image-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.image-preview-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.image-preview-name {
  font-size: 12px;
  color: var(--text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-selection-summary {
  font-size: 11px;
  color: var(--primary-color);
}

.image-board {
  position: relative;
  width: 100%;
  min-height: 180px;
  border-radius: 12px;
  overflow: hidden;
  background: #0f172a;
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.image-board.is-crop-mode {
  cursor: crosshair;
}

.image-preview {
  display: block;
  width: 100%;
  max-height: 320px;
  object-fit: contain;
  user-select: none;
  -webkit-user-drag: none;
}

.selection-box {
  position: absolute;
  border: 2px solid #3b82f6;
  background: rgba(59, 130, 246, 0.15);
  box-shadow: 0 0 0 9999px rgba(15, 23, 42, 0.15);
  pointer-events: none;
}

.upload-tip {
  font-size: 11px;
  color: var(--text-secondary);
  margin: 6px 0 0;
  line-height: 1.5;
}

.xpath-tip {
  font-size: 11px;
  color: var(--text-secondary);
  margin: 4px 0 0;
  line-height: 1.4;
}

.xpath-preview {
  background: linear-gradient(135deg, #ffffff 0%, #f7fbff 100%);
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 16px;
  padding: 14px 16px;
  margin: 0 0 12px 0;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.preview-title {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
}

.preview-compare {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.preview-item {
  flex: 1;
  min-width: 0;
}

.preview-label {
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.preview-value {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 11px;
  padding: 8px 10px;
  border-radius: 6px;
  word-break: break-all;
  line-height: 1.4;
  max-height: 60px;
  overflow-y: auto;
}

.preview-value.original {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
}

.preview-value.optimized {
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  color: #0f766e;
}

.preview-arrow {
  font-size: 18px;
  color: #999;
  flex-shrink: 0;
}

.preview-info {
  font-size: 11px;
  color: var(--text-secondary);
  padding-top: 8px;
  border-top: 1px dashed rgba(226, 232, 240, 0.95);
}

.preview-info--hint {
  color: #d97706;
  border-top: none;
  padding-top: 4px;
}

.footer {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid rgba(226, 232, 240, 0.88);
}

.footer .el-button {
  width: 100%;
  border-radius: 8px;
}

.footer-tip {
  font-size: 11px;
  color: var(--text-secondary);
  text-align: center;
  line-height: 1.5;
}

.empty-tip {
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
  padding: 60px 20px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.92) 0%, rgba(255, 255, 255, 0.98) 100%);
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.empty-tip::before {
  content: '📋';
  font-size: 48px;
  opacity: 0.5;
}

:deep(.el-radio-button__inner) {
  border-radius: 6px;
}

:deep(.el-alert) {
  border-radius: 8px;
}
</style>
