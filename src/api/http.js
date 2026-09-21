import axios from 'axios'

/**
 * JWT 统一请求层。
 *
 * 业务代码只调用 api.get/post/...，不在页面里重复处理 Token、统一响应和 401。
 */
export const TOKEN_KEY = 'fixed-asset-token'
export const USER_KEY = 'fixed-asset-user'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

api.interceptors.request.use((config) => {
  // 每次请求都从 localStorage 读取 Token，保证重新登录后无需刷新整页。
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    // 后端统一返回 { code, message, data }，这里只把 data 交给 Store。
    const payload = response.data
    if (payload && typeof payload.code === 'number') {
      if (payload.code !== 0) {
        throw new Error(payload.message || '请求失败')
      }
      return payload.data
    }
    return payload
  },
  (error) => {
    if (error.response?.status === 401) {
      // 401 代表 Token 无效或过期：清理本地会话并回登录页。
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }
    const message = error.response?.data?.message || error.message || '网络请求失败'
    return Promise.reject(new Error(message))
  },
)
