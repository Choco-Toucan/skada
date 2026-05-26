import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types'
import { message } from 'ant-design-vue'

declare global {
  interface Window {
    __SKADA_CONFIG__: {
      apiBaseUrl: string
    }
  }
}

const http = axios.create({
  baseURL: window.__SKADA_CONFIG__?.apiBaseUrl || '/skada/mng-service/api/v1',
  timeout: 15000,
})

/** 请求拦截器：附加 Token */
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('skada_token')
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

/** 清除登录态并跳转登录页 */
function clearAuthAndRedirect(errorMessage?: string) {
  if (errorMessage) {
    message.error(errorMessage)
  }
  localStorage.removeItem('skada_token')
  localStorage.removeItem('skada_display_id')
  localStorage.removeItem('skada_role')
  window.location.href = '/#/login'
}

/** 响应拦截器：统一处理错误 */
http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<any>>) => {
    const body = response.data
    if (body.code !== 200) {
      if (body.code === 401) {
        clearAuthAndRedirect(body.message || 'token已过期或无效')
        return Promise.reject(Object.assign(new Error(body.message), { __handled: true }))
      }
      message.error(body.message || '请求失败')
      return Promise.reject(Object.assign(new Error(body.message), { __handled: true }))
    }
    return response
  },
  (error) => {
    // 如果响应拦截器已处理过，不再重复提示
    if (error.__handled) {
      return Promise.reject(error)
    }
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      clearAuthAndRedirect(error.response?.data?.message || 'token已过期或无效')
      return Promise.reject(error)
    }
    if (error.response?.data?.message) {
      message.error(error.response.data.message)
    } else {
      message.error('网络错误')
    }
    return Promise.reject(error)
  },
)

export default http
