// UI 测试动作定义与辅助方法（仅前端模拟数据用）

let stepIdSeed = Date.now()

export const locatorOptions = [
  { label: 'XPath', value: 'xpath' },
  { label: 'CSS Selector', value: 'css' },
  { label: 'ID', value: 'id' },
  { label: 'Name', value: 'name' },
  { label: 'Link Text', value: 'linkText' },
]

export const actionGroups = [
  {
    type: 'browser',
    title: '浏览器操作',
    actions: [
      { key: 'openPage', label: '打开网页', description: '打开指定 URL' },
      { key: 'refreshPage', label: '刷新页面', description: '刷新当前页面' },
      { key: 'goBack', label: '后退', description: '浏览器后退' },
      { key: 'goForward', label: '前进', description: '浏览器前进' },
      { key: 'closeWindow', label: '关闭窗口', description: '关闭当前窗口' },
    ],
  },
  {
    type: 'element',
    title: '元素操作',
    actions: [
      { key: 'clickElement', label: '点击元素', description: '根据定位信息点击元素' },
      { key: 'inputText', label: '输入文本', description: '向输入框填入文本' },
      { key: 'clearText', label: '清空文本', description: '清空输入框内容' },
      { key: 'getText', label: '获取文本/属性', description: '读取元素文本或属性' },
      { key: 'selectOption', label: '下拉框选择', description: '在下拉框中选择选项' },
      { key: 'toggleCheck', label: '勾选框/单选框', description: '切换勾选状态' },
    ],
  },
  {
    type: 'wait',
    title: '等待操作',
    actions: [
      { key: 'sleep', label: '强制等待', description: '等待固定时间' },
      { key: 'waitVisible', label: '等待元素可见', description: '等待元素出现在页面上' },
      { key: 'waitClickable', label: '等待元素可点击', description: '等待元素变为可点击' },
      { key: 'waitDisappear', label: '等待元素消失', description: '等待元素从页面上消失' },
    ],
  },
  {
    type: 'assert',
    title: '断言操作',
    actions: [
      { key: 'assertTitle', label: '断言标题', description: '校验页面标题' },
      { key: 'assertUrl', label: '断言 URL', description: '校验当前 URL' },
      { key: 'assertTextContains', label: '断言文本包含', description: '校验页面文本包含预期内容' },
      { key: 'assertElementExist', label: '断言元素存在/不存在', description: '校验元素是否存在' },
      { key: 'assertElementVisible', label: '断言元素可见/不可见', description: '校验元素可见性' },
    ],
  },
  {
    type: 'ai',
    title: 'AI 视觉操作',
    actions: [
      { key: 'aiNaturalLanguage', label: '自然语言指令', description: '通过自然语言描述执行操作' },
      { key: 'aiImageClick', label: '图像识别点击', description: '基于截图识别并点击' },
    ],
  },
]

export function createStepFromAction(action, groupType) {
  const base = {
    id: `step-${stepIdSeed++}`,
    type: groupType,
    action: action.key,
    description: action.label,
  }

  switch (action.key) {
    case 'openPage':
      return {
        ...base,
        parameters: { url: 'https://example.com' },
      }
    case 'refreshPage':
    case 'goBack':
    case 'goForward':
    case 'closeWindow':
      return {
        ...base,
        parameters: {},
      }
    case 'clickElement':
    case 'clearText':
    case 'getText':
    case 'toggleCheck':
      return {
        ...base,
        parameters: {
          locatorType: 'css',
          locatorValue: '',
        },
      }
    case 'inputText':
      return {
        ...base,
        parameters: {
          locatorType: 'css',
          locatorValue: '',
          text: '',
        },
      }
    case 'selectOption':
      return {
        ...base,
        parameters: {
          locatorType: 'css',
          locatorValue: '',
          optionType: 'value',
          optionValue: '',
        },
      }
    case 'sleep':
      return {
        ...base,
        parameters: {
          seconds: 2,
        },
      }
    case 'waitVisible':
    case 'waitClickable':
    case 'waitDisappear':
      return {
        ...base,
        parameters: {
          locatorType: 'css',
          locatorValue: '',
          timeout: 10,
        },
      }
    case 'assertTitle':
      return {
        ...base,
        parameters: {
          expected: '',
        },
      }
    case 'assertUrl':
      return {
        ...base,
        parameters: {
          expected: '',
        },
      }
    case 'assertTextContains':
      return {
        ...base,
        parameters: {
          expectedText: '',
        },
      }
    case 'assertElementExist':
    case 'assertElementVisible':
      return {
        ...base,
        parameters: {
          locatorType: 'css',
          locatorValue: '',
          expected: true,
        },
      }
    case 'aiNaturalLanguage':
      return {
        ...base,
        parameters: {
          instruction: '例如：点击登录按钮',
        },
      }
    case 'aiImageClick':
      return {
        ...base,
        parameters: {
          mode: 'crop',
          instruction: '',
          assetId: '',
          assetName: '',
          imagePath: '',
          previewUrl: '',
          sourceType: 'upload',
          threshold: 0.82,
          box: {
            xRatio: 0,
            yRatio: 0,
            widthRatio: 0,
            heightRatio: 0,
          },
        },
      }
    default:
      return {
        ...base,
        parameters: {},
      }
  }
}

