import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { bootstrapSession } from './stores/backendStore'
import './styles.css'

const app = createApp(App).use(ElementPlus, { locale: zhCn }).use(router)

bootstrapSession().finally(() => app.mount('#app'))
