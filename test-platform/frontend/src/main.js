import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 使用 es 入口，避免部分 CDN/托管对 dist 下 .mjs 的解析差异导致白屏
import zhCn from 'element-plus/es/locale/lang/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.config.errorHandler = (err, instance, info) => {
  console.error('[Vue error]', err, info)
  const el = document.getElementById('app')
  if (el && !el.querySelector('.app-fatal-error')) {
    el.innerHTML =
      '<div class="app-fatal-error" style="padding:24px;font-family:sans-serif;">' +
      '<h2>页面加载出错</h2>' +
      '<p>请用 Chrome 打开后按 F12 → Console 查看红色报错；常见原因：浏览器拦截脚本、或静态资源 404。</p>' +
      '<pre style="white-space:pre-wrap;word-break:break-all;">' +
      String(err && err.message ? err.message : err) +
      '</pre></div>'
  }
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

router.onError(err => {
  console.error('[Router]', err)
})

router.isReady().then(() => {
  app.mount('#app')
})
