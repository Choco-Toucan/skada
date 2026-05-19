import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginResponse } from '@/types'
import { login as loginApi, logout as logoutApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('skada_token') || '')
  const displayId = ref(localStorage.getItem('skada_display_id') || '')
  const role = ref(localStorage.getItem('skada_role') || '')

  const isLoggedIn = () => !!token.value

  async function login(username: string, password: string) {
    const res = await loginApi({ username, password })
    const data = res.data.data
    if (!data) {
      throw new Error('登录响应数据为空')
    }
    token.value = data.token
    displayId.value = data.displayId
    role.value = data.role
    localStorage.setItem('skada_token', data.token)
    localStorage.setItem('skada_display_id', data.displayId)
    localStorage.setItem('skada_role', data.role)
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // ignore
    }
    token.value = ''
    displayId.value = ''
    role.value = ''
    localStorage.removeItem('skada_token')
    localStorage.removeItem('skada_display_id')
    localStorage.removeItem('skada_role')
  }

  return { token, displayId, role, isLoggedIn, login, logout }
})
