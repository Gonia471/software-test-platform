import axios from 'axios'

const envBaseUrl = import.meta.env.VITE_API_BASE_URL
const normalizedBaseUrl = envBaseUrl ? envBaseUrl.replace(/\/+$/, '') : ''

const request = axios.create({
  // 未配置 VITE_API_BASE_URL 时，默认走 Vite 代理
  baseURL: normalizedBaseUrl ? `${normalizedBaseUrl}/api` : '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  config.__authToken = token || ''
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  res => res,
  err => {
    console.error('API Error:', err.config?.url, err.response?.status, err.response?.data)
    const responseMessage = err.response?.data?.message
    if (responseMessage) {
      err.message = responseMessage
    }
    if (err.response?.status === 401) {
      const requestToken = err.config?.__authToken || ''
      const currentToken = localStorage.getItem('token')
      // Only clear the session if the failing request still belongs to
      // the current login. This avoids old in-flight requests wiping out
      // a freshly issued token right after the user logs in again.
      if (currentToken && requestToken && currentToken === requestToken) {
        console.warn('Unauthorized! Clearing token and redirecting to login...')
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        localStorage.removeItem('phone')
        localStorage.removeItem('userId')
        localStorage.removeItem('isDevMode')
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(err)
  }
)

export default request
