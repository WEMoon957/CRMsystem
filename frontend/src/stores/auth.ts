import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserVO, LoginResponse } from '@/types'
import { authApi } from '@/api'

const REFRESH_TOKEN_KEY = 'crm_refresh_token'

/**
 * 认证状态：
 * - accessToken 只保存在内存（Pinia），刷新页面后丢失，用 refreshToken 静默恢复；
 * - refreshToken 存 localStorage（服务端可撤销，泄露后可轮换）。
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const user = ref<UserVO | null>(null)

  const isLoggedIn = computed(() => accessToken.value !== null)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  function saveTokens(login: LoginResponse) {
    accessToken.value = login.accessToken
    user.value = login.user
    localStorage.setItem(REFRESH_TOKEN_KEY, login.refreshToken)
  }

  function clearTokens() {
    accessToken.value = null
    user.value = null
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  async function login(username: string, password: string) {
    saveTokens(await authApi.login(username, password))
  }

  async function restore() {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    if (!refreshToken) return false
    try {
      saveTokens(await authApi.refresh(refreshToken))
      return true
    } catch {
      clearTokens()
      return false
    }
  }

  async function logout() {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    try {
      if (accessToken.value && refreshToken) {
        await authApi.logout(refreshToken)
      }
    } finally {
      clearTokens()
    }
  }

  return { accessToken, user, isLoggedIn, isAdmin, login, restore, logout, saveTokens, clearTokens }
})
