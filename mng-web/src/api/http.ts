import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types'
import { message } from 'ant-design-vue'

const http = axios.create({
  baseURL: '/api/v1',
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

/** 响应拦截器：统一处理错误 */
http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<any>>) => {
    const body = response.data
    if (body.code !== 200) {
      message.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return response
  },
  (error) => {
    if (error.response?.data?.message) {
      message.error(error.response.data.message)
    } else {
      message.error('网络错误')
    }
    // 401 跳转登录
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      localStorage.removeItem('skada_token')
      localStorage.removeItem('skada_admin')
      window.location.href = '/#/login'
    }
    return Promise.reject(error)
  },
)

export default http
