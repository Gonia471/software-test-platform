<template>
  <el-card class="panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <div class="panel-header-left">
          <span class="panel-title">测试步骤</span>
          <span class="panel-subtitle">拖拽调整顺序，点击选择步骤</span>
        </div>
        <div class="panel-actions">
          <el-button type="primary" size="small" plain @click="$emit('add-empty-step')">
            新增占位步骤
          </el-button>
        </div>
      </div>
    </template>

    <div
      ref="stepContainerRef"
      class="step-container"
      :class="{ 'is-empty': !steps || !steps.length }"
    >
      <draggable
        v-model="innerSteps"
        item-key="id"
        :group="dragGroup"
        handle=".step-card__drag"
        class="step-list"
        :class="{ 'is-empty-list': !innerSteps.length }"
      >
        <template #item="{ element, index }">
          <div
            class="step-card"
            :data-step-id="element.id"
            :class="{ 'is-active': element.id === selectedStepId }"
            @click.stop="$emit('select-step', element.id)"
          >
            <div class="step-card__left">
              <span class="step-index">#{{ index + 1 }}</span>
              <button
                type="button"
                class="step-card__drag"
                title="拖拽排序"
              >
                ⋮⋮
              </button>
            </div>
            <div class="step-card__body">
              <div class="step-title">
                <span class="step-type-tag">{{ mapTypeLabel(element.type) }}</span>
                <span class="step-action">{{ mapActionLabel(element.action) }}</span>
              </div>
              <div class="step-description">
                {{ element.description || '暂无描述' }}
              </div>
              <div class="step-params">
                {{ formatParamsPreview(element) }}
              </div>
            </div>
            <div class="step-card__ops" @click.stop>
              <div class="step-card__ops-inner">
                <el-button
                  class="step-op-btn"
                  link
                  size="small"
                  @click="$emit('duplicate-step', element.id)"
                >
                  复制
                </el-button>
                <el-button
                  class="step-op-btn"
                  link
                  type="danger"
                  size="small"
                  @click="$emit('delete-step', element.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </template>
        <template #footer>
          <div v-if="!innerSteps.length" class="empty-tip">
            将左侧动作拖拽到此处以创建测试步骤
          </div>
        </template>
      </draggable>
    </div>
  </el-card>
</template>

<script setup>
import { computed, ref } from 'vue'
import draggable from 'vuedraggable'

const props = defineProps({
  steps: {
    type: Array,
    required: true,
  },
  selectedStepId: {
    type: String,
    default: '',
  },
  dragGroup: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits([
  'update:steps',
  'select-step',
  'duplicate-step',
  'delete-step',
  'add-empty-step',
])

const innerSteps = computed({
  get() {
    return props.steps
  },
  set(val) {
    emit('update:steps', val)
  },
})

const stepContainerRef = ref(null)

function mapTypeLabel(type) {
  switch (type) {
    case 'browser':
      return '浏览器'
    case 'element':
      return '元素'
    case 'wait':
      return '等待'
    case 'assert':
      return '断言'
    case 'ai':
      return 'AI'
    default:
      return '步骤'
  }
}

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
      return action || '步骤'
  }
}

function formatParamsPreview(step) {
  if (!step || !step.parameters) return '未配置参数'
  const p = step.parameters

  switch (step.action) {
    case 'openPage':
      return `URL: ${p.url || '-'}`
    case 'inputText':
      return `定位: ${p.locatorType || '-'}=${p.locatorValue || '-'}，文本: ${p.text || '-'}`
    case 'clickElement':
    case 'clearText':
    case 'getText':
    case 'toggleCheck':
    case 'waitVisible':
    case 'waitClickable':
    case 'waitDisappear':
    case 'assertElementExist':
    case 'assertElementVisible':
      return `定位: ${p.locatorType || '-'}=${p.locatorValue || '-'}`
    case 'selectOption':
      return `下拉: ${p.locatorType || '-'}=${p.locatorValue || '-'}，按${p.optionType === 'text' ? '文本' : '值'}=${p.optionValue || '-'}`
    case 'sleep':
      return `等待: ${p.seconds ?? '-'} 秒`
    case 'assertTitle':
    case 'assertUrl':
      return `预期: ${p.expected || '-'}`
    case 'assertTextContains':
      return `预期文本包含: ${p.expectedText || '-'}`
    case 'aiNaturalLanguage':
      return `指令: ${p.instruction || '-'}`
    case 'aiImageClick':
      return p.imagePath ? `截图: ${p.imagePath}` : '请上传截图'
    default:
      return '未配置参数'
  }
}

function scrollToStep(id) {
  const container = stepContainerRef.value
  if (!container) return
  const el = container.querySelector(`[data-step-id="${id}"]`)
  if (el && typeof el.scrollIntoView === 'function') {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

defineExpose({
  scrollToStep,
})
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
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  background: linear-gradient(135deg, #fbfdff 0%, #f3f8ff 100%);
  min-height: 86px;
  box-sizing: border-box;
}

.panel-header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
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
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.panel-actions {
  flex-shrink: 0;
}

.step-container {
  flex: 1;
  overflow: auto;
  padding: 14px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.9) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.step-container.is-empty {
  background: #f8fbff;
  border-radius: 16px;
  min-height: 120px;
  border: 2px dashed rgba(148, 163, 184, 0.3);
}

.step-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 100%;
}

.step-list.is-empty-list {
  min-height: 120px;
  justify-content: center;
}

.step-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  padding: 14px 14px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background-color: rgba(255, 255, 255, 0.96);
  cursor: pointer;
  transition: var(--transition);
}

.step-card:hover {
  border-color: var(--primary-color-light);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
}

.step-card.is-active {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
  background: linear-gradient(135deg, #f8fbff 0%, #eef6ff 100%);
}

.step-card__left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 36px;
}

.step-index {
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-color);
  background: rgba(59, 130, 246, 0.1);
  padding: 4px 10px;
  border-radius: 999px;
}

.step-card__drag {
  border: none;
  background: transparent;
  cursor: grab;
  font-size: 16px;
  color: #cbd5e1;
  padding: 0;
  transition: var(--transition);
}

.step-card__drag:hover {
  color: var(--primary-color);
}

.step-card__body {
  min-width: 0;
}

.step-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.step-type-tag {
  font-size: 10px;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(20, 184, 166, 0.1);
  color: #0f766e;
  font-weight: 700;
}

.step-action {
  font-size: 14px;
  font-weight: 700;
  color: var(--primary-color);
}

.step-description {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.step-params {
  font-size: 11px;
  color: var(--text-secondary);
  background: #f8fbff;
  padding: 5px 10px;
  border-radius: 999px;
  display: block;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-card__ops {
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-card__ops-inner {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 4px 0;
  border-radius: 12px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.step-op-btn {
  margin: 0;
  padding: 4px 10px;
  min-height: 24px;
  font-size: 11px;
  justify-content: center;
  border-radius: 0;
  font-weight: 500;
}

.step-op-btn + .step-op-btn {
  border-top: 1px solid rgba(226, 232, 240, 0.9);
}

.empty-tip {
  text-align: center;
}
</style>
