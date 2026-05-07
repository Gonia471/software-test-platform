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
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default request
