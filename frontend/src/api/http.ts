import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

/**
 * API 客户端：
 * - Base URL 走 Vite 代理（开发）/ 环境变量（生产）；
 * - 自动附带 accessToken；
 * - 401 时用 refreshToken 静默刷新并重试（最多 1 次），刷新失败则退出登录。
 */
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

let refreshing: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('crm_refresh_token')
  if (!refreshToken) return null
  try {
    const res = await axios.post('/api/auth/refresh', { refreshToken })
    if (res.data?.code !== 0) return null
    const auth = useAuthStore()
    auth.saveTokens(res.data.data)
    return res.data.data.accessToken as string
  } catch {
    return null
  }
}

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body && body.code !== 0) {
      const err = new ApiError(body.code, body.message || '请求失败')
      // 认证失效：静默刷新后重试一次
      if (body.code === 2001 && !(response.config as any).__retried) {
        refreshing = refreshing ?? refreshAccessToken()
        return refreshing.then(async (token) => {
          refreshing = null
          if (!token) {
            const auth = useAuthStore()
            auth.clearTokens()
            window.location.href = '/login'
            return Promise.reject(new ApiError(2001, '登录已过期，请重新登录'))
          }
          ;(response.config as any).__retried = true
          response.config.headers.Authorization = `Bearer ${token}`
          return http(response.config)
        })
      }
      return Promise.reject(err)
    }
    return body
  },
  async (error: AxiosError) => {
    if (error.code === 'ERR_NETWORK') {
      ElMessage.error('网络异常，请检查连接后重试')
      return Promise.reject(new ApiError(-1, '网络异常'))
    }
    if (error.response?.status && error.response.status >= 500) {
      ElMessage.error('服务器开小差了，请稍后重试')
    }
    return Promise.reject(new ApiError(
      error.response?.status ?? -1,
      (error.response?.data as any)?.message || '请求失败'))
  }
)

export class ApiError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

export function friendlyError(e: unknown, fallback = '操作失败'): string {
  if (e instanceof ApiError) return e.message
  if (e instanceof Error) return e.message || fallback
  return fallback
}

export default http
